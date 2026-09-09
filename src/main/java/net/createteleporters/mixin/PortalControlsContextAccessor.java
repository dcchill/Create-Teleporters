package net.createteleporters.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Collection;

@Mixin(targets = "com.simibubi.create.content.contraptions.actors.trainControls.ControlsServerHandler$ControlsContext", remap = false)
public interface PortalControlsContextAccessor {
	@Accessor("entity") AbstractContraptionEntity ctp$entity();
	@Accessor("controlsLocalPos") BlockPos ctp$controls();
	@Accessor("keys") Collection<? extends Pair<Integer, Integer>> ctp$keys();
}
