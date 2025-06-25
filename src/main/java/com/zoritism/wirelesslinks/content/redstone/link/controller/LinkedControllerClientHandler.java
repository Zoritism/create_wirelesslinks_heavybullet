package com.zoritism.wirelesslinks.content.redstone.link.controller;

import java.util.*;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zoritism.wirelesslinks.registry.ModItems;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency.FrequencyPair;
import com.zoritism.wirelesslinks.util.Couple;

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

public class LinkedControllerClientHandler {

	public static final IGuiOverlay OVERLAY = LinkedControllerClientHandler::renderOverlay;

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
		} else {
			MODE = Mode.IDLE;
			onReset();
		}
	}

	public static void toggle() {
		if (MODE == Mode.IDLE) {
			MODE = Mode.ACTIVE;
			lecternPos = null;
		} else {
			MODE = Mode.IDLE;
			onReset();
		}
	}

	public static void activateInLectern(BlockPos lecternAt) {
		if (MODE == Mode.IDLE) {
			MODE = Mode.ACTIVE;
			lecternPos = lecternAt;
		}
	}

	public static void deactivateInLectern() {
		if (MODE == Mode.ACTIVE && inLectern()) {
			MODE = Mode.IDLE;
			onReset();
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
		// TODO: сброс визуальных кнопок, если требуется
	}

	public static void tick() {
		// TODO: если есть кастомный рендерер, вызовите его tick() тут

		// === ТЕСТ: делать всегда активный сигнал для всех частот, что указаны в контроллере ===
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player != null) {
			ItemStack heldItem = player.getMainHandItem();
			if (!heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
				heldItem = player.getOffhandItem();
			}
			if (heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
				// Количество логических слотов (6 пар = 12 слотов)
				int slotCount = 12;
				for (int logicalSlot = 0; logicalSlot < slotCount / 2; logicalSlot++) {
					FrequencyPair pair = LinkedControllerItem.slotToFrequency(heldItem, logicalSlot);
					// Если хоть одна из частот не пустая, считаем что нужно активировать сигнал
					if (!pair.getFirst().getStack().isEmpty() || !pair.getSecond().getStack().isEmpty()) {
						Couple<RedstoneLinkFrequency.Frequency> couple = Couple.of(pair.getFirst(), pair.getSecond());
						// Всегда true (active)
						LinkedControllerServerHandler.receivePressed(
								player.level(), player.blockPosition(), player.getUUID(),
								Collections.singletonList(couple), true
						);
					} else {
						// Если частоты пустые — сигнал не отправляем (или можно явно выключить)
						Couple<RedstoneLinkFrequency.Frequency> couple = Couple.of(pair.getFirst(), pair.getSecond());
						LinkedControllerServerHandler.receivePressed(
								player.level(), player.blockPosition(), player.getUUID(),
								Collections.singletonList(couple), false
						);
					}
				}
			}
		}
		// ===== Оригинальная логика ниже =====

		if (MODE == Mode.IDLE)
			return;
		if (packetCooldown > 0)
			packetCooldown--;

		if (player == null || player.isSpectator()) {
			MODE = Mode.IDLE;
			onReset();
			return;
		}

		ItemStack heldItem = player.getMainHandItem();
		if (!inLectern() && !heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
			heldItem = player.getOffhandItem();
			if (!heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
				MODE = Mode.IDLE;
				onReset();
				return;
			}
		}

		if (mc.screen != null || InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
			MODE = Mode.IDLE;
			onReset();
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
			// TODO: отправка пакета releasedKeys (false)
			// TODO: отправка пакета newKeys (true)
			// TODO: keepalive packet для всех нажатых pressedKeys (true)
			if (!releasedKeys.isEmpty()) {
				// send released packet
			}
			if (!newKeys.isEmpty()) {
				// send pressed packet
				packetCooldown = PACKET_RATE;
			}
			if (packetCooldown == 0 && !pressedKeys.isEmpty()) {
				// send keepalive packet
				packetCooldown = PACKET_RATE;
			}
		}

		if (MODE == Mode.BIND) {
			// TODO: визуализация выделения блока (shape), если требуется
			for (Integer integer : newKeys) {
				// TODO: отправка пакета бинда (integer, selectedLocation)
				MODE = Mode.IDLE;
				break;
			}
		}

		currentlyPressed = pressedKeys;

		// Сбросить нажатие, чтобы движения игрока не происходили
		controls.forEach(kb -> kb.setDown(false));
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