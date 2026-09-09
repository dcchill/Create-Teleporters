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
import net.minecraft.client.gui.components.Tooltip;
import net.createteleporters.util.CustomPortalTeleportMode;
import net.minecraft.client.gui.GuiGraphics;

import net.createteleporters.world.inventory.CustomTeleporterGuiMenu;
import net.createteleporters.procedures.FluidDisplayProcedure;
import net.createteleporters.procedures.CustomPortalBaseOnTickUpdateProcedure;
import net.createteleporters.network.CustomTeleporterGuiButtonMessage;
import net.createteleporters.init.CreateteleportersModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class CustomTeleporterGuiScreen extends AbstractContainerScreen<CustomTeleporterGuiMenu> implements CreateteleportersModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	ImageButton imagebutton_check;
	private Button modeButton;

	public CustomTeleporterGuiScreen(CustomTeleporterGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 175;
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
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(ResourceLocation.parse("createteleporters:textures/screens/custom_tp_gui.png"), this.leftPos + -8, this.topPos + -29, 0, 0, 192, 204, 192, 204);
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
		guiGraphics.drawString(this.font, CustomPortalTeleportMode.displayName(menu.getTeleportMode()), modeRowX() + 36, 47, 0xFFF6E9FF, true);
		guiGraphics.drawString(this.font, CustomPortalBaseOnTickUpdateProcedure.execute(world, x, y, z), 0, 70, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.createteleporters.custom_teleporter_gui.label_entity_teleporter"), -1, -22, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
		modeButton = addRenderableWidget(new Button(leftPos + modeRowX(), topPos + 45, 28, 12, modeLabel(), button ->
			PacketDistributor.sendToServer(new CustomTeleporterGuiButtonMessage(1, x, y, z)), narration -> narration.get()) {
			@Override
			public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
				boolean portalMode = CustomPortalTeleportMode.PORTAL_TO_PORTAL.equals(menu.getTeleportMode());
				int trackColor = portalMode ? 0xFF914BCC : 0xFF574365;
				drawSwitchShape(graphics, getX(), getY(), 28, 12, isHoveredOrFocused() ? 0xFFF2B0FF : 0xFFBC79E6);
				drawSwitchShape(graphics, getX() + 1, getY() + 1, 26, 10, trackColor);
				int thumbX = getX() + (portalMode ? 18 : 2);
				drawSwitchShape(graphics, thumbX, getY() + 3, 8, 8, 0xFF392147);
				drawSwitchShape(graphics, thumbX, getY() + 2, 8, 8, 0xFFF6E9FF);
			}
		});
		updateModeButton();
		imagebutton_check = new ImageButton(this.leftPos + 159, this.topPos + 65, 18, 18,
				new WidgetSprites(ResourceLocation.parse("createteleporters:textures/screens/check.png"), ResourceLocation.parse("createteleporters:textures/screens/check_hover.png")), e -> {
					int x = CustomTeleporterGuiScreen.this.x;
					int y = CustomTeleporterGuiScreen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new CustomTeleporterGuiButtonMessage(0, x, y, z));
					}
				}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
				guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_check);
	}

	private Component modeLabel() {
		return Component.translatable("gui.createteleporters.custom_teleporter_gui.mode", CustomPortalTeleportMode.displayName(menu.getTeleportMode()));
	}
	private int modeRowX() {
		int labelWidth = Math.max(
			font.width(CustomPortalTeleportMode.displayName(CustomPortalTeleportMode.COORDINATE)),
			font.width(CustomPortalTeleportMode.displayName(CustomPortalTeleportMode.PORTAL_TO_PORTAL)));
		return (imageWidth - 36 - labelWidth) / 2;
	}
	private static void drawSwitchShape(GuiGraphics graphics, int x, int y, int width, int height, int color) {
		graphics.fill(x + 2, y, x + width - 2, y + height, color);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
		graphics.fill(x, y + 3, x + width, y + height - 3, color);
	}
	private void updateModeButton() {
		modeButton.setMessage(modeLabel());
		modeButton.setTooltip(Tooltip.create(Component.translatable("gui.createteleporters.custom_teleporter_gui.mode_tooltip")));
	}
	@Override
	protected void containerTick() { super.containerTick(); updateModeButton(); }
}
