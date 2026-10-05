package net.createteleporters.procedures;

import net.createteleporters.init.CreateteleportersModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class AdvTpLinkRightclickedProcedure {
    public static void execute(Entity entity) {
        if (entity instanceof LivingEntity living)
            execute(entity, living.getMainHandItem().is(CreateteleportersModItems.ADV_TPLINK.get()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public static void execute(Entity entity, InteractionHand hand) {
        if (entity instanceof LivingEntity living && living.getItemInHand(hand).is(CreateteleportersModItems.ADV_TPLINK.get()))
            TpLinkRightclickedProcedure.saveTarget(entity, living.getItemInHand(hand), 200, true);
    }
}
