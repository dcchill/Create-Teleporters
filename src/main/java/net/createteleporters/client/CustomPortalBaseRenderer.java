package net.createteleporters.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createteleporters.CreateteleportersMod;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;

public class CustomPortalBaseRenderer implements BlockEntityRenderer<CustomPortalBaseBlockEntity> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
		CreateteleportersMod.MODID, "block/p_portal");
	private static final int SEGMENTS = 12;
	private static final float BORDER_OVERLAP = 1f / 16f;
	private static final float[] BORDER_DEPTHS = {6.5f / 16, 9.5f / 16};

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
		float outerMin = min - BORDER_OVERLAP;
		float outerMax = max + BORDER_OVERLAP;
		float bottom = 1 - BORDER_OVERLAP;
		float upper = top + BORDER_OVERLAP;

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
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TEXTURE);
		ResourceLocation atlas = TextureAtlas.LOCATION_BLOCKS;

		poseStack.pushPose();
		PoseStack.Pose pose = poseStack.last();
		VertexConsumer rim = buffers.getBuffer(RenderType.entityTranslucentEmissive(atlas, false));
		for (int i = 0; i < SEGMENTS; i++) {
			float a = i / (float) SEGMENTS;
			float b = (i + 1) / (float) SEGMENTS;
			float h0 = lerp(outerMin, outerMax, a);
			float h1 = lerp(outerMin, outerMax, b);
			float y0 = lerp(bottom, upper, a);
			float y1 = lerp(bottom, upper, b);
			float jagA = jag(seed, i, time);
			float jagB = jag(seed, i + 1, time);
			int alpha = (int) (255 * pulse);

			float thickness = Math.max(jagA, jagB);
			for (float depth : BORDER_DEPTHS) {
				quad(rim, pose, sprite, northSouth, h0, bottom, h1, 1 + thickness, depth,
					red, green, blue, alpha);
				quad(rim, pose, sprite, northSouth, h0, top - thickness, h1, upper, depth,
					red, green, blue, alpha);
				quad(rim, pose, sprite, northSouth, outerMin, y0, min + thickness, y1, depth,
					red, green, blue, alpha);
				quad(rim, pose, sprite, northSouth, max - thickness, y0, outerMax, y1, depth,
					red, green, blue, alpha);
			}
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
			? new AABB(x + min - BORDER_OVERLAP, y, z + BORDER_DEPTHS[0], x + max + BORDER_OVERLAP, y + top + 1, z + BORDER_DEPTHS[1])
			: new AABB(x + BORDER_DEPTHS[0], y, z + min - BORDER_OVERLAP, x + BORDER_DEPTHS[1], y + top + 1, z + max + BORDER_OVERLAP);
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	private static float jag(long seed, int index, float time) {
		double phase = index * 12.9898 + (seed & 1023) * 0.017 + time * 2.2;
		return 0.18f + 0.24f * (0.5f + 0.5f * (float) Math.sin(phase));
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private static void quad(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, boolean northSouth,
			float h0, float y0, float h1, float y1, float depth,
			int red, int green, int blue, int alpha) {
		int horizontalSegments = Math.max(1, (int) Math.ceil(h1 - h0));
		int verticalSegments = Math.max(1, (int) Math.ceil(y1 - y0));
		for (int x = 0; x < horizontalSegments; x++) {
			float xa = lerp(h0, h1, x / (float) horizontalSegments);
			float xb = lerp(h0, h1, (x + 1f) / horizontalSegments);
			float u0 = sprite.getU0();
			float u1 = lerp(u0, sprite.getU1(), xb - xa);
			for (int y = 0; y < verticalSegments; y++) {
				float ya = lerp(y0, y1, y / (float) verticalSegments);
				float yb = lerp(y0, y1, (y + 1f) / verticalSegments);
				float v0 = sprite.getV0();
				float v1 = lerp(v0, sprite.getV1(), yb - ya);
				vertex(consumer, pose, northSouth, xa, ya, depth, u0, v0, red, green, blue, alpha);
				vertex(consumer, pose, northSouth, xb, ya, depth, u1, v0, red, green, blue, alpha);
				vertex(consumer, pose, northSouth, xb, yb, depth, u1, v1, red, green, blue, alpha);
				vertex(consumer, pose, northSouth, xa, yb, depth, u0, v1, red, green, blue, alpha);
			}
		}
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
