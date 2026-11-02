package com.bettercontent.betterrailbeetle.item;

import com.bettercontent.betterrailbeetle.upgrade.ModuleKind;
import com.bettercontent.betterrailbeetle.upgrade.ManufacturedRatings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class ModuleItem extends Item {
    private final ModuleKind kind;

    public ModuleItem(ModuleKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public ModuleKind kind() { return kind; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.better_rail_beetle.module.tier", kind.tier()).withStyle(ChatFormatting.GOLD));
        String property = switch (kind.family()) {
            case GOVERNOR -> "speed_limit";
            case RECUPERATOR -> "recovery";
            case BRAKES -> "deceleration";
            case ADHESION -> "adhesion_delivery";
            case TORQUE -> "torque_transfer";
            case DRAWGEAR -> "towing_limit";
            case TRESTLE -> "work_rate";
            default -> null;
        };
        if (property != null && stack.getTag() != null
                && stack.getTag().contains("better_manufacturing:settings", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            double rating = ManufacturedRatings.read(stack, property);
            tooltip.add(Component.literal(property.replace('_', ' ') + ": "
                    + String.format(java.util.Locale.ROOT, "%.2f", rating) + "x").withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.better_rail_beetle.module." + kind.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.better_rail_beetle.install_stopped").withStyle(ChatFormatting.DARK_GRAY));
    }
}
