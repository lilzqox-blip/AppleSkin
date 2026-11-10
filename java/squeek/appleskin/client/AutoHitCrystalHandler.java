package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.ModConfig;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto Hit Crystal: pegang pedang, tahan tombol aktif (default klik kanan) ke sebuah block ->
 * pasang obsidian di sana, ganti ke end crystal, lalu jalankan logika Auto Crystal
 * (pasang + hancurkan crystal), seperti hit-crystalling.
 *
 * Mode ContinuousObsidian: setiap tick dilihat dulu crosshair-nya mengarah ke apa --
 * block biasa -> pasang obsidian; obsidian/bedrock/crystal yang sudah ada -> jalankan
 * siklus crystal. Jadi selama tombol ditahan dan kamu menggeser pandangan ke block baru,
 * obsidian+crystal terus terpasang berantai, bukan cuma sekali lalu berhenti di tahap crystal.
 */
public class AutoHitCrystalHandler
{
	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
		"key.appleskin.auto_hit_crystal", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, AutoCrystalHandler.CATEGORY));

	private static int placeClock;
	private static int switchClock;
	private static boolean active;
	private static boolean crystalling;
	private static boolean crystalSelected;

	public static void init()
	{
		ClientTickEvents.END_CLIENT_TICK.register(AutoHitCrystalHandler::onTick);
	}

	private static void onTick(MinecraftClient mc)
	{
		if (mc.player == null || mc.world == null || mc.interactionManager == null)
			return;

		while (TOGGLE_KEY.wasPressed())
		{
			ModConfig.INSTANCE.autoHitCrystalEnabled = !ModConfig.INSTANCE.autoHitCrystalEnabled;
			try
			{
				ModConfig.INSTANCE.save();
			}
			catch (IOException ignored)
			{
			}
			if (ModConfig.INSTANCE.showToggleNotifications)
				mc.player.sendMessage(Text.literal("Auto Hit Crystal: " + (ModConfig.INSTANCE.autoHitCrystalEnabled ? "ON" : "OFF")), true);
		}

		if (!ModConfig.INSTANCE.autoHitCrystalEnabled)
		{
			reset();
			return;
		}

		if (mc.currentScreen != null)
			return;

		boolean held = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), ModConfig.INSTANCE.autoHitCrystalButton) == GLFW.GLFW_PRESS;
		if (!held)
		{
			reset();
			return;
		}

		ItemStack mainHand = mc.player.getMainHandStack();
		boolean validItem = mainHand.isIn(ItemTags.SWORDS)
			|| (ModConfig.INSTANCE.autoHitCrystalWorkWithTotem && mainHand.isOf(Items.TOTEM_OF_UNDYING))
			|| (ModConfig.INSTANCE.autoHitCrystalWorkWithCrystal && mainHand.isOf(Items.END_CRYSTAL));

		if (!validItem && !active)
			return;

		active = true;

		if (ModConfig.INSTANCE.autoHitCrystalContinuousObsidian)
			tickContinuous(mc);
		else
			tickOnce(mc);
	}

	/** Mode lama: pasang satu obsidian, lalu pindah permanen ke tahap crystal. */
	private static void tickOnce(MinecraftClient mc)
	{
		int randomNum = rand();

		if (mc.crosshairTarget instanceof BlockHitResult checkHit && checkHit.getType() == HitResult.Type.BLOCK
			&& !crystalling && ModConfig.INSTANCE.autoHitCrystalCheckPlace && !canPlaceObsidian(mc, checkHit))
			return;

		if (mc.crosshairTarget instanceof BlockHitResult hit0 && !crystalSelected && placeClock == 0 && switchClock == 0)
		{
			if (ModConfig.INSTANCE.autoHitCrystalSwordSwap && hit0.getType() == HitResult.Type.BLOCK)
			{
				Block block = mc.world.getBlockState(hit0.getBlockPos()).getBlock();
				if (!crystalling)
					crystalling = block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
			}
		}

		// Tahap 1: pasang obsidian di block yang dilihat
		if (!crystalling)
		{
			if (mc.crosshairTarget instanceof BlockHitResult hit)
			{
				if (hit.getType() == HitResult.Type.MISS)
					return;

				if (!mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.OBSIDIAN))
				{
					if (isChargedAnchor(mc, hit.getBlockPos()))
						return;

					mc.options.useKey.setPressed(false);

					if (!mc.player.isHolding(Items.OBSIDIAN))
					{
						if (switchClock > 0)
						{
							switchClock--;
							return;
						}

						if (randomNum <= ModConfig.INSTANCE.autoHitCrystalSwitchChance)
						{
							switchClock = ModConfig.INSTANCE.autoHitCrystalSwitchDelay;
							selectHotbarItem(mc, Items.OBSIDIAN);
						}
					}

					if (mc.player.isHolding(Items.OBSIDIAN))
					{
						if (placeClock > 0)
						{
							placeClock--;
							return;
						}

						randomNum = rand();

						if (randomNum <= ModConfig.INSTANCE.autoHitCrystalPlaceChance)
						{
							placeBlock(mc, hit);
							placeClock = ModConfig.INSTANCE.autoHitCrystalPlaceDelay;
							crystalling = true;
						}
					}
				}
			}
		}

		// Tahap 2: ganti ke end crystal, lalu jalankan Auto Crystal
		if (crystalling)
			runCrystalStage(mc, randomNum);
	}

	/**
	 * Mode ContinuousObsidian: tiap tick dicek sendiri crosshair-nya mengarah ke apa,
	 * tidak ada status permanen "sudah pindah ke crystal" -- jadi bisa bolak-balik
	 * obsidian -> crystal -> obsidian baru selama tombol ditahan.
	 */
	private static void tickContinuous(MinecraftClient mc)
	{
		HitResult target = mc.crosshairTarget;

		boolean targetIsCrystalWork =
			(target instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK
				&& isObsidianOrBedrock(mc, bh.getBlockPos()))
			|| (target instanceof EntityHitResult eh
				&& (eh.getEntity() instanceof EndCrystalEntity || eh.getEntity() instanceof SlimeEntity));

		if (targetIsCrystalWork)
		{
			runCrystalStage(mc, rand());
			return;
		}

		if (!(target instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
			return;

		if (ModConfig.INSTANCE.autoHitCrystalCheckPlace && !canPlaceObsidian(mc, hit))
			return;
		if (isChargedAnchor(mc, hit.getBlockPos()))
			return;

		mc.options.useKey.setPressed(false);

		if (!mc.player.isHolding(Items.OBSIDIAN))
		{
			if (switchClock > 0)
			{
				switchClock--;
				return;
			}
			if (rand() <= ModConfig.INSTANCE.autoHitCrystalSwitchChance)
			{
				switchClock = ModConfig.INSTANCE.autoHitCrystalSwitchDelay;
				crystalSelected = false;
				selectHotbarItem(mc, Items.OBSIDIAN);
			}
			return;
		}

		if (placeClock > 0)
		{
			placeClock--;
			return;
		}

		if (rand() <= ModConfig.INSTANCE.autoHitCrystalPlaceChance)
		{
			placeBlock(mc, hit);
			placeClock = ModConfig.INSTANCE.autoHitCrystalPlaceDelay;
		}
	}

	/** Ganti ke end crystal (kalau perlu), lalu jalankan Auto Crystal (pasang + hancurkan). */
	private static void runCrystalStage(MinecraftClient mc, int randomNum)
	{
		if (!mc.player.isHolding(Items.END_CRYSTAL) && !crystalSelected)
		{
			if (switchClock > 0)
			{
				switchClock--;
				return;
			}

			if (randomNum <= ModConfig.INSTANCE.autoHitCrystalSwitchChance)
			{
				crystalSelected = selectHotbarItem(mc, Items.END_CRYSTAL);
				switchClock = ModConfig.INSTANCE.autoHitCrystalSwitchDelay;
			}
			return;
		}

		if (mc.player.isHolding(Items.END_CRYSTAL))
		{
			// kalau modul AutoCrystal menyala, dia sudah jalan sendiri
			if (!ModConfig.INSTANCE.autoCrystalEnabled)
				AutoCrystalHandler.crystalTick(mc);
		}
		else
		{
			crystalSelected = false;
		}
	}

	private static void reset()
	{
		placeClock = ModConfig.INSTANCE.autoHitCrystalPlaceDelay;
		switchClock = ModConfig.INSTANCE.autoHitCrystalSwitchDelay;
		active = false;
		crystalling = false;
		crystalSelected = false;
	}

	// ---------------------------------------------------------------- helpers

	private static int rand()
	{
		return ThreadLocalRandom.current().nextInt(1, 101);
	}

	/** Obsidian bisa dipasang menempel di block yang dilihat (sisi yang dikenai crosshair). */
	private static boolean canPlaceObsidian(MinecraftClient mc, BlockHitResult hit)
	{
		BlockPos target = hit.getBlockPos().offset(hit.getSide());
		return mc.world.getBlockState(target).isReplaceable()
			&& mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), target, ShapeContext.absent());
	}

	private static boolean isObsidianOrBedrock(MinecraftClient mc, BlockPos pos)
	{
		Block block = mc.world.getBlockState(pos).getBlock();
		return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
	}

	private static boolean isChargedAnchor(MinecraftClient mc, BlockPos pos)
	{
		return mc.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)
			&& mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) > 0;
	}

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
				HotbarUtil.select(mc, i);
				return true;
			}
		}
		return false;
	}
}
