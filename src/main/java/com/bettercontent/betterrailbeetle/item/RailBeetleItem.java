package com.bettercontent.betterrailbeetle.item;

import com.bettercontent.betterrailbeetle.RailBeetleRegistries;
import com.bettercontent.betterrailbeetle.entity.RailBeetleEntity;
import com.bettercontent.betterrailbeetle.upgrade.ManufacturedRatings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseRailBlock;

public final class RailBeetleItem extends Item {
    public RailBeetleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.getTag() != null && stack.getTag().contains("better_manufacturing:settings", net.minecraft.nbt.Tag.TAG_COMPOUND))
            tooltip.add(Component.literal("drive performance: " + String.format(java.util.Locale.ROOT, "%.2f",
                    ManufacturedRatings.read(stack, "drive_performance")) + "x").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos railPos = context.getClickedPos();
        if (!(context.getLevel().getBlockState(railPos).getBlock() instanceof BaseRailBlock)) {
            return InteractionResult.FAIL;
        }
        if (context.getLevel() instanceof ServerLevel level) {
            RailBeetleEntity beetle = RailBeetleRegistries.RAIL_BEETLE_ENTITY.get().create(level);
            if (beetle == null) return InteractionResult.FAIL;
            beetle.setPos(railPos.getX() + 0.5, railPos.getY() + 0.0625, railPos.getZ() + 0.5);
            beetle.setYRot(context.getRotation());
            beetle.setInitialHeading(Direction.fromYRot(context.getRotation()));
            beetle.setChassisSettings(context.getItemInHand());
            if (StarterBeetlePackage.requested(context.getItemInHand())) {
                StarterBeetlePackage.install(beetle);
            }
            level.addFreshEntity(beetle);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
