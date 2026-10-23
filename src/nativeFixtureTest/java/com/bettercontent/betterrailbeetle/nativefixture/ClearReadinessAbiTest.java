package com.bettercontent.betterrailbeetle.nativefixture;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class ClearReadinessAbiTest {
    private final ProducerAbiTest support=new ProducerAbiTest();
    ClassNode read(String name)throws Exception{return support.read(name);}
    List<MethodInsnNode> calls(MethodNode method){return support.calls(method);}
    Map<String,Object> values(AnnotationNode annotation){return support.values(annotation);}
    static final String BASE="com/bettercontent/betterrailbeetle/nativeclear/";
    MethodNode method(ClassNode n,String name){return n.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    @Test void exactPublicTypedExecutorAccessorAndAdmissionInvokerBindActualNativeAbi() throws Exception {
        var map=read("net/minecraft/server/level/ChunkMap");
        var field=map.fields.stream().filter(f->f.name.equals("mainThreadExecutor")).findFirst().orElseThrow();
        assertEquals("Lnet/minecraft/util/thread/BlockableEventLoop;",field.desc);assertTrue(field.signature.contains("Ljava/lang/Runnable;"));
        assertEquals("(J)Lnet/minecraft/server/level/ChunkHolder;",method(map,"getVisibleChunkIfPresent").desc);
        assertEquals("()Z",method(read("net/minecraft/server/level/ServerChunkCache"),"runDistanceManagerUpdates").desc);
        var cache=read("net/minecraft/server/level/ServerChunkCache");
        var constructor=method(cache,"<init>");
        assertTrue(calls(constructor).stream().anyMatch(c->c.owner.equals(map.name)&&c.name.equals("<init>")&&c.desc.contains("Lnet/minecraft/util/thread/BlockableEventLoop;")));
        assertTrue(Arrays.stream(constructor.instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f&&f.name.equals("mainThreadProcessor")&&f.desc.endsWith("ServerChunkCache$MainThreadExecutor;")));
        var loop=read("net/minecraft/server/level/ServerChunkCache$MainThreadExecutor");
        var pollCalls=calls(method(loop,"pollTask"));assertEquals("runDistanceManagerUpdates",pollCalls.get(0).name);assertTrue(pollCalls.stream().anyMatch(c->c.name.equals("tryScheduleUpdate")));
        var api=read(BASE+"mixin/ReadinessMapAccess");assertEquals(field.desc,"L"+Type.getReturnType(method(api,"railReadinessExecutor").desc).getInternalName()+";");
        for(String helper:List.of("ClearReadiness","ClearReadinessPolicy","ClearReadiness$NativePort","ClearReadiness$NativeStage"))assertFalse((BASE+helper).startsWith(BASE+"mixin/"));
    }
    @Test void actualPendingLifecycleExplainsLiveBeShortCircuitAndNativeTickingCompletion() throws Exception {
        var level=read("net/minecraft/world/level/chunk/LevelChunk");
        var be=level.methods.stream().filter(m->m.name.equals("getBlockEntity")&&m.desc.contains("EntityCreationType")).findFirst().orElseThrow();
        int pending=-1;for(int i=0;i<be.instructions.size();i++)if(be.instructions.get(i) instanceof FieldInsnNode f&&f.name.equals("pendingBlockEntities")){pending=i;break;}
        final int index=pending;assertTrue(index>0);
        assertTrue(Arrays.stream(be.instructions.toArray()).limit(index).anyMatch(i->i instanceof JumpInsnNode j&&j.getOpcode()==Opcodes.IFNONNULL&&be.instructions.indexOf(j.label)>index));
        assertFalse(Arrays.stream(method(level,"removeBlockEntity").instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f&&f.name.equals("pendingBlockEntities")));
        var proto=method(read("net/minecraft/world/level/chunk/ProtoChunk"),"removeBlockEntity");assertEquals(2,calls(proto).stream().filter(c->c.name.equals("remove")).count());
        assertTrue(Arrays.stream(proto.instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f&&f.name.equals("pendingBlockEntities")));
        var post=calls(method(level,"postProcessGeneration"));int get=-1,clear=-1;for(int i=0;i<post.size();i++){if(post.get(i).name.equals("getBlockEntity"))get=i;if(post.get(i).name.equals("clear")&&post.get(i).owner.equals("java/util/Map"))clear=i;}assertTrue(get>=0&&clear>get);
        var map=read("net/minecraft/server/level/ChunkMap");
        var postStart=map.methods.stream().filter(m->m.name.startsWith("lambda$prepareTickingChunk$")&&calls(m).stream().anyMatch(c->c.name.equals("postProcessGeneration"))).findFirst().orElseThrow();
        assertEquals(List.of("postProcessGeneration","startTickingChunk"),calls(postStart).stream().map(c->c.name).toList());
        assertTrue(calls(method(read("net/minecraft/server/level/ChunkHolder"),"updateFutures")).stream().anyMatch(c->c.name.equals("prepareTickingChunk")));
        assertEquals(List.of("runAllUpdates","promoteChunkMap","clearCache"),calls(method(read("net/minecraft/server/level/ServerChunkCache"),"runDistanceManagerUpdates")).stream().map(c->c.name).toList());
        assertTrue(read("net/minecraft/server/level/DistanceManager").methods.stream().flatMap(m->calls(m).stream()).anyMatch(c->c.owner.equals("net/minecraft/server/level/ChunkHolder")&&c.name.equals("updateFutures")));
    }
    @Test void pinnedNativeInflationIsExactAndReadinessWrapperForwardsOnceWithThrowableCleanup() throws Exception {
        var original=method(read("net/minecraft/gametest/framework/StructureUtils"),"clearSpaceForStructure");
        var offsets=new ArrayList<Integer>();
        for(var i:original.instructions)if(i.getOpcode()==Opcodes.ISUB||i.getOpcode()==Opcodes.IADD){var prev=i.getPrevious();while(prev.getOpcode()<0)prev=prev.getPrevious();int value=prev instanceof IntInsnNode n?n.operand:prev.getOpcode()-Opcodes.ICONST_0;offsets.add(i.getOpcode()==Opcodes.ISUB?-value:value);}
        assertEquals(List.of(-2,-3,-3,3,20,3),offsets);
        var wrapper=method(read(BASE+"mixin/ClearReadinessMixin"),"awaitNativeTickingBeforeClear");
        var fields=values(wrapper.visibleAnnotations.stream().filter(a->a.desc.endsWith("/WrapMethod;")).findFirst().orElseThrow());
        for(String key:List.of("require","expect","allow"))assertEquals(1,fields.get(key));assertEquals(List.of(original.name+original.desc),fields.get("method"));
        wrapperContract(wrapper);
        wrapper.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,"com/llamalad7/mixinextras/injector/wrapoperation/Operation","call","([Ljava/lang/Object;)Ljava/lang/Object;",true));
        assertThrows(AssertionError.class,()->wrapperContract(wrapper));
    }
    void wrapperContract(MethodNode m){var list=calls(m);assertEquals(1,list.stream().filter(c->c.owner.endsWith("/Operation")&&c.name.equals("call")).count());assertEquals("prepare",list.get(0).name);assertTrue(list.stream().anyMatch(c->c.name.equals("addSuppressed")));assertTrue(m.tryCatchBlocks.stream().anyMatch(b->"java/lang/Throwable".equals(b.type)));}
    @Test void nativeAdapterUsesOnlyExistingFutureOnActualLoopAndNegativeManualBypassRejects() throws Exception {
        for(String name:List.of("ClearReadiness","ClearReadiness$NativePort","ClearReadiness$NativeStage"))safe(read(BASE+name));
        var stage=read(BASE+"ClearReadiness$NativeStage");assertTrue(calls(method(stage,"successful")).stream().anyMatch(c->c.name.equals("getNow")));assertTrue(calls(method(stage,"verify")).stream().anyMatch(c->c.name.equals("getTickingChunkFuture")));
        var port=read(BASE+"ClearReadiness$NativePort");assertTrue(calls(method(port,"pump")).stream().anyMatch(c->c.owner.equals("net/minecraft/util/thread/BlockableEventLoop")&&c.name.equals("managedBlock")));
        method(port,"pump").instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"net/minecraft/world/level/chunk/LevelChunk","postProcessGeneration","()V",false));assertThrows(AssertionError.class,()->safe(port));
        var fixture=java.nio.file.Files.readString(java.nio.file.Path.of("gradle/native-gametest-fixture.gradle"));assertTrue(fixture.contains("nativeRun.property('bc.rail.nativeClear.readiness', 'ticking-v1')"));assertTrue(fixture.contains("run.properties.containsKey('bc.rail.nativeClear.readiness')"));for(String mixin:List.of("ReadinessMapAccess","ReadinessCacheAccess","ClearReadinessMixin"))assertTrue(fixture.contains("'"+mixin+"':"));
    }
    void safe(ClassNode n){for(var m:n.methods)for(var c:calls(m)){
        assertFalse(Set.of("postProcessGeneration","prepareTickingChunk","getBlockEntity","setBlock","setBlockState","join","sleep","wait","getBlockState","newBlockEntity").contains(c.name),c.name);
        assertFalse(c.owner.startsWith("java/lang/reflect/")||c.owner.startsWith("java/nio/file/")||c.owner.contains("ServerChunkCache$"));
        if(c.name.equals("managedBlock"))assertEquals("net/minecraft/util/thread/BlockableEventLoop",c.owner);
    }}
}
