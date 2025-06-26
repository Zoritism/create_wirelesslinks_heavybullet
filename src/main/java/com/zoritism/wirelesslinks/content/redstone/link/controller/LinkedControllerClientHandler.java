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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = "wirelesslinks", value = Dist.CLIENT)
public class LinkedControllerClientHandler {

	public static final IGuiOverlay OVERLAY = LinkedControllerClientHandler::renderOverlay;
	private static final Logger LOGGER = LogManager.getLogger();

	public static Mode MODE = Mode.IDLE;
	public static int PACKET_RATE = 5;
	public static Collection<Integer> currentlyPressed = new HashSet<>();
	private static BlockPos lecternPos = null;
	private static BlockPos selectedLocation = BlockPos.ZERO;
	private static int packetCooldown = 0;
	private static boolean f5Pressed = false;

	// --- Сохраняем частоты, которые были активны при входе в ACTIVE ---
	private static List<Couple<ItemStack>> lastActiveFrequencies = new ArrayList<>();

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
		// Активировать можно только если контроллер в руке!
		if (!isHeldController()) {
			LOGGER.info("[Client] toggle: Not held, cannot activate");
			return;
		}
		if (MODE == Mode.IDLE) {
			MODE = Mode.ACTIVE;
			lecternPos = null;
			storeActiveFrequencies();
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
			storeActiveFrequencies();
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
		// 1. Если были нажаты каналы — отправить powered=false для всех!
		if (!currentlyPressed.isEmpty()) {
			sendChannelsResetPacket();
		}
		Vector<KeyMapping> controls = DefaultControls.getControls();
		for (KeyMapping mapping : controls) {
			try {
				mapping.setDown(false);
			} catch (Exception ignored) {}
		}
		packetCooldown = 0;
		selectedLocation = BlockPos.ZERO;
		lecternPos = null;
		currentlyPressed.clear();
		f5Pressed = false;
		lastActiveFrequencies.clear();
		LOGGER.info("[Client] onReset: State cleared (packetCooldown=0, selectedLocation=ZERO, lecternPos=null, currentlyPressed cleared, lastActiveFrequencies cleared)");
	}

	/**
	 * Перед входом в ACTIVE режим сохраняем частоты, чтобы потом сбросить сигналы даже если предмет будет выброшен
	 */
	private static void storeActiveFrequencies() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			lastActiveFrequencies = new ArrayList<>();
			return;
		}
		ItemStack heldItem = player.getMainHandItem();
		if (!heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
			heldItem = player.getOffhandItem();
		}
		if (!heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
			lastActiveFrequencies = new ArrayList<>();
			return;
		}
		ItemStackHandler inv = LinkedControllerItem.getFrequencyInventory(heldItem);
		List<Couple<ItemStack>> freq = new ArrayList<>();
		int slotCount = 12; // 6 пар частот
		for (int logicalSlot = 0; logicalSlot < slotCount / 2; logicalSlot++) {
			int aIdx = logicalSlot * 2;
			int bIdx = aIdx + 1;
			ItemStack a = inv.getStackInSlot(aIdx);
			ItemStack b = inv.getStackInSlot(bIdx);
			if (!a.isEmpty() || !b.isEmpty()) {
				freq.add(Couple.of(a.copy(), b.copy()));
			}
		}
		lastActiveFrequencies = freq;
	}

	/**
	 * Отправляет powered=false для всех каналов, которые были зажаты, используя lastActiveFrequencies
	 */
	private static void sendChannelsResetPacket() {
		if (lastActiveFrequencies.isEmpty()) return;
		for (Integer channel : currentlyPressed) {
			if (channel >= 0 && channel < lastActiveFrequencies.size()) {
				ModPackets.getChannel().sendToServer(new LinkedControllerInputPacket(List.of(channel), false));
				LOGGER.info("[Client] [RESET] Sent powered:false for channel {} freq={}", channel, lastActiveFrequencies.get(channel));
			}
		}
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

		// Управление контроллером только если он в руке и активен!
		if (MODE == Mode.ACTIVE && isHeldController()) {
			Vector<KeyMapping> controls = DefaultControls.getControls();
			for (int i = 0; i < controls.size(); i++) {
				KeyMapping mapping = controls.get(i);
				if (mapping.matches(event.getKey(), event.getScanCode())) {
					try {
						mapping.setDown(false); // блокируем движение!
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
		// Проверяем каждый тик, не убрали ли контроллер из руки или не сменили предмет
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
		ItemStack held = player.getMainHandItem();
		boolean isController = held.is(ModItems.LINKED_CONTROLLER.get());
		// Если мы были активны и предмет в руке сменился/убрали контроллер — сбрасываем режим
		if (MODE == Mode.ACTIVE && !isController) {
			MODE = Mode.IDLE;
			onReset();
			LOGGER.info("[Client] Lost controller in hand or changed item, switched to IDLE mode and reset");
		}
		// Остальной тик-логика
		tick();
	}

	private static boolean isHeldController() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return false;
		ItemStack held = mc.player.getMainHandItem();
		return held.is(ModItems.LINKED_CONTROLLER.get());
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
					LOGGER.info("[Client] [CTRL] Channel {}: Sent powered:{} for ({}, {})", channel, pressed, a, b);
				}
			} else {
				LOGGER.info("[Client] [CTRL] No linked controller in hand.");
			}
		}
	}

	public static void tick() {
		// F5 работает только если контроллер в руке и активен
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (MODE != Mode.ACTIVE || !isHeldController())
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
		if (!heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
			MODE = Mode.IDLE;
			onReset();
			LOGGER.info("[Client] tick: No linked controller in hand, switched to IDLE and reset");
			return;
		}

		if (mc.screen != null || InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
			MODE = Mode.IDLE;
			onReset();
			LOGGER.info("[Client] tick: Some screen opened or ESC pressed, switched to IDLE and reset");
			return;
		}

		if (f5Pressed && packetCooldown == 0) {
			sendTestPacket(true);
			packetCooldown = PACKET_RATE;
		}
	}

	public static void sendTestPacket(boolean powered) {
		LOGGER.info("[Client] F5 {}! Sending powered:{} for all channels in linked controller...", powered ? "pressed" : "released", powered);
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player != null) {
			ItemStack heldItem = player.getMainHandItem();
			if (!heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
				heldItem = player.getOffhandItem();
			}
			if (heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
				ItemStackHandler inv = LinkedControllerItem.getFrequencyInventory(heldItem);
				int slotCount = 12; // 6 пар частот
				List<Couple<ItemStack>> frequencyCouples = new ArrayList<>();
				for (int logicalSlot = 0; logicalSlot < slotCount / 2; logicalSlot++) {
					int aIdx = logicalSlot * 2;
					int bIdx = aIdx + 1;
					ItemStack a = inv.getStackInSlot(aIdx);
					ItemStack b = inv.getStackInSlot(bIdx);
					if (!a.isEmpty() || !b.isEmpty()) {
						frequencyCouples.add(Couple.of(a, b));
						LOGGER.info("[Client] [F5] LogicalSlot {}: A={}, B={}", logicalSlot, a, b);
					}
				}
				if (!frequencyCouples.isEmpty()) {
					ModPackets.getChannel().sendToServer(new LinkedControllerInputPacket(
							makeButtonIndices(frequencyCouples), powered
					));
					LOGGER.info("[Client] [F5] Sent powered:{} for {} channels", powered, frequencyCouples.size());
				} else {
					LOGGER.info("[Client] [F5] No frequencies set in controller, nothing sent.");
				}
			} else {
				LOGGER.info("[Client] [F5] No linked controller in hand.");
			}
		} else {
			LOGGER.info("[Client] [F5] No player instance.");
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