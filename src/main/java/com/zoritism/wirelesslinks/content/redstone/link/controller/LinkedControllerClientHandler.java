package com.zoritism.wirelesslinks.content.redstone.link.controller;

import java.util.*;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zoritism.wirelesslinks.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.zoritism.wirelesslinks.foundation.network.ModPackets;
import com.zoritism.wirelesslinks.content.redstone.link.controller.LinkedControllerInputPacket;

public class LinkedControllerClientHandler {

	public static final IGuiOverlay OVERLAY = LinkedControllerClientHandler::renderOverlay;
	private static final Logger LOGGER = LogManager.getLogger();

	public static Mode MODE = Mode.IDLE;
	public static int PACKET_RATE = 5;
	public static Collection<Integer> currentlyPressed = new HashSet<>();
	private static BlockPos lecternPos = null;
	private static BlockPos selectedLocation = BlockPos.ZERO;
	private static int packetCooldown = 0;

	public static void toggleBindMode(BlockPos location) {
		if (MODE == Mode.IDLE) {
			MODE = Mode.BIND;
			selectedLocation = location;
			LOGGER.info("[Client] toggleBindMode: BIND mode enabled, selectedLocation={}", location);
		} else {
			MODE = Mode.IDLE;
			onReset();
			LOGGER.info("[Client] toggleBindMode: Switched to IDLE mode");
		}
	}

	public static void toggle() {
		if (MODE == Mode.IDLE) {
			MODE = Mode.ACTIVE;
			lecternPos = null;
			LOGGER.info("[Client] toggle: Switched to ACTIVE mode");
		} else {
			MODE = Mode.IDLE;
			onReset();
			LOGGER.info("[Client] toggle: Switched to IDLE mode");
		}
	}

	public static void activateInLectern(BlockPos lecternAt) {
		if (MODE == Mode.IDLE) {
			MODE = Mode.ACTIVE;
			lecternPos = lecternAt;
			LOGGER.info("[Client] activateInLectern: Activated in lectern at {}", lecternAt);
		}
	}

	public static void deactivateInLectern() {
		if (MODE == Mode.ACTIVE && inLectern()) {
			MODE = Mode.IDLE;
			onReset();
			LOGGER.info("[Client] deactivateInLectern: Switched to IDLE mode from lectern");
		}
	}

	public static boolean inLectern() {
		return lecternPos != null;
	}

	protected static void onReset() {
		DefaultControls.getControls().forEach(kb -> kb.setDown(kb.isDown()));
		packetCooldown = 0;
		selectedLocation = BlockPos.ZERO;
		lecternPos = null;
		currentlyPressed.clear();
		LOGGER.info("[Client] onReset: State cleared (packetCooldown=0, selectedLocation=ZERO, lecternPos=null, currentlyPressed cleared)");
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (MODE == Mode.IDLE)
			return;
		if (packetCooldown > 0)
			packetCooldown--;

		if (player == null || player.isSpectator()) {
			MODE = Mode.IDLE;
			onReset();
			LOGGER.info("[Client] tick: Player is null or spectator, switched to IDLE and reset");
			return;
		}

		ItemStack heldItem = player.getMainHandItem();
		if (!inLectern() && !heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
			heldItem = player.getOffhandItem();
			if (!heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
				MODE = Mode.IDLE;
				onReset();
				LOGGER.info("[Client] tick: No linked controller in hand, switched to IDLE and reset");
				return;
			}
		}

		if (mc.screen != null || InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
			MODE = Mode.IDLE;
			onReset();
			LOGGER.info("[Client] tick: Some screen opened or ESC pressed, switched to IDLE and reset");
			return;
		}

		Vector<KeyMapping> controls = DefaultControls.getControls();
		Set<Integer> pressedKeys = new HashSet<>();
		for (int i = 0; i < controls.size(); i++) {
			if (controls.get(i).isDown())
				pressedKeys.add(i);
		}

		Set<Integer> newKeys = new HashSet<>(pressedKeys);
		Set<Integer> releasedKeys = new HashSet<>(currentlyPressed);
		newKeys.removeAll(currentlyPressed);
		releasedKeys.removeAll(pressedKeys);

		if (MODE == Mode.ACTIVE) {
			// === Реальная отправка пакетов на сервер ===
			if (!releasedKeys.isEmpty()) {
				LOGGER.info("[Client] tick: releasedKeys={}", releasedKeys);
				ModPackets.getChannel().sendToServer(new LinkedControllerInputPacket(releasedKeys, false, getControllerPos(player)));
			}
			if (!newKeys.isEmpty()) {
				LOGGER.info("[Client] tick: newKeys={}", newKeys);
				ModPackets.getChannel().sendToServer(new LinkedControllerInputPacket(newKeys, true, getControllerPos(player)));
				packetCooldown = PACKET_RATE;
			}
			if (packetCooldown == 0 && !pressedKeys.isEmpty()) {
				LOGGER.info("[Client] tick: keepalive for pressedKeys={}", pressedKeys);
				ModPackets.getChannel().sendToServer(new LinkedControllerInputPacket(pressedKeys, true, getControllerPos(player)));
				packetCooldown = PACKET_RATE;
			}
		}

		if (MODE == Mode.BIND) {
			for (Integer integer : newKeys) {
				LOGGER.info("[Client] tick: Bind mode, key pressed: {} (binding to block {})", integer, selectedLocation);
				// Можно реализовать отдельный пакет для бинда
				MODE = Mode.IDLE;
				break;
			}
		}

		currentlyPressed = pressedKeys;

		// Сбросить нажатие, чтобы движения игрока не происходили
		controls.forEach(kb -> kb.setDown(false));
	}

	private static BlockPos getControllerPos(LocalPlayer player) {
		return inLectern() ? lecternPos : player.blockPosition();
	}

	public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int screenWidth, int screenHeight) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui)
			return;
		if (MODE != Mode.BIND)
			return;

		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();

		Screen tooltipScreen = new Screen(CommonComponents.EMPTY) {};
		tooltipScreen.init(mc, screenWidth, screenHeight);

		Vector<KeyMapping> controls = DefaultControls.getControls();
		List<Component> list = new ArrayList<>();
		list.add(Component.literal("Linked Controller: Bind Mode").withStyle(ChatFormatting.GOLD));
		list.add(Component.literal("Press any movement key to bind."));

		int width = 0;
		int height = list.size() * mc.font.lineHeight;
		for (Component c : list)
			width = Math.max(width, mc.font.width(c));

		int x = (screenWidth / 3) - (width / 2);
		int y = screenHeight - height - 24;

		graphics.renderComponentTooltip(mc.font, list, x, y);
		poseStack.popPose();
	}

	public enum Mode {
		IDLE, ACTIVE, BIND
	}
}