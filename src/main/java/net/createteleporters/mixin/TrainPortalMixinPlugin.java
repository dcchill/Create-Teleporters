package net.createteleporters.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class TrainPortalMixinPlugin implements IMixinConfigPlugin {
	public void onLoad(String mixinPackage) { }
	public String getRefMapperConfig() { return null; }
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		try {
			MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
			return true;
		} catch (ClassNotFoundException | java.io.IOException absent) {
			return false;
		}
	}
	public void acceptTargets(Set<String> mine, Set<String> others) { }
	public List<String> getMixins() { return null; }
	public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) { }
	public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) { }
}
