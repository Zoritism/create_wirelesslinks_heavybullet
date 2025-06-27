package com.zoritism.wirelesslinks.content.redstone.link.controller;

import java.util.*;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zoritism.wirelesslinks.registry.ModItems;
import com.zoritism.wirelesslinks.foundation.network.ModPackets;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Клиентская логика контроллера: управление сигналом строго по событиям PRESS/RELEASE.
 * Теперь работает и в основной, и во второй руке!
 */
@Mod.EventBusSubscriber(modid = "wirelesslinks", value = Dist.CLIENT)
public class LinkedControllerClientHandler {

	public static final IGuiOverlay OVERLAY = LinkedControllerClientHandler::renderOverlay;

	public static Mode MODE = Mode.IDLE;
	public static int PACKET_RATE = 5;
	public static Collection<Integer> currentlyPressed = new HashSet<>();
	private static BlockPos lecternPos = null;
	private static BlockPos selectedLocation = BlockPos.ZERO;
	private static int packetCooldown = 0;
	private static boolean f5Pressed = false;
	private static ItemStack lastHeldController = ItemStack.EMPTY;

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
		// Активировать можно только если контроллер в одной из рук!
		if (!isControllerInEitherHand()) {
			return;
		}
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
		for (int i : currentlyPressed) {
			sendControlChannelPacket(i, false);
		}
		currentlyPressed.clear();

		Vector<KeyMapping> controls = DefaultControls.getControls();
		for (KeyMapping mapping : controls) {
			try {
				mapping.setDown(false);
			} catch (Exception ignored) {}
		}
		packetCooldown = 0;
		selectedLocation = BlockPos.ZERO;
		lecternPos = null;
		f5Pressed = false;
	}

	@SubscribeEvent
	public static void onKeyInput(InputEvent.Key event) {
		if (event.getKey() == GLFW.GLFW_KEY_F5) {
			if (event.getAction() == GLFW.GLFW_PRESS) {
				f5Pressed = true;
			} else if (event.getAction() == GLFW.GLFW_RELEASE) {
				f5Pressed = false;
				sendTestPacket(false);
			}
			return;
		}

		if (MODE == Mode.ACTIVE && isControllerInEitherHand()) {
			Vector<KeyMapping> controls = DefaultControls.getControls();
			for (int i = 0; i < controls.size(); i++) {
				KeyMapping mapping = controls.get(i);
				if (mapping.matches(event.getKey(), event.getScanCode())) {
					try {
						mapping.setDown(false);
					} catch (Exception ignored) {}

					if (event.getAction() == GLFW.GLFW_PRESS) {
						if (currentlyPressed.add(i)) {
							sendControlChannelPacket(i, true);
						}
					} else if (event.getAction() == GLFW.GLFW_RELEASE) {
						if (currentlyPressed.remove(i)) {
							sendControlChannelPacket(i, false);
						}
					}
					return;
				}
			}
		}
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			if (MODE != Mode.IDLE) {
				MODE = Mode.IDLE;
				onReset();
			}
			return;
		}

		boolean isController = isControllerInEitherHand();
		if (MODE == Mode.ACTIVE && !isController) {
			MODE = Mode.IDLE;
			onReset();
		}
		if (MODE == Mode.ACTIVE && !hasControllerAnywhere(player)) {
			MODE = Mode.IDLE;
			onReset();
		}

		lastHeldController = isController ? getControllerFromEitherHand(player) : ItemStack.EMPTY;

		// === ДОБАВЛЕНО: вызывать тик рендера контроллера ===
		LinkedControllerItemRenderer.tick();
		// === КОНЕЦ ДОБАВЛЕНИЯ ===

		tick();
	}

	private static boolean isControllerInEitherHand() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return false;
		ItemStack main = mc.player.getMainHandItem();
		ItemStack off = mc.player.getOffhandItem();
		return main.is(ModItems.LINKED_CONTROLLER.get()) || off.is(ModItems.LINKED_CONTROLLER.get());
	}

	private static ItemStack getControllerFromEitherHand(LocalPlayer player) {
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		if (main.is(ModItems.LINKED_CONTROLLER.get())) return main;
		if (off.is(ModItems.LINKED_CONTROLLER.get())) return off;
		return ItemStack.EMPTY;
	}

	private static boolean hasControllerAnywhere(LocalPlayer player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(ModItems.LINKED_CONTROLLER.get()))
				return true;
		}
		ItemStack offhand = player.getOffhandItem();
		if (offhand.is(ModItems.LINKED_CONTROLLER.get()))
			return true;
		return false;
	}

	private static void sendControlChannelPacket(int channel, boolean pressed) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player != null) {
			ItemStack heldItem = player.getMainHandItem();
			if (!heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
				heldItem = player.getOffhandItem();
			}
			if (heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
				ItemStackHandler inv = LinkedControllerItem.getFrequencyInventory(heldItem);

				int aIdx = channel * 2;
				int bIdx = aIdx + 1;
				ItemStack a = inv.getStackInSlot(aIdx);
				ItemStack b = inv.getStackInSlot(bIdx);
				if (!a.isEmpty() || !b.isEmpty()) {
					List<Couple<ItemStack>> frequencyCouples = List.of(Couple.of(a, b));
					ModPackets.getChannel().sendToServer(new LinkedControllerInputPacket(
							List.of(channel), pressed
					));
				}
			}
		}
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (MODE != Mode.ACTIVE || !isControllerInEitherHand())
			return;
		if (packetCooldown > 0)
			packetCooldown--;

		if (player == null || player.isSpectator()) {
			MODE = Mode.IDLE;
			onReset();
			return;
		}

		ItemStack heldItem = getControllerFromEitherHand(player);
		if (heldItem.isEmpty()) {
			MODE = Mode.IDLE;
			onReset();
			return;
		}

		if (mc.screen != null || InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
			MODE = Mode.IDLE;
			onReset();
			return;
		}

		// --- Новый блок: отправка held сигналов каждый тик для всех нажатых кнопок ---
		if (!currentlyPressed.isEmpty()) {
			for (int i : currentlyPressed) {
				sendControlChannelPacket(i, true);
			}
		}

		if (f5Pressed && packetCooldown == 0) {
			sendTestPacket(true);
			packetCooldown = PACKET_RATE;
		}
	}

	public static void sendTestPacket(boolean powered) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player != null) {
			ItemStack heldItem = getControllerFromEitherHand(player);
			if (heldItem.isEmpty()) {
				return;
			}
			ItemStackHandler inv = LinkedControllerItem.getFrequencyInventory(heldItem);
			int slotCount = 12;
			List<Couple<ItemStack>> frequencyCouples = new ArrayList<>();
			for (int logicalSlot = 0; logicalSlot < slotCount / 2; logicalSlot++) {
				int aIdx = logicalSlot * 2;
				int bIdx = aIdx + 1;
				ItemStack a = inv.getStackInSlot(aIdx);
				ItemStack b = inv.getStackInSlot(bIdx);
				if (!a.isEmpty() || !b.isEmpty()) {
					frequencyCouples.add(Couple.of(a, b));
				}
			}
			if (!frequencyCouples.isEmpty()) {
				ModPackets.getChannel().sendToServer(new LinkedControllerInputPacket(
						makeButtonIndices(frequencyCouples), powered
				));
			}
		}
	}

	private static List<Integer> makeButtonIndices(List<Couple<ItemStack>> couples) {
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < couples.size(); i++) {
			indices.add(i);
		}
		return indices;
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