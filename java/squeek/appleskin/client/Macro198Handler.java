package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.ModConfig;
import squeek.appleskin.mixin.MinecraftClientAccessor;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Macro 198: tahan tombol macro (default klik tengah). Lihat sisi atas sebuah block ->
 * pasang obsidian (kalau belum), pasang end crystal di atasnya, lalu hancurkan crystal di sekitar.
 */
public class Macro198Handler
{
	private enum State { IDLE, PLACE_OBI, WAIT_OBI, PLACE_CRYSTAL, BREAK_CRYSTAL }

	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
		"key.appleskin.macro198", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, AutoCrystalHandler.CATEGORY));

	private static int placeIn;
	private static int breakIn;
	private static State state = State.IDLE;
	private static BlockPos targetPos;

	public static void init()
	{
		ClientTickEvents.END_CLIENT_TICK.register(Macro198Handler::onTick);
	}

	private static void onTick(MinecraftClient mc)
	{
		if (mc.player == null || mc.world == null || mc.interactionManager == null)
			return;

		while (TOGGLE_KEY.wasPressed())
		{
			ModConfig.INSTANCE.macro198Enabled = !ModConfig.INSTANCE.macro198Enabled;
			try
			{
				ModConfig.INSTANCE.save();
			}
			catch (IOException ignored)
			{
			}
			if (ModConfig.INSTANCE.showToggleNotifications)
				mc.player.sendMessage(Text.literal("Macro 198: " + (ModConfig.INSTANCE.macro198Enabled ? "ON" : "OFF")), true);
		}

		if (!ModConfig.INSTANCE.macro198Enabled || mc.currentScreen != null)
		{
			resetState();
			return;
		}

		if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), ModConfig.INSTANCE.macro198Button) != GLFW.GLFW_PRESS)
		{
			resetState();
			return;
		}

		if (placeIn > 0)
			placeIn--;
		if (breakIn > 0)
			breakIn--;

		attackNearbyCrystals(mc);

		State previous = state;
		for (int i = 0; i < 4 && state == previous; i++)
		{
			switch (state)
			{
				case IDLE -> scanForPlacement(mc);
				case PLACE_OBI -> placeObsidian(mc);
				case WAIT_OBI -> waitForObsidian(mc);
				case PLACE_CRYSTAL -> placeCrystal(mc);
				case BREAK_CRYSTAL -> finishCycle(mc);
			}
			previous = state;
		}
	}

	private static void resetState()
	{
		placeIn = 0;
		breakIn = 0;
		state = State.IDLE;
		targetPos = null;
	}

	private static void scanForPlacement(MinecraftClient mc)
	{
		HitResult hit = mc.player.raycast(4.5D, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK)
			return;

		BlockPos pos = blockHit.getBlockPos();
		if (mc.world.getBlockState(pos).isAir())
			return;
		if (mc.player.getEyePos().distanceTo(blockHit.getPos()) > 4.5D)
			return;
		if (blockHit.getSide() != Direction.UP)
			return;

		if (isSupport(mc.world.getBlockState(pos).getBlock()))
		{
			targetPos = pos;
			state = State.PLACE_CRYSTAL;
		}
		else
		{
			targetPos = pos.up();
			state = State.PLACE_OBI;
		}
	}

	private static void placeObsidian(MinecraftClient mc)
	{
		if (placeIn > 0)
			return;
		if (ThreadLocalRandom.current().nextInt(1, 101) > ModConfig.INSTANCE.macro198PlaceChance)
			return;

		HitResult hit = mc.player.raycast(4.5D, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK)
		{
			state = State.IDLE;
			return;
		}
		if (mc.player.getEyePos().distanceTo(blockHit.getPos()) > 4.5D)
		{
			state = State.IDLE;
			return;
		}

		if (!selectHotbarItem(mc, Items.OBSIDIAN))
		{
			state = State.IDLE;
			return;
		}

		placeBlock(mc, blockHit);
		placeIn = ModConfig.INSTANCE.macro198PlaceDelay;
		state = State.WAIT_OBI;
	}

	private static void waitForObsidian(MinecraftClient mc)
	{
		if (isSupport(mc.world.getBlockState(targetPos).getBlock()))
			state = State.PLACE_CRYSTAL;
		else if (isSupport(mc.world.getBlockState(targetPos.down()).getBlock()))
		{
			targetPos = targetPos.down();
			state = State.PLACE_CRYSTAL;
		}
		else
			state = State.IDLE;
	}

	private static void placeCrystal(MinecraftClient mc)
	{
		if (placeIn > 0)
			return;

		if (!isSupport(mc.world.getBlockState(targetPos).getBlock()))
		{
			state = State.IDLE;
			return;
		}

		if (!AutoCrystalHandler.canPlaceCrystal(mc, targetPos))
		{
			state = State.BREAK_CRYSTAL;
			return;
		}

		if (ThreadLocalRandom.current().nextInt(1, 101) > ModConfig.INSTANCE.macro198PlaceChance)
			return;

		if (!selectHotbarItem(mc, Items.END_CRYSTAL))
		{
			state = State.IDLE;
			return;
		}

		BlockHitResult hit = new BlockHitResult(
			new Vec3d(targetPos.getX() + 0.5D, targetPos.getY() + 1.0D, targetPos.getZ() + 0.5D),
			Direction.UP, targetPos, false);

		placeBlock(mc, hit);
		placeIn = ModConfig.INSTANCE.macro198PlaceDelay;
		state = State.BREAK_CRYSTAL;
	}

	private static void finishCycle(MinecraftClient mc)
	{
		if (ModConfig.INSTANCE.macro198AutoBreak)
			state = State.IDLE;
		else
			state = isSupport(mc.world.getBlockState(targetPos).getBlock()) ? State.PLACE_CRYSTAL : State.IDLE;

		if (ModConfig.INSTANCE.macro198AirSwing && breakIn == 0
			&& mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.MISS
			&& ThreadLocalRandom.current().nextInt(1, 101) <= ModConfig.INSTANCE.macro198AirSwingChance)
		{
			((MinecraftClientAccessor) mc).appleskin$setAttackCooldown(10);
			mc.player.swingHand(Hand.MAIN_HAND);
			breakIn = ModConfig.INSTANCE.macro198BreakDelay;
		}
	}

	private static void attackNearbyCrystals(MinecraftClient mc)
	{
		if (breakIn > 0)
			return;
		if (ThreadLocalRandom.current().nextInt(1, 101) > ModConfig.INSTANCE.macro198AttackChance)
			return;

		if (mc.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof EndCrystalEntity crystal)
		{
			mc.interactionManager.attackEntity(mc.player, crystal);
			mc.player.swingHand(Hand.MAIN_HAND);
			breakIn = ModConfig.INSTANCE.macro198BreakDelay;
			return;
		}

		for (Entity entity : mc.world.getEntities())
		{
			if (entity instanceof EndCrystalEntity && entity.distanceTo(mc.player) < 9.0D)
			{
				mc.interactionManager.attackEntity(mc.player, entity);
				mc.player.swingHand(Hand.MAIN_HAND);
				breakIn = ModConfig.INSTANCE.macro198BreakDelay;
				return;
			}
		}
	}

	// ---------------------------------------------------------------- helpers

	private static void placeBlock(MinecraftClient mc, BlockHitResult hit)
	{
		AutoCrystalHandler.bypassUseCancel = true;
		try
		{
			mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
		}
		finally
		{
			AutoCrystalHandler.bypassUseCancel = false;
		}
		mc.player.swingHand(Hand.MAIN_HAND);
	}

	private static boolean selectHotbarItem(MinecraftClient mc, Item item)
	{
		if (mc.player.getMainHandStack().isOf(item))
			return true;
		for (int i = 0; i < 9; i++)
		{
			if (mc.player.getInventory().getStack(i).isOf(item))
			{
				mc.player.getInventory().setSelectedSlot(i);
				return true;
			}
		}
		return false;
	}

	private static boolean isSupport(Block block)
	{
		return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
	}
}
