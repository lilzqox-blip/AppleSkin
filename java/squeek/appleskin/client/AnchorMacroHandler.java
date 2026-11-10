package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.ModConfig;

import java.io.IOException;

/**
 * Anchor Macro: tahan right click ke respawn anchor -> otomatis charge (glowstone),
 * lalu ganti ke slot totem dan meledakkan anchor. Opsional: swap balik ke anchor
 * dan lempar ender pearl setelah ledakan.
 */
public class AnchorMacroHandler
{
	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
		"key.appleskin.anchor_macro",
		InputUtil.Type.KEYSYM,
		GLFW.GLFW_KEY_K,
		KeyBinding.Category.create(Identifier.of("appleskin", "anchor_macro"))
	));

	private static final int MAX_REFUSALS = 2;

	private static BlockPos targetAnchorPos;
	private static int lastChargeLevel = -1;
	private static int unchangedTicks;
	private static int switchDelayCounter;
	private static int glowstoneDelayCounter;
	private static int explodeDelayCounter;
	private static int lastInteractAge = Integer.MIN_VALUE;
	private static boolean swapBackPending;
	private static int pearlStage;
	private static int savedSlotBeforePearl = -1;
	private static long lastPearlThrowMs;
	private static BlockPos refusalPos;
	private static int refusalCount;
	private static boolean wasActive;

	public static void init()
	{
		ClientTickEvents.END_CLIENT_TICK.register(AnchorMacroHandler::onTick);
	}

	private static void onTick(MinecraftClient mc)
	{
		if (mc.player == null || mc.world == null || mc.interactionManager == null)
		{
			clearTrackedAnchor();
			resetPearlStage();
			swapBackPending = false;
			return;
		}

		while (TOGGLE_KEY.wasPressed())
		{
			ModConfig.INSTANCE.anchorMacroEnabled = !ModConfig.INSTANCE.anchorMacroEnabled;
			try
			{
				ModConfig.INSTANCE.save();
			}
			catch (IOException ignored)
			{
			}
			if (ModConfig.INSTANCE.showToggleNotifications)
				mc.player.sendMessage(Text.literal("Anchor Macro: " + (ModConfig.INSTANCE.anchorMacroEnabled ? "ON" : "OFF")), true);
		}

		if (!ModConfig.INSTANCE.anchorMacroEnabled)
		{
			if (wasActive)
			{
				resetAll();
				wasActive = false;
			}
			return;
		}
		wasActive = true;

		tickPearlSequence(mc);
		swapBackToAnchorSlot(mc);

		boolean rightHeld = isRightMouseHeld(mc);
		if (!rightHeld)
			resetRefusals();

		if (mc.currentScreen == null && !isUsingFoodOrShield(mc, rightHeld) && rightHeld)
			tickAnchorInteraction(mc);
		else
			clearTrackedAnchor();
	}

	// ---------------------------------------------------------------- anchor

	private static void tickAnchorInteraction(MinecraftClient mc)
	{
		BlockHitResult hit = getLookedAtAnchor(mc);
		if (hit == null)
			return;

		BlockPos pos = hit.getBlockPos();
		BlockState state = mc.world.getBlockState(pos);
		mc.options.useKey.setPressed(false);

		int charges = state.get(RespawnAnchorBlock.CHARGES);

		if (targetAnchorPos != null)
		{
			boolean unchanged = lastChargeLevel > 0 && charges == lastChargeLevel;
			if (targetAnchorPos.equals(pos) && unchanged && unchangedTicks < 10)
			{
				unchangedTicks++;
				return;
			}
			checkChargeRefusal(mc, pos, charges);
		}

		if (isRefusalLimitReached(pos))
			return;

		if (charges == 0)
		{
			if (tryChargeAnchor(mc, hit))
				trackAnchorState(pos, charges);
		}
		else if (tryDetonateAnchor(mc, hit, state))
		{
			trackAnchorState(pos, charges);
			swapBackPending = true;
			startPearlSequence(mc);
		}
	}

	private static boolean tryChargeAnchor(MinecraftClient mc, BlockHitResult hit)
	{
		if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE))
		{
			if (switchDelayCounter < ModConfig.INSTANCE.anchorSwitchDelay)
			{
				switchDelayCounter++;
				return false;
			}
			switchDelayCounter = 0;

			int slot = findHotbarSlot(mc, Items.GLOWSTONE);
			if (slot == -1)
				return false;
			HotbarUtil.select(mc, slot);
		}

		if (glowstoneDelayCounter < ModConfig.INSTANCE.anchorGlowstoneDelay)
		{
			glowstoneDelayCounter++;
			return false;
		}

		if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE) && canInteractNow(mc) && interactAnchor(mc, hit))
		{
			glowstoneDelayCounter = 0;
			return true;
		}
		return false;
	}

	private static boolean tryDetonateAnchor(MinecraftClient mc, BlockHitResult hit, BlockState state)
	{
		int slot = ModConfig.INSTANCE.anchorTotemSlot - 1;
		if (mc.player.getInventory().getSelectedSlot() != slot)
		{
			if (switchDelayCounter < ModConfig.INSTANCE.anchorSwitchDelay)
			{
				switchDelayCounter++;
				return false;
			}
			switchDelayCounter = 0;
			HotbarUtil.select(mc, slot);
		}

		if (explodeDelayCounter < ModConfig.INSTANCE.anchorExplodeDelay)
		{
			explodeDelayCounter++;
			return false;
		}

		boolean holdingGlowstone = mc.player.getMainHandStack().isOf(Items.GLOWSTONE) || mc.player.getOffHandStack().isOf(Items.GLOWSTONE);
		if (holdingGlowstone && state.get(RespawnAnchorBlock.CHARGES) < 4)
			return false;

		if (canInteractNow(mc) && interactAnchor(mc, hit))
		{
			explodeDelayCounter = 0;
			return true;
		}
		return false;
	}

	private static boolean interactAnchor(MinecraftClient mc, BlockHitResult hit)
	{
		mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
		lastInteractAge = mc.player.age;
		return true;
	}

	private static boolean canInteractNow(MinecraftClient mc)
	{
		if (mc.player.age == lastInteractAge)
			return false;
		return !mc.player.shouldCancelInteraction()
			|| (mc.player.getMainHandStack().isEmpty() && mc.player.getOffHandStack().isEmpty());
	}

	private static BlockHitResult getLookedAtAnchor(MinecraftClient mc)
	{
		if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK)
			return mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR) ? hit : null;
		return null;
	}

	private static void trackAnchorState(BlockPos pos, int charges)
	{
		targetAnchorPos = pos;
		lastChargeLevel = charges;
		unchangedTicks = 0;
	}

	private static void clearTrackedAnchor()
	{
		targetAnchorPos = null;
		lastChargeLevel = -1;
		unchangedTicks = 0;
	}

	// -------------------------------------------------------------- refusals

	private static void checkChargeRefusal(MinecraftClient mc, BlockPos pos, int charges)
	{
		if (targetAnchorPos.equals(pos) && lastChargeLevel == 0)
		{
			if (charges > 0)
			{
				if (pos.equals(refusalPos))
					resetRefusals();
			}
			else
			{
				if (!pos.equals(refusalPos))
				{
					refusalPos = pos;
					refusalCount = 0;
				}
				refusalCount++;
				if (refusalCount == MAX_REFUSALS)
				{
					mc.player.sendMessage(Text.literal("Anchor Macro: server menolak charge " + MAX_REFUSALS
						+ "x, berhenti klik anchor ini (lepas right click untuk coba lagi)"), true);
				}
			}
		}
		clearTrackedAnchor();
	}

	private static boolean isRefusalLimitReached(BlockPos pos)
	{
		return pos.equals(refusalPos) && refusalCount >= MAX_REFUSALS;
	}

	private static void resetRefusals()
	{
		refusalPos = null;
		refusalCount = 0;
	}

	// ----------------------------------------------------------- swap + pearl

	private static void swapBackToAnchorSlot(MinecraftClient mc)
	{
		if (!swapBackPending || pearlStage != 0 || mc.currentScreen != null)
			return;

		swapBackPending = false;
		if (ModConfig.INSTANCE.anchorSwapBack)
		{
			int slot = findHotbarSlot(mc, Items.RESPAWN_ANCHOR);
			if (slot != -1)
				HotbarUtil.select(mc, slot);
		}
	}

	private static void startPearlSequence(MinecraftClient mc)
	{
		if (!ModConfig.INSTANCE.anchorPearl || pearlStage != 0)
			return;
		if (System.currentTimeMillis() - lastPearlThrowMs < ModConfig.INSTANCE.anchorPearlCooldown * 1000L)
			return;
		if (findHotbarSlot(mc, Items.ENDER_PEARL) == -1)
			return;

		savedSlotBeforePearl = mc.player.getInventory().getSelectedSlot();
		pearlStage = 1;
	}

	private static void tickPearlSequence(MinecraftClient mc)
	{
		if (pearlStage == 0 || mc.currentScreen != null)
			return;

		int slot = findHotbarSlot(mc, Items.ENDER_PEARL);
		if (slot == -1)
		{
			resetPearlStage();
			return;
		}

		mc.options.useKey.setPressed(false);
		switch (pearlStage)
		{
			case 1:
				HotbarUtil.select(mc, slot);
				pearlStage = 2;
				break;
			case 2:
				float pitch = mc.player.getPitch();
				if (ModConfig.INSTANCE.anchorPearlAimDown)
					mc.player.setPitch(ModConfig.INSTANCE.anchorPearlPitch);
				mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
				mc.player.setPitch(pitch);
				lastPearlThrowMs = System.currentTimeMillis();
				pearlStage = 3;
				break;
			default:
				if (savedSlotBeforePearl >= 0 && savedSlotBeforePearl <= 8)
					HotbarUtil.select(mc, savedSlotBeforePearl);
				resetPearlStage();
		}
	}

	private static void resetPearlStage()
	{
		pearlStage = 0;
		savedSlotBeforePearl = -1;
	}

	// ----------------------------------------------------------------- utils

	private static boolean isRightMouseHeld(MinecraftClient mc)
	{
		return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
	}

	private static boolean isUsingFoodOrShield(MinecraftClient mc, boolean rightHeld)
	{
		boolean food = mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD)
			|| mc.player.getOffHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD);
		boolean shield = mc.player.getMainHandStack().getItem() instanceof ShieldItem
			|| mc.player.getOffHandStack().getItem() instanceof ShieldItem;
		return (food || shield) && rightHeld;
	}

	private static int findHotbarSlot(MinecraftClient mc, Item item)
	{
		for (int i = 0; i < 9; i++)
		{
			if (mc.player.getInventory().getStack(i).isOf(item))
				return i;
		}
		return -1;
	}

	private static void resetAll()
	{
		switchDelayCounter = 0;
		glowstoneDelayCounter = 0;
		explodeDelayCounter = 0;
		swapBackPending = false;
		lastInteractAge = Integer.MIN_VALUE;
		resetRefusals();
		clearTrackedAnchor();
		resetPearlStage();
	}
}
