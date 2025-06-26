package com.zoritism.wirelesslinks.content.redstone.link.controller;

import java.util.*;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zoritism.wirelesslinks.registry.ModItems;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
		// TODO: сброс визуальных кнопок, если требуется
		LOGGER.info("[Client] onReset: State cleared (packetCooldown=0, selectedLocation=ZERO, lecternPos=null, currentlyPressed cleared)");
	}

	/**
	 * Обработка нажатия F5: отправить powered:true по всем реально заданным частотам контроллера
	 */
	@SubscribeEvent
	public static void onKeyInput(InputEvent.Key event) {
		if (event.getKey() == GLFW.GLFW_KEY_F5 && event.getAction() == GLFW.GLFW_PRESS) {
			sendTestPacket();
		}
	}

	public static void sendTestPacket() {
		LOGGER.info("[Client] F5 pressed! Sending powered:true for all channels in linked controller...");
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player != null) {
			ItemStack heldItem = player.getMainHandItem();
			if (!heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
				heldItem = player.getOffhandItem();
			}
			if (heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
				int slotCount = 12; // 6 пар частот
				List<Couple<ItemStack>> frequencyCouples = new ArrayList<>();
				for (int logicalSlot = 0; logicalSlot < slotCount / 2; logicalSlot++) {
					FrequencyPair pair = LinkedControllerItem.slotToFrequency(heldItem, logicalSlot);
					ItemStack a = pair.getFirst().getStack();
					ItemStack b = pair.getSecond().getStack();
					if (!a.isEmpty() || !b.isEmpty()) {
						frequencyCouples.add(Couple.of(a, b));
						LOGGER.info("[Client] [F5] LogicalSlot {}: A={}, B={}", logicalSlot, a, b);
					}
				}
				if (!frequencyCouples.isEmpty()) {
					LinkedControllerServerHandler.receivePressed(
							player.level(), player.blockPosition(), player.getUUID(),
							frequencyCouples, true // powered:true
					);
					LOGGER.info("[Client] [F5] Sent powered:true for {} channels", frequencyCouples.size());
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
			// TODO: отправка пакета releasedKeys (false)
			// TODO: отправка пакета newKeys (true)
			// TODO: keepalive packet для всех нажатых pressedKeys (true)
			if (!releasedKeys.isEmpty()) {
				LOGGER.info("[Client] tick: releasedKeys={}", releasedKeys);
				// send released packet
			}
			if (!newKeys.isEmpty()) {
				LOGGER.info("[Client] tick: newKeys={}", newKeys);
				// send pressed packet
				packetCooldown = PACKET_RATE;
			}
			if (packetCooldown == 0 && !pressedKeys.isEmpty()) {
				LOGGER.info("[Client] tick: keepalive for pressedKeys={}", pressedKeys);
				// send keepalive packet
				packetCooldown = PACKET_RATE;
			}
		}

		if (MODE == Mode.BIND) {
			// TODO: визуализация выделения блока (shape), если требуется
			for (Integer integer : newKeys) {
				LOGGER.info("[Client] tick: Bind mode, key pressed: {} (binding to block {})", integer, selectedLocation);
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