package net.createteleporters.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import net.createteleporters.world.inventory.CustomTeleporterGuiMenu;
import net.createteleporters.procedures.FluidDisplayProcedure;
import net.createteleporters.procedures.CustomPortalBaseOnTickUpdateProcedure;
import net.createteleporters.network.CustomTeleporterGuiButtonMessage;
import net.createteleporters.init.CreateteleportersModScreens;
import net.createteleporters.util.CustomPortalTeleportMode;

import com.mojang.blaze3d.systems.RenderSystem;

public class CustomTeleporterGuiScreen extends AbstractContainerScreen<CustomTeleporterGuiMenu> implements CreateteleportersModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	ImageButton imagebutton_check;
	Button button_mode;

	public CustomTeleporterGuiScreen(CustomTeleporterGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		menuStateUpdateActive = false;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (button_mode != null && button_mode.isMouseOver(mouseX, mouseY)) {
			guiGraphics.renderTooltip(this.font, Component.translatable("gui.createteleporters.custom_teleporter_gui.mode_tooltip"), mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(ResourceLocation.parse("createteleporters:textures/screens/custom_tp_gui.png"), this.leftPos + -8, this.topPos + -29, 0, 0, 192, 195, 192, 195);
		guiGraphics.blit(ResourceLocation.parse("createteleporters:textures/screens/empty_tank.png"), this.leftPos + -68, this.topPos + -22, 0, 0, 95, 95, 95, 95);
		guiGraphics.blit(ResourceLocation.parse("createteleporters:textures/screens/tank_sprite.png"), this.leftPos + -68, this.topPos + -22, Mth.clamp((int) FluidDisplayProcedure.execute(world, x, y, z) * 95, 0, 1615), 0, 95, 95, 1710, 95);
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.createteleporters.custom_teleporter_gui.label_tp_link"), 45, 20, -1, false);
		guiGraphics.drawString(this.font, CustomPortalBaseOnTickUpdateProcedure.execute(world, x, y, z), 0, 61, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.createteleporters.custom_teleporter_gui.label_entity_teleporter"), -1, -22, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
		imagebutton_check = new ImageButton(this.leftPos + 159, this.topPos + 56, 18, 18,
				new WidgetSprites(ResourceLocation.parse("createteleporters:textures/screens/check.png"), ResourceLocation.parse("createteleporters:textures/screens/check_hover.png")), e -> {
					int x = CustomTeleporterGuiScreen.this.x;
					int y = CustomTeleporterGuiScreen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new CustomTeleporterGuiButtonMessage(0, x, y, z));
						CustomTeleporterGuiButtonMessage.handleButtonAction(entity, 0, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
				guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_check);
		button_mode = new Button(this.leftPos + 134, this.topPos + 16, 36, 16, getModeLabel(), e -> {
			int x = CustomTeleporterGuiScreen.this.x;
			int y = CustomTeleporterGuiScreen.this.y;
			PacketDistributor.sendToServer(new CustomTeleporterGuiButtonMessage(1, x, y, z));
			CustomTeleporterGuiButtonMessage.handleButtonAction(entity, 1, x, y, z);
			button_mode.setMessage(getModeLabel());
		}, supplier -> supplier.get()) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				String mode = CustomPortalTeleportMode.getOrMigrate(world, new BlockPos(CustomTeleporterGuiScreen.this.x, CustomTeleporterGuiScreen.this.y, z));
				boolean portalMode = CustomPortalTeleportMode.PORTAL_TO_PORTAL.equals(mode);
				int trackX = getX();
				int trackY = getY() + 3;
				int trackW = 26;
				int trackH = 11;
				int trackColor = portalMode ? 0xFF2F8F5B : 0xFF555B66;
				int borderColor = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFB8C0CC;
				int knobW = 8;
				int knobX = portalMode ? trackX + trackW - knobW - 2 : trackX + 2;

				guiGraphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF15191F);
				guiGraphics.fill(trackX + 1, trackY + 1, trackX + trackW - 1, trackY + trackH - 1, trackColor);
				guiGraphics.fill(trackX, trackY, trackX + trackW, trackY + 1, borderColor);
				guiGraphics.fill(trackX, trackY + trackH - 1, trackX + trackW, trackY + trackH, borderColor);
				guiGraphics.fill(trackX, trackY, trackX + 1, trackY + trackH, borderColor);
				guiGraphics.fill(trackX + trackW - 1, trackY, trackX + trackW, trackY + trackH, borderColor);
				guiGraphics.fill(knobX, trackY + 2, knobX + knobW, trackY + trackH - 2, 0xFFF4F7FA);
			}
		};
		this.addRenderableWidget(button_mode);
	}

	private Component getModeLabel() {
		return CustomPortalTeleportMode.displayName(CustomPortalTeleportMode.getOrMigrate(world, new BlockPos(x, y, z)));
	}
}
