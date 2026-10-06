package net.createteleporters.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createteleporters.CreateteleportersMod;
import net.createteleporters.network.CustomPortalEffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = CreateteleportersMod.MODID, value = Dist.CLIENT)
public final class CustomPortalEffects {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
		CreateteleportersMod.MODID, "block/p_portal");
	private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS, false);
	private static byte phase = CustomPortalEffectPayload.CANCEL;
	private static int age = 1;
	private static int duration = 1;
	private static int color = 0xB24CFF;
	private static float cancelStart;

	private CustomPortalEffects() {
	}

	public static void accept(CustomPortalEffectPayload message) {
		color = message.color();
		if (message.phase() == CustomPortalEffectPayload.CANCEL) {
			cancelStart = intensity(0);
			duration = 5;
		} else {
			duration = message.duration() > 0 ? message.duration() : message.phase() == CustomPortalEffectPayload.CHARGE ? 11 : 15;
		}
		phase = message.phase();
		age = 0;
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		if (!Minecraft.getInstance().isPaused() && age < duration)
			age++;
	}

	@SubscribeEvent
	public static void renderWorld(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null)
			return;
		float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float strength = intensity(partial) * minecraft.options.screenEffectScale().get().floatValue();
		if (strength <= 0.001f)
			return;

		int red = color >> 16 & 255;
		int green = color >> 8 & 255;
		int blue = color & 255;
		if (phase == CustomPortalEffectPayload.ARRIVAL)
			renderExitFlash(event, minecraft, strength, red, green, blue);
		double px = Mth.lerp(partial, player.xo, player.getX()) - event.getCamera().getPosition().x;
		double py = Mth.lerp(partial, player.yo, player.getY()) - event.getCamera().getPosition().y + 1.0;
		double pz = Mth.lerp(partial, player.zo, player.getZ()) - event.getCamera().getPosition().z;
		float progress = Mth.clamp((age + partial) / duration, 0, 1);
		float radius = phase == CustomPortalEffectPayload.ARRIVAL
			? 0.25f + progress * 2.8f
			: phase == CustomPortalEffectPayload.CHARGE ? 1.8f - progress * 1.45f : 1.4f;
		int alpha = Mth.clamp((int) (strength * 255), 0, 255);

		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(px, py, pz);
		MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(TEXTURE).wrap(buffers.getBuffer(RENDER_TYPE));
		for (int plane = 0; plane < 3; plane++) {
			for (int i = 0; i < 16; i++) {
				float spin = progress * (phase == CustomPortalEffectPayload.CHARGE ? -2.2f : 2.6f);
				float a = (float) (i * Math.PI * 2 / 16.0) + spin;
				float b = (float) ((i + 1) * Math.PI * 2 / 16.0) + spin;
				float wobble = 1.0f + 0.11f * (float) Math.sin(i * 2.37 + plane * 1.8 + progress * 7.0);
				ringQuad(consumer, poseStack.last(), plane, a, b, radius * 0.84f * wobble, radius * 1.04f * wobble,
					red, green, blue, alpha, i / 16f, (i + 1) / 16f);
			}
		}
		for (int i = 0; i < 12; i++) {
			float a = i * 0.91f + progress * (i % 2 == 0 ? 3.0f : -2.0f);
			float r = radius * (0.7f + 0.3f * (float) Math.sin(i * 4.1));
			float x = (float) Math.cos(a) * r;
			float z = (float) Math.sin(a) * r;
			float y = (float) Math.sin(a * 1.7f + progress * 4) * 0.8f;
			shard(consumer, poseStack.last(), x, y, z, 0.07f + i % 3 * 0.035f,
				red, green, blue, Math.max(40, alpha - i * 8));
		}
		buffers.endBatch(RENDER_TYPE);
		poseStack.popPose();
	}

	@SubscribeEvent
	public static void camera(ViewportEvent.ComputeCameraAngles event) {
		float strength = intensity((float) event.getPartialTick())
			* Minecraft.getInstance().options.screenEffectScale().get().floatValue();
		if (strength <= 0.001f)
			return;
		double time = age + event.getPartialTick();
		event.setRoll(event.getRoll() + (float) Math.sin(time * 1.8) * 2.5f * strength);
		event.setPitch(event.getPitch() + (float) Math.sin(time * 2.7) * 0.75f * strength);
	}

	private static float intensity(float partialTick) {
		if (age >= duration)
			return 0;
		float progress = Mth.clamp((age + partialTick) / duration, 0, 1);
		if (phase == CustomPortalEffectPayload.CHARGE)
			return progress * progress;
		if (phase == CustomPortalEffectPayload.ARRIVAL)
			return (1 - progress) * (1 - progress);
		return cancelStart * (1 - progress);
	}

	private static void renderExitFlash(RenderLevelStageEvent event, Minecraft minecraft, float strength,
			int red, int green, int blue) {
		int alpha = Mth.clamp((int) (strength * 190), 0, 190);
		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		var look = event.getCamera().getLookVector();
		poseStack.translate(look.x(), look.y(), look.z());
		poseStack.mulPose(event.getCamera().rotation());
		MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(TEXTURE).wrap(buffers.getBuffer(RENDER_TYPE));
		consumer.addVertex(poseStack.last(), -3, -3, 0).setColor(red, green, blue, alpha).setUv(0, 0)
			.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0, 0, 1);
		consumer.addVertex(poseStack.last(), 3, -3, 0).setColor(red, green, blue, alpha).setUv(1, 0)
			.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0, 0, 1);
		consumer.addVertex(poseStack.last(), 3, 3, 0).setColor(red, green, blue, alpha).setUv(1, 1)
			.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0, 0, 1);
		consumer.addVertex(poseStack.last(), -3, 3, 0).setColor(red, green, blue, alpha).setUv(0, 1)
			.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0, 0, 1);
		buffers.endBatch(RENDER_TYPE);
		poseStack.popPose();
	}


	private static void ringQuad(VertexConsumer consumer, PoseStack.Pose pose, int plane, float a, float b,
			float inner, float outer, int red, int green, int blue, int alpha, float u0, float u1) {
		vertex(consumer, pose, plane, a, inner, u0, 0, red, green, blue, alpha);
		vertex(consumer, pose, plane, a, outer, u0, 1, red, green, blue, alpha);
		vertex(consumer, pose, plane, b, outer, u1, 1, red, green, blue, alpha);
		vertex(consumer, pose, plane, b, inner, u1, 0, red, green, blue, alpha);
	}

	private static void shard(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
			float size, int red, int green, int blue, int alpha) {
		consumer.addVertex(pose, x - size, y, z).setColor(red, green, blue, alpha).setUv(0, 0)
			.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
		consumer.addVertex(pose, x, y + size * 2, z).setColor(red, green, blue, alpha).setUv(0.5f, 1)
			.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
		consumer.addVertex(pose, x + size, y, z).setColor(red, green, blue, alpha).setUv(1, 0)
			.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
		consumer.addVertex(pose, x, y - size * 2, z).setColor(red, green, blue, alpha).setUv(0.5f, 0)
			.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int plane, float angle, float radius,
			float u, float v, int red, int green, int blue, int alpha) {
		float c = (float) Math.cos(angle) * radius;
		float s = (float) Math.sin(angle) * radius;
		float x = plane == 0 ? c : 0;
		float y = plane == 1 ? c : plane == 0 ? s : 0;
		float z = plane == 2 ? s : plane == 1 ? s : 0;
		consumer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
	}
}
