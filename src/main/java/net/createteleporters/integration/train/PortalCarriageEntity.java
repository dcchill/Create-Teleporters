package net.createteleporters.integration.train;

import java.util.Optional;
import net.minecraft.core.BlockPos;
public interface PortalCarriageEntity {
	Optional<BlockPos> ctp$getControls();
	void ctp$setControls(Optional<BlockPos> controls);
	String ctp$getSide();
	void ctp$setSide(String side);
}
