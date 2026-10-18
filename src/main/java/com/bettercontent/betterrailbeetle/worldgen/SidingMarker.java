package com.bettercontent.betterrailbeetle.worldgen;

import com.bettercontent.betterrailbeetle.RailBeetleRegistries;
import com.bettercontent.betterrailbeetle.compat.CreateCompat;
import com.bettercontent.betterrailbeetle.entity.RailBeetleEntity;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent one-shot initializer. Never polls chunks, loads terrain, or restocks completed sites. */
public final class SidingMarker extends BlockEntity {
    private BlockPos origin;
    private Direction heading = Direction.NORTH;
    private boolean complete;
    private int attempts;
    private int spawnedMask;
    public SidingMarker(BlockPos pos, BlockState state) { super(SidingWorldgen.MARKER_ENTITY.get(), pos, state); }
    public void configure(BlockPos origin, Direction heading) {
        this.origin = origin.immutable(); this.heading = heading; setChanged();
    }
    public static void tick(Level level, BlockPos pos, BlockState state, SidingMarker marker) {
        if (marker.complete || marker.origin == null || level.getGameTime()%20 != 0) return;
        // Entity loading follows chunk loading. Wait a full second before consulting UUIDs.
        if (++marker.attempts < 2) return;
        if (marker.attempts > 12) {
            marker.complete = true; marker.setChanged();
            com.mojang.logging.LogUtils.getLogger().warn("Abandoning incomplete Rail Beetle siding at {}", pos);
            return;
        }
        marker.initialize((ServerLevel) level);
    }
    public boolean complete() { return complete; }
    public UUID vehicleId(ServerLevel level, int index) {
        return UUID.nameUUIDFromBytes(("better_rail_beetle:siding:" + level.dimension().location() + ":" + worldPosition.asLong() + ":" + index).getBytes(StandardCharsets.UTF_8));
    }
    private void initialize(ServerLevel level) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("create")) return;
        AbstractMinecart[] carts = new AbstractMinecart[3];
        for (int i = 0; i < 3; i++) {
            Entity existing = level.getEntity(vehicleId(level, i));
            if (existing != null && !(existing instanceof AbstractMinecart)) return;
            if (existing instanceof AbstractMinecart cart) { carts[i] = cart; continue; }
            // A previously spawned vehicle that disappeared is not permission to mint another.
            if ((spawnedMask & (1 << i)) != 0) { complete = true; setChanged(); return; }
            BlockPos rail = SidingPiece.position(origin, heading, 1 - 3*i, 0);
            // If the site was disturbed before initialization, abandon it rather than repair player changes.
            if (!level.getBlockState(rail).is(Blocks.RAIL)) { complete = true; setChanged(); return; }
            AbstractMinecart cart = switch (i) {
                case 0 -> RailBeetleRegistries.RAIL_BEETLE_ENTITY.get().create(level);
                case 1 -> EntityType.MINECART.create(level);
                default -> EntityType.CHEST_MINECART.create(level);
            };
            if (cart == null) return;
            cart.setUUID(vehicleId(level, i));
            cart.moveTo(rail.getX()+0.5, rail.getY()+0.0625, rail.getZ()+0.5, heading.toYRot(), 0);
            if (cart instanceof RailBeetleEntity beetle) {
                beetle.parkForDiscovery(heading);
                beetle.inventory().setStackInSlot(0, new ItemStack(Items.RAIL, 64));
                beetle.inventory().setStackInSlot(1, new ItemStack(Items.COBBLESTONE, 64));
                beetle.inventory().setStackInSlot(2, new ItemStack(Items.COAL, 16));
            }
            if (cart instanceof MinecartChest chest) {
                chest.setItem(0, new ItemStack(Items.RAIL, 64));
                chest.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
                chest.setItem(2, new ItemStack(Items.COAL, 16));
            }
            if (!level.addFreshEntity(cart)) return;
            spawnedMask |= 1 << i;
            setChanged();
            carts[i] = cart;
        }
        if (!CreateCompat.ensureCoupled(carts[0], carts[1]) || !CreateCompat.ensureCoupled(carts[1], carts[2])) return;
        // Commit remains after vehicles leave or are destroyed. No automatic replacement.
        complete = true;
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (origin != null) tag.putLong("Origin", origin.asLong());
        tag.putInt("SpawnedMask", spawnedMask);
        tag.putString("Heading", heading.getName()); tag.putBoolean("Complete", complete);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        origin = tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : null;
        Direction saved = Direction.byName(tag.getString("Heading"));
        heading = saved != null && saved.getAxis().isHorizontal() ? saved : Direction.NORTH;
        complete = tag.getBoolean("Complete"); spawnedMask = tag.getInt("SpawnedMask"); attempts = 0;
    }
}
