package net.createteleporters.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createteleporters.CreateteleportersMod;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;

public class CustomPortalBaseRenderer implements BlockEntityRenderer<CustomPortalBaseBlockEntity> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
		CreateteleportersMod.MODID, "textures/block/p_portal.png");
	private static final int SEGMENTS = 12;

	public CustomPortalBaseRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(CustomPortalBaseBlockEntity blockEntity, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight, int packedOverlay) {
		CompoundTag data = blockEntity.getPersistentData();
		if (!data.getBoolean("portalVisualActive"))
			return;

		int min = data.getInt("portalMinExtent") + 1;
		int max = data.getInt("portalMaxExtent");
		int top = data.getInt("portalHeight");
		if (max <= min || top <= 1)
			return;

		String rotation = data.getString("rotation");
		boolean northSouth = "north".equals(rotation) || "south".equals(rotation);
		DyeColor dye = DyeColor.byName(data.getString("portalColor"), DyeColor.PURPLE);
		int rgb = dye.getTextureDiffuseColor();
		int red = rgb >> 16 & 255;
		int green = rgb >> 8 & 255;
		int blue = rgb & 255;
		float time = (blockEntity.getLevel().getGameTime() + partialTick) * 0.045f;
		float pulse = 0.78f + 0.22f * (float) Math.sin(time * 3.4f);
		long seed = blockEntity.getBlockPos().asLong();

		poseStack.pushPose();
		PoseStack.Pose pose = poseStack.last();
		if (!data.getBoolean("immersivePortalCreated")) {
			VertexConsumer core = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE, false));
			quad(core, pose, northSouth, min, 1, max, top, 0.494f,
				red / 7, green / 7, blue / 7, 250);
			VertexConsumer energy = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE, false));
			quad(energy, pose, northSouth, min, 1, max, top, 0.506f,
				red, green, blue, (int) (190 * pulse));
		}

		VertexConsumer rim = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE, false));
		for (int i = 0; i < SEGMENTS; i++) {
			float a = i / (float) SEGMENTS;
			float b = (i + 1) / (float) SEGMENTS;
			float h0 = lerp(min, max, a);
			float h1 = lerp(min, max, b);
			float y0 = lerp(1, top, a);
			float y1 = lerp(1, top, b);
			float jagA = jag(seed, i, time);
			float jagB = jag(seed, i + 1, time);
			int alpha = (int) (255 * pulse);

			quad(rim, pose, northSouth, h0, 1, h1, 1 + Math.max(jagA, jagB), 0.515f,
				red, green, blue, alpha);
			quad(rim, pose, northSouth, h0, top - Math.max(jagA, jagB), h1, top, 0.515f,
				red, green, blue, alpha);
			quad(rim, pose, northSouth, min, y0, min + Math.max(jagA, jagB), y1, 0.515f,
				red, green, blue, alpha);
			quad(rim, pose, northSouth, max - Math.max(jagA, jagB), y0, max, y1, 0.515f,
				red, green, blue, alpha);

			float shardH = lerp(min, max, ((i * 7) % SEGMENTS + 0.5f) / SEGMENTS);
			float shardY = lerp(1, top, ((i * 5) % SEGMENTS + 0.5f) / SEGMENTS);
			float drift = 0.08f * (float) Math.sin(time * 4 + i * 2.1);
			diamond(rim, pose, northSouth, shardH, shardY + drift, 0.523f,
				0.06f + jagA * 0.18f, 0.14f + jagB * 0.24f, red, green, blue, 235);
		}
		poseStack.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(CustomPortalBaseBlockEntity blockEntity) {
		CompoundTag data = blockEntity.getPersistentData();
		int min = data.getInt("portalMinExtent");
		int max = data.getInt("portalMaxExtent") + 1;
		int top = Math.max(2, data.getInt("portalHeight") + 1);
		boolean northSouth = "north".equals(data.getString("rotation")) || "south".equals(data.getString("rotation"));
		int x = blockEntity.getBlockPos().getX();
		int y = blockEntity.getBlockPos().getY();
		int z = blockEntity.getBlockPos().getZ();
		return northSouth
			? new AABB(x + min, y, z - 1, x + max, y + top, z + 2)
			: new AABB(x - 1, y, z + min, x + 2, y + top, z + max);
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	private static float jag(long seed, int index, float time) {
		double phase = index * 12.9898 + (seed & 1023) * 0.017 + time * 2.2;
		return 0.10f + 0.24f * (0.5f + 0.5f * (float) Math.sin(phase));
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private static void quad(VertexConsumer consumer, PoseStack.Pose pose, boolean northSouth,
			float h0, float y0, float h1, float y1, float depth,
			int red, int green, int blue, int alpha) {
		vertex(consumer, pose, northSouth, h0, y0, depth, 0, 0, red, green, blue, alpha);
		vertex(consumer, pose, northSouth, h1, y0, depth, 1, 0, red, green, blue, alpha);
		vertex(consumer, pose, northSouth, h1, y1, depth, 1, 1, red, green, blue, alpha);
		vertex(consumer, pose, northSouth, h0, y1, depth, 0, 1, red, green, blue, alpha);
	}

	private static void diamond(VertexConsumer consumer, PoseStack.Pose pose, boolean northSouth,
			float h, float y, float depth, float halfWidth, float halfHeight,
			int red, int green, int blue, int alpha) {
		vertex(consumer, pose, northSouth, h, y - halfHeight, depth, 0.5f, 0, red, green, blue, alpha);
		vertex(consumer, pose, northSouth, h + halfWidth, y, depth, 1, 0.5f, red, green, blue, alpha);
		vertex(consumer, pose, northSouth, h, y + halfHeight, depth, 0.5f, 1, red, green, blue, alpha);
		vertex(consumer, pose, northSouth, h - halfWidth, y, depth, 0, 0.5f, red, green, blue, alpha);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, boolean northSouth,
			float horizontal, float y, float depth, float u, float v,
			int red, int green, int blue, int alpha) {
		float x = northSouth ? horizontal : depth;
		float z = northSouth ? depth : horizontal;
		consumer.addVertex(pose, x, y, z)
			.setColor(red, green, blue, alpha)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(LightTexture.FULL_BRIGHT)
			.setNormal(pose, northSouth ? 0 : 1, 0, northSouth ? 1 : 0);
	}
}
