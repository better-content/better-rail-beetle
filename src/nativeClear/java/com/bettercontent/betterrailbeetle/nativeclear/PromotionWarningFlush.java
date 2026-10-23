package com.bettercontent.betterrailbeetle.nativeclear;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The sole output point: after measured native setup/tests and shutdown loops. */
@Mod.EventBusSubscriber(modid = "better_rail_beetle")
public final class PromotionWarningFlush {
    private PromotionWarningFlush() {}

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        var snapshot = PromotionWarningCapture.finish();
        if (snapshot == null) return;
        String attempt = token("bc.rail.nativeClear.token", "UNKNOWN");
        String witness = token("bc.rail.nativeWitness.token", "ABSENT");
        LogUtils.getLogger().info("BC_RAIL_PROMOTION_WARNING_WITNESS attempt={} witness={} max_records=4 max_frames=24 snapshot={}",
                attempt, witness, snapshot);
    }

    private static String token(String key, String absent) {
        String value = System.getProperty(key);
        if (value == null) return absent;
        try { return java.util.UUID.fromString(value).toString(); }
        catch (IllegalArgumentException malformed) { return "INVALID"; }
    }
}
