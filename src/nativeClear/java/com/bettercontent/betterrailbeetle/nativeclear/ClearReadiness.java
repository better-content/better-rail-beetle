package com.bettercontent.betterrailbeetle.nativeclear;

import com.bettercontent.betterrailbeetle.nativeclear.mixin.ReadinessCacheAccess;
import com.bettercontent.betterrailbeetle.nativeclear.mixin.ReadinessMapAccess;
import com.mojang.datafixers.util.Either;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/** Source-native fixture preparation; NEVER called by a shipping mod or ordinary RunConfig. */
public final class ClearReadiness {
    public static ClearReadinessPolicy.Lease prepare(BoundingBox bounds,ServerLevel world) {
        if(!"ticking-v1".equals(System.getProperty("bc.rail.nativeClear.readiness")) || !(world.getServer() instanceof GameTestServer))
            throw new IllegalStateException("Unsupported native clear readiness RunConfig/server");
        return ClearReadinessPolicy.prepare(new NativePort(world),ClearReadinessPolicy.footprint(bounds.minX(),bounds.minZ(),bounds.maxX(),bounds.maxZ()));
    }
    private record NativePort(ServerLevel world,ServerChunkCache cache,ChunkMap map,
                              ReadinessMapAccess access,BlockableEventLoop<Runnable> loop) implements ClearReadinessPolicy.Port {
        NativePort(ServerLevel world) {this(world,world.getChunkSource(),world.getChunkSource().chunkMap);}
        private NativePort(ServerLevel world,ServerChunkCache cache,ChunkMap map) {
            this(world,cache,map,(ReadinessMapAccess)map,((ReadinessMapAccess)map).railReadinessExecutor());
        }
        public boolean onThread(){return loop.isSameThread();}
        public long now(){return System.nanoTime();}
        public boolean forced(ClearReadinessPolicy.Tile tile){return world.getForcedChunks().contains(ChunkPos.asLong(tile.x(),tile.z()));}
        public void force(ClearReadinessPolicy.Tile tile,boolean value){
            if(!world.setChunkForced(tile.x(),tile.z(),value))throw new IllegalStateException("Unexpected native forced admission/release result");
        }
        public void admit(){((ReadinessCacheAccess)cache).railReadinessAdmit();}
        public void pump(BooleanSupplier stop){loop.managedBlock(stop);}
        public ClearReadinessPolicy.Stage snapshot(ClearReadinessPolicy.Tile tile){
            long key=ChunkPos.asLong(tile.x(),tile.z());
            ChunkHolder holder=access.railReadinessVisible(key);
            if(holder==null||!holder.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))throw new IllegalStateException("No native BLOCK_TICKING admission");
            var future=holder.getTickingChunkFuture();
            var stage=new NativeStage(this,key,holder,future);
            stage.done(); // reject an already-completed Right/error now, never retry until green
            return stage;
        }
    }
    private record NativeStage(NativePort port,long key,ChunkHolder holder,
                               CompletableFuture<Either<LevelChunk,ChunkHolder.ChunkLoadingFailure>> future) implements ClearReadinessPolicy.Stage {
        public boolean done(){
            if(!future.isDone())return false;
            successful();
            return true;
        }
        private LevelChunk successful(){
            var result=future.getNow(null); // done-only, never join/block the main thread
            if(result==null||result.left().isEmpty())throw new IllegalStateException("Native TICKING future returned unloaded/Right/null");
            return result.left().orElseThrow();
        }
        public void verify(){
            if(!port.onThread()||!future.isDone()||port.access.railReadinessVisible(key)!=holder
                    ||holder.getTickingChunkFuture()!=future||!holder.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))
                throw new IllegalStateException("Stale native holder/TICKING future");
            LevelChunk chunk=successful();
            if(chunk.getPos().toLong()!=key||chunk.getLevel()!=port.world)throw new IllegalStateException("Wrong native ready chunk/world");
        }
    }
    private ClearReadiness() {}
}
