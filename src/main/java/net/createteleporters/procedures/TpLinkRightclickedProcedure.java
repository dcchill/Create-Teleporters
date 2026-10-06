package net.createteleporters.procedures;

import net.createteleporters.init.CreateteleportersModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class TpLinkRightclickedProcedure {
    public static void execute(Entity entity) {
        if (entity instanceof LivingEntity living)
            execute(entity, living.getMainHandItem().is(CreateteleportersModItems.TP_LINK.get()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public static void execute(Entity entity, InteractionHand hand) {
        if (entity instanceof LivingEntity living && living.getItemInHand(hand).is(CreateteleportersModItems.TP_LINK.get()))
            saveTarget(entity, living.getItemInHand(hand), 2500, false);
    }

    static void saveTarget(Entity entity, ItemStack stack, int range, boolean advanced) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Vec3 from = entity.getEyePosition(1f);
        Vec3 to = from.add(entity.getViewVector(1f).scale(range));
        Vec3 backwards = from.subtract(to);
        BlockHitResult miss = BlockHitResult.miss(to, Direction.getNearest(backwards.x, backwards.y, backwards.z), BlockPos.containing(to));
        ClipContext context = new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity);
        
        BlockHitResult hit = BlockGetter.traverseBlocks(from, to, context, (clip, pos) -> {
            if (level.isOutsideBuildHeight(pos)) return miss;
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null) return miss;
            var state = chunk.getBlockState(pos);
            return level.clipWithInteractionOverride(from, to, pos, clip.getBlockShape(state, level, pos), state);
        }, clip -> miss);
        if (hit.getType() != HitResult.Type.BLOCK) {
            if (entity instanceof Player player) player.displayClientMessage(Component.literal("No loaded block in sight"), true);
            return;
        }
        BlockPos pos = hit.getBlockPos();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putDouble("xpo", pos.getX() + 0.5);
            tag.putDouble("ypo", pos.getY() + 0.9);
            tag.putDouble("zpo", pos.getZ() + 0.5);
            tag.putDouble("yawpo", entity.getYRot());
            if (advanced) tag.putString("dimension", level.dimension().location().toString());
        });
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.literal("Teleport Data Saved"), true);
            player.displayClientMessage(Component.literal("X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ()), false);
        }
    }
}
