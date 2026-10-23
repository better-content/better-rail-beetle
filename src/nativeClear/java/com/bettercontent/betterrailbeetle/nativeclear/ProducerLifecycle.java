package com.bettercontent.betterrailbeetle.nativeclear;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Startup dictionary and sole shutdown output; no added world generation or mutation. */
@Mod.EventBusSubscriber(modid = "better_rail_beetle")
public final class ProducerLifecycle {
    private ProducerLifecycle() {}
    @SubscribeEvent
    public static void starting(ServerAboutToStartEvent event) {
        try {
            // Select ALL matching blocks BEFORE any selected block's state palette is enumerated.
            var selected = new ArrayList<Block>();
            boolean selectionComplete = true;
            for (Block block : BuiltInRegistries.BLOCK) {
                if (BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft") || block instanceof EntityBlock) {
                    if (selected.size() == ProducerPalette.CAP) selectionComplete = false;
                    else selected.add(block);
                }
            }
            selected.sort(java.util.Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
            var selectedNames = selected.stream().map(block -> BuiltInRegistries.BLOCK.getKey(block).toString()).toList();
            var identities = new IdentityHashMap<Object,ProducerPalette.State>();
            int index = 0;
            boolean complete = selectionComplete;
            outer: for (Block block : selected) {
                var states = block.getStateDefinition().getPossibleStates();
                for (BlockState state : states) {
                    if (index == ProducerPalette.CAP) { complete = false; break outer; }
                    identities.put(state,new ProducerPalette.State(index++,state.toString()));
                }
            }
            ProducerCapture.initialize(new ProducerPalette(identities,selectedNames,complete));
        } catch (Throwable observerFailure) { ProducerCapture.failed(); }
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        var result = ProducerCapture.finish();
        if (result == null) return;
        var palette = ProducerCapture.palette();
        String attempt;
        try { attempt = java.util.UUID.fromString(System.getProperty("bc.rail.nativeClear.token", "")).toString(); }
        catch (IllegalArgumentException invalid) { attempt = "INVALID"; }
        LogUtils.getLogger().info("BC_RAIL_PRODUCER_WITNESS attempt={} schema={} palette={} selected={} states={} palette_complete={} summary={}",
                attempt,ProducerPalette.sha256(ProducerPalette.SCHEMA),palette == null ? "MISSING" : palette.digest(),
                palette == null ? 0 : palette.selectedCount(),palette == null ? 0 : palette.size(),
                palette != null && palette.complete(),result);
    }
}
