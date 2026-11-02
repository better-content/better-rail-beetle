package com.bettercontent.betterrailbeetle.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Minimal native boundary: inspect an already baked scalar, never read or recompute part materials. */
public final class ManufacturedRatings {
    private static final String SETTINGS = "better_manufacturing:settings";
    private ManufacturedRatings() {}

    public static boolean validChassis(CompoundTag tag) {
        if (tag == null || tag.getAllKeys().size() != 4 || !tag.contains("version", Tag.TAG_INT)
                || tag.getInt("version") != 1 || !tag.contains("output", Tag.TAG_STRING)
                || !tag.getString("output").equals("better_rail_beetle:better_rail_beetle")
                || !tag.contains("properties", Tag.TAG_COMPOUND) || !tag.contains("materials", Tag.TAG_COMPOUND)) return false;
        CompoundTag props = tag.getCompound("properties"), labels = tag.getCompound("materials");
        if (props.getAllKeys().size() != 1 || !props.contains("drive_performance", Tag.TAG_DOUBLE)
                || !labels.getAllKeys().equals(java.util.Set.of("drive_shaft", "working_surface"))) return false;
        for (String role : labels.getAllKeys()) {
            if (!labels.contains(role, Tag.TAG_STRING) || labels.getString(role).length() > 192) return false;
        }
        double value = props.getDouble("drive_performance");
        return Double.isFinite(value) && value >= .75 && value <= 1.5;
    }

    public static double read(ItemStack stack, String property) {
        if (stack.isEmpty() || stack.getTag() == null || !stack.getTag().contains(SETTINGS, Tag.TAG_COMPOUND)) return 1;
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return 1;
        return read(stack.getTag().getCompound(SETTINGS), id.toString(), property);
    }

    public static double read(CompoundTag tag, String output, String property) {
        if (tag == null || tag.getAllKeys().size() != 4 || !tag.contains("version", Tag.TAG_INT)
                || tag.getInt("version") != 1 || !tag.contains("output", Tag.TAG_STRING)
                || !output.equals(tag.getString("output")) || !tag.contains("properties", Tag.TAG_COMPOUND)
                || !tag.contains("materials", Tag.TAG_COMPOUND)) return 1;
        String expected = switch (output) {
            case "better_rail_beetle:better_rail_beetle", "better_rail_beetle:steam_drive",
                 "better_rail_beetle:flux_traction_motor", "better_rail_beetle:source_impeller",
                 "better_rail_beetle:lifeforce_ram", "better_rail_beetle:pneumatic_expansion_motor",
                 "better_rail_beetle:soul_combustor", "better_rail_beetle:spirit_warping_engine" -> "drive_performance";
            case "better_rail_beetle:high_speed_governor_1", "better_rail_beetle:high_speed_governor_2" -> "speed_limit";
            case "better_rail_beetle:exhaust_recuperator_1", "better_rail_beetle:exhaust_recuperator_2" -> "recovery";
            case "better_rail_beetle:brake_manifold_1", "better_rail_beetle:brake_manifold_2" -> "deceleration";
            case "better_rail_beetle:adhesion_sanders_1", "better_rail_beetle:adhesion_sanders_2" -> "adhesion_delivery";
            case "better_rail_beetle:compound_torque_clutch_1", "better_rail_beetle:compound_torque_clutch_2" -> "torque_transfer";
            case "better_rail_beetle:reinforced_drawgear_1", "better_rail_beetle:reinforced_drawgear_2" -> "towing_limit";
            case "better_rail_beetle:trestle_erector_1", "better_rail_beetle:trestle_erector_2" -> "work_rate";
            default -> null;
        };
        if (!property.equals(expected)) return 1;
        if (output.equals("better_rail_beetle:better_rail_beetle")) return validChassis(tag) ? tag.getCompound("properties").getDouble(property) : 1;
        var properties = tag.getCompound("properties");
        var materials = tag.getCompound("materials");
        if (properties.getAllKeys().size() != 1 || !properties.contains(property, Tag.TAG_DOUBLE)
                || !materials.getAllKeys().equals(java.util.Set.of("drive_shaft", "working_surface"))) return 1;
        for (String role : materials.getAllKeys())
            if (!materials.contains(role, Tag.TAG_STRING) || materials.getString(role).length() > 192) return 1;
        double rate = properties.getDouble(property);
        return Double.isFinite(rate) && rate >= .75 && rate <= 1.5 ? rate : 1;
    }
}
