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
import net.minecraft.client.gui.GuiGraphics;

import net.createteleporters.world.inventory.BlockTeleporterGuiMenu;
import net.createteleporters.procedures.FluidDisplayProcedure;
import net.createteleporters.network.BlockTeleporterGuiButtonMessage;
import net.createteleporters.init.CreateteleportersModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class BlockTeleporterGuiScreen extends AbstractContainerScreen<BlockTeleporterGuiMenu> implements CreateteleportersModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	ImageButton imagebutton_check;

	public BlockTeleporterGuiScreen(BlockTeleporterGuiMenu container, Inventory inventory, Component text) {
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
		guiGraphics.drawString(this.font, Component.translatable("gui.createteleporters.block_teleporter_gui.label_tp_link"), 45, 20, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.createteleporters.block_teleporter_gui.label_entity_teleporter"), -1, -22, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
		imagebutton_check = new ImageButton(this.leftPos + 159, this.topPos + 56, 18, 18,
				new WidgetSprites(ResourceLocation.parse("createteleporters:textures/screens/check.png"), ResourceLocation.parse("createteleporters:textures/screens/check_hover.png")), e -> {
					int x = BlockTeleporterGuiScreen.this.x;
					int y = BlockTeleporterGuiScreen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new BlockTeleporterGuiButtonMessage(0, x, y, z));
						BlockTeleporterGuiButtonMessage.handleButtonAction(entity, 0, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
				guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_check);
	}
}