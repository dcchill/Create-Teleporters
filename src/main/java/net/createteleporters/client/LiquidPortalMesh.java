package net.createteleporters.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3f;

final class LiquidPortalMesh {
	private LiquidPortalMesh() {
	}

	static void render(CustomPortalBaseBlockEntity controller, PoseStack.Pose pose, MultiBufferSource buffers, float partialTick) {
		var level = controller.getLevel();
		var data = controller.getPersistentData();
		if (!CustomPortalSurfaceClient.isEnabled() || data.getBoolean("immersivePortalCreated")) return;
		int min = data.getInt("portalMinExtent") + 1;
		int max = data.getInt("portalMaxExtent");
		int top = data.getInt("portalHeight");
		if (max <= min || max - min > 21 || top <= 1 || top > 22) return;
		boolean northSouth = "north".equals(data.getString("rotation")) || "south".equals(data.getString("rotation"));
		int horizontalDensity = Math.max(1, Math.min(6, 96 / (max - min)));
		int verticalDensity = Math.max(1, Math.min(6, 96 / (top - 1)));
		float time = (level.getGameTime() + partialTick) * 0.045f;
		float age = data.contains("portalRippleTime") ? level.getGameTime() - data.getLong("portalRippleTime") + partialTick : PortalLiquidSurface.RIPPLE_TICKS;
		float openingAge = data.contains("portalOpeningTime") ? level.getGameTime() - data.getLong("portalOpeningTime") + partialTick : PortalLiquidSurface.OPENING_TICKS;
		var trainEvents = data.getList("portalTrainRipples", net.minecraft.nbt.Tag.TAG_COMPOUND);
		float[][] trainRipples = new float[0][];
		if (!trainEvents.isEmpty() && level.getGameTime() - trainEvents.getCompound(trainEvents.size() - 1).getLong("Time") < PortalLiquidSurface.OPENING_TICKS) {
			trainRipples = new float[Math.min(8, trainEvents.size())][3];
			for (int i = 0; i < trainRipples.length; i++) {
				var ripple = trainEvents.getCompound(trainEvents.size() - trainRipples.length + i);
				trainRipples[i][0] = ripple.getFloat("Horizontal");
				trainRipples[i][1] = ripple.getFloat("Y");
				trainRipples[i][2] = level.getGameTime() - ripple.getLong("Time") + partialTick;
			}
		}
		float rippleH = data.getFloat("portalRippleHorizontal");
		float rippleY = data.getFloat("portalRippleY");
		Minecraft minecraft = Minecraft.getInstance();
		VertexConsumer consumer = null;
		for (int h = min; h < max; h++) for (int y = 1; y < top; y++) {
			BlockPos pos = northSouth ? controller.getBlockPos().offset(h, y, 0) : controller.getBlockPos().offset(0, y, h);
			if (!controller.getBlockPos().equals(CustomPortalSurfaceClient.owner(level, pos))) continue;
			var state = level.getBlockState(pos);
			if (!state.is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())
					|| !(minecraft.getBlockRenderer().getBlockModel(state) instanceof LiquidPortalModel model)) continue;
			if (consumer == null) consumer = buffers.getBuffer(RenderType.translucentMovingBlock());
			for (LiquidPortalModel.Face face : model.faces(state)) {
				if (face.cullFace() != null && !Block.shouldRenderFace(state, level, pos, face.cullFace(), pos.relative(face.cullFace()))) continue;
				int tint = face.quad().isTinted() ? minecraft.getBlockColors().getColor(state, level, pos, face.quad().getTintIndex()) : 0xFFFFFF;
				float[][] original = face.vertices();
				float[] sCuts = cuts(original[0], original[1], h, y, northSouth, horizontalDensity, verticalDensity);
				float[] tCuts = cuts(original[0], original[3], h, y, northSouth, horizontalDensity, verticalDensity);
				float[][] corners = new float[4][9];
				Vector3f normal = new Vector3f();
				Vector3f edge = new Vector3f();
				for (int s = 0; s < sCuts.length - 1; s++) for (int t = 0; t < tCuts.length - 1; t++) {
					point(corners[0], original, sCuts[s], tCuts[t], h, y, northSouth, min, max, top, time, rippleH, rippleY, age, openingAge, trainRipples);
					point(corners[1], original, sCuts[s + 1], tCuts[t], h, y, northSouth, min, max, top, time, rippleH, rippleY, age, openingAge, trainRipples);
					point(corners[2], original, sCuts[s + 1], tCuts[t + 1], h, y, northSouth, min, max, top, time, rippleH, rippleY, age, openingAge, trainRipples);
					point(corners[3], original, sCuts[s], tCuts[t + 1], h, y, northSouth, min, max, top, time, rippleH, rippleY, age, openingAge, trainRipples);
					normal.set(corners[1][0] - corners[0][0], corners[1][1] - corners[0][1], corners[1][2] - corners[0][2]);
					edge.set(corners[2][0] - corners[0][0], corners[2][1] - corners[0][1], corners[2][2] - corners[0][2]);
					normal.cross(edge);
					if (normal.lengthSquared() < 1e-12f) continue;
					normal.normalize();
					float shade = level.getShade(normal.x, normal.y, normal.z, face.quad().isShade());
					for (float[] corner : corners) consumer.addVertex(pose, corner[0], corner[1], corner[2])
						.setColor((int) (corner[5] * (tint >> 16 & 255) / 255 * shade), (int) (corner[6] * (tint >> 8 & 255) / 255 * shade), (int) (corner[7] * (tint & 255) / 255 * shade), (int) corner[8])
						.setUv(corner[3], corner[4]).setLight(LightTexture.FULL_BRIGHT)
						.setNormal(pose, normal.x, normal.y, normal.z);
				}
			}
		}
	}

	private static float[] cuts(float[] a, float[] b, int h, int y, boolean northSouth, int horizontalDensity, int verticalDensity) {
		int axis = 0;
		for (int i = 1; i < 3; i++) if (Math.abs(b[i] - a[i]) > Math.abs(b[axis] - a[axis])) axis = i;
		float offset = axis == 1 ? y : axis == (northSouth ? 0 : 2) ? h : 0;
		return PortalLiquidSurface.cuts(a[axis] + offset, b[axis] + offset, axis == 1 ? verticalDensity : horizontalDensity);
	}

	static void point(float[] result, float[][] original, float s, float t, int h, int y, boolean northSouth,
			float min, float max, float top, float time, float rippleH, float rippleY, float age, float openingAge, float[][] trainRipples) {
		for (int i = 0; i < result.length; i++) result[i] = PortalLiquidSurface.interpolate(original[0][i], original[1][i], original[2][i], original[3][i], s, t);
		int horizontalAxis = northSouth ? 0 : 2;
		int depthAxis = northSouth ? 2 : 0;
		result[horizontalAxis] += h;
		result[1] += y;
		result[depthAxis] = PortalLiquidSurface.surfaceDepth(result[depthAxis], result[horizontalAxis], result[1], min, max, top, time, rippleH, rippleY, age, openingAge, trainRipples);
	}
}
