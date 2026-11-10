package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.ModConfig;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Double Anchor (port dari DoubleAnchor.java + DoubleAnchorState.java).
 *
 * Dua bagian terpisah:
 * 1) Pasif: pegang respawn anchor, klik kanan anchor yang sudah ter-charge ->
 *    kirim satu paket interaksi tambahan (double place), lalu opsional tutup dengan glowstone.
 * 2) Macro (MacroBind): satu tombol menjalankan urutan penuh di block yang kamu lihat ->
 *    pasang anchor, charge, ledakkan + pasang anchor lagi di tempat yang sama (double),
 *    charge lagi, ledakkan lagi (opsional tunggu damage tick), lalu kembalikan slot.
 *
 * Tidak ada notifikasi di action bar sama sekali (diminta dihapus).
 */
public class DoubleAnchorHandler
{
	private enum MacroState { IDLE, PLACE_1, GLOW_1, EXPLODE_DOUBLE_1, GLOW_2, EXPLODE_2, RESTORE }

	// --------------------------------------------------------------- pasif

	private static BlockPos lastAnchorPos;
	private static int doubleAnchorCount;
	private static boolean glowstonePending;
	private static int glowstoneDelayLeft;

	// --------------------------------------------------------------- macro

	private static MacroState macroState = MacroState.IDLE;
	private static BlockHitResult startHit;
	private static BlockPos macroAnchorPos;
	private static int startSlot = -1;
	private static boolean startedThisPress;
	private static int stageDelayLeft;
	private static int damageTickWaitTicks;
	private static boolean macroBindWasDown;

	public static void init()
	{
		ClientTickEvents.END_CLIENT_TICK.register(DoubleAnchorHandler::onTick);
	}

	private static void onTick(MinecraftClient mc)
	{
		if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null)
			return;

		if (!ModConfig.INSTANCE.doubleAnchorEnabled)
		{
			lastAnchorPos = null;
			doubleAnchorCount = 0;
			glowstonePending = false;
			resetMacro();
			return;
		}

		tickPassive(mc);
		tickMacro(mc);
	}

	// --------------------------------------------------------- bagian pasif

	private static void tickPassive(MinecraftClient mc)
	{
		// pasang glowstone penutup setelah double place (ditunda sedikit, sama seperti aslinya)
		if (glowstonePending)
		{
			if (glowstoneDelayLeft > 0)
			{
				glowstoneDelayLeft--;
				return;
			}
			glowstonePending = false;

			if (!(mc.crosshairTarget instanceof BlockHitResult hit))
				return;
			if (!mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR))
				return;
			if (mc.world.getBlockState(hit.getBlockPos()).get(RespawnAnchorBlock.CHARGES) != 0)
				return;

			int glow = findHotbarSlot(mc, Items.GLOWSTONE);
			if (glow != -1)
			{
				HotbarUtil.select(mc, glow);
				interact(mc, hit);
			}
			return;
		}

		if (macroState != MacroState.IDLE || mc.currentScreen != null)
			return;
		if (!mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR))
			return;
		if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS)
			return;
		if (!(mc.crosshairTarget instanceof BlockHitResult hit))
			return;
		if (!isChargedAnchor(mc, hit.getBlockPos()))
			return;

		if (hit.getBlockPos().equals(lastAnchorPos))
		{
			if (doubleAnchorCount >= 1)
				return;
		}
		else
		{
			lastAnchorPos = hit.getBlockPos();
			doubleAnchorCount = 0;
		}

		if (ThreadLocalRandom.current().nextInt(1, 101) > ModConfig.INSTANCE.doubleAnchorChance)
			return;

		mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
		doubleAnchorCount++;

		if (ModConfig.INSTANCE.doubleAnchorGlowstone)
		{
			glowstonePending = true;
			glowstoneDelayLeft = ModConfig.INSTANCE.doubleAnchorGlowstoneDelay;
		}
	}

	// ---------------------------------------------------------- bagian macro

	private static void tickMacro(MinecraftClient mc)
	{
		int key = ModConfig.INSTANCE.doubleAnchorMacroBind;
		boolean held = key > 0 && mc.currentScreen == null
			&& GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;

		if (mc.currentScreen != null || mc.player.isUsingItem() || mc.player.isSneaking())
		{
			startedThisPress = false;
			return;
		}

		if (macroState != MacroState.IDLE)
		{
			if (stageDelayLeft > 0)
			{
				stageDelayLeft--;
				return;
			}
			runMacroStage(mc);
			return;
		}

		if (!held)
		{
			startedThisPress = false;
			macroBindWasDown = false;
			return;
		}

		if (startedThisPress && !ModConfig.INSTANCE.doubleAnchorMacroHoldRepeat)
			return;
		if (startedThisPress && !ModConfig.INSTANCE.doubleAnchorMacroHoldRepeat == false && macroBindWasDown)
		{
			// masih ditahan dan HoldRepeat ON -> boleh mulai lagi setelah siklus sebelumnya selesai
		}
		macroBindWasDown = true;

		if (stageDelayLeft > 0)
		{
			stageDelayLeft--;
			return;
		}

		if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
			return;
		if (mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR))
			return;

		BlockPos placePos = mc.world.getBlockState(hit.getBlockPos()).isReplaceable()
			? hit.getBlockPos()
			: hit.getBlockPos().offset(hit.getSide());
		if (mc.player.getBoundingBox().intersects(new Box(placePos)))
			return;

		startedThisPress = true;
		startHit = hit;
		macroAnchorPos = placePos;
		startSlot = mc.player.getInventory().getSelectedSlot();
		macroState = MacroState.PLACE_1;
		stageDelayLeft = ModConfig.INSTANCE.doubleAnchorMacroDelay;
	}

	private static void runMacroStage(MinecraftClient mc)
	{
		switch (macroState)
		{
			case PLACE_1:
			{
				if (mc.player.getBoundingBox().intersects(new Box(macroAnchorPos)))
				{
					abortMacro();
					return;
				}
				if (!placeAnchor(mc, startHit))
				{
					abortMacro();
					return;
				}
				macroState = MacroState.GLOW_1;
				break;
			}
			case GLOW_1:
			{
				if (!isAnchor(mc, macroAnchorPos))
				{
					abortMacro();
					return;
				}
				BlockHitResult topHit = topFaceHit(macroAnchorPos);
				if (!chargeAnchor(mc, topHit))
				{
					abortMacro();
					return;
				}
				macroState = MacroState.EXPLODE_DOUBLE_1;
				break;
			}
			case EXPLODE_DOUBLE_1:
			{
				if (!isChargedAnchor(mc, macroAnchorPos))
				{
					abortMacro();
					return;
				}
				BlockHitResult topHit = topFaceHit(macroAnchorPos);
				if (!explodeAndDoublePlace(mc, topHit))
				{
					abortMacro();
					return;
				}
				macroState = MacroState.GLOW_2;
				break;
			}
			case GLOW_2:
			{
				if (!isAnchor(mc, macroAnchorPos))
				{
					abortMacro();
					return;
				}
				BlockHitResult topHit = topFaceHit(macroAnchorPos);
				if (!chargeAnchor(mc, topHit))
				{
					abortMacro();
					return;
				}
				macroState = MacroState.EXPLODE_2;
				damageTickWaitTicks = 0;
				break;
			}
			case EXPLODE_2:
			{
				if (!isChargedAnchor(mc, macroAnchorPos))
				{
					abortMacro();
					return;
				}
				if (shouldWaitForDamageTick(mc))
				{
					// tunggu tick ini, coba lagi tick berikutnya
					stageDelayLeft = 0;
					return;
				}
				BlockHitResult topHit = topFaceHit(macroAnchorPos);
				if (!explodeAnchor(mc, topHit))
				{
					abortMacro();
					return;
				}
				macroState = MacroState.RESTORE;
				break;
			}
			case RESTORE:
			{
				if (ModConfig.INSTANCE.doubleAnchorMacroSwapBack && ModConfig.INSTANCE.doubleAnchorMacroSwap && startSlot != -1)
					HotbarUtil.select(mc, startSlot);
				resetMacro();
				return;
			}
			default:
				resetMacro();
				return;
		}

		stageDelayLeft = ModConfig.INSTANCE.doubleAnchorMacroDelay;
	}

	private static void abortMacro()
	{
		if (ModConfig.INSTANCE.doubleAnchorMacroSwap && startSlot != -1)
		{
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.player != null)
				HotbarUtil.select(mc, startSlot);
		}
		resetMacro();
	}

	private static void resetMacro()
	{
		macroState = MacroState.IDLE;
		startHit = null;
		macroAnchorPos = null;
		startSlot = -1;
		stageDelayLeft = 0;
		damageTickWaitTicks = 0;
	}

	private static boolean shouldWaitForDamageTick(MinecraftClient mc)
	{
		if (!ModConfig.INSTANCE.doubleAnchorMacroDamageTick)
			return false;

		double range = ModConfig.INSTANCE.doubleAnchorMacroDamageTickRange10 / 10.0;
		PlayerEntity closest = null;
		double bestDist = Double.MAX_VALUE;
		for (PlayerEntity p : mc.world.getPlayers())
		{
			if (p == mc.player)
				continue;
			double d = p.distanceTo(mc.player);
			if (d < bestDist)
			{
				bestDist = d;
				closest = p;
			}
		}

		if (closest == null || bestDist >= range || closest.hurtTime == 0)
			return false;

		if (damageTickWaitTicks >= msToTicks(ModConfig.INSTANCE.doubleAnchorMacroDamageTickMaxWait))
			return false;

		damageTickWaitTicks++;
		return true;
	}

	private static int msToTicks(int ms)
	{
		return Math.max(1, ms / 50);
	}

	// ------------------------------------------------------------- aksi anchor

	private static boolean placeAnchor(MinecraftClient mc, BlockHitResult hit)
	{
		if (!selectOrRequire(mc, Items.RESPAWN_ANCHOR))
			return false;
		interact(mc, hit);
		return true;
	}

	private static boolean chargeAnchor(MinecraftClient mc, BlockHitResult hit)
	{
		if (!selectOrRequire(mc, Items.GLOWSTONE))
			return false;
		interact(mc, hit);
		return true;
	}

	private static boolean explodeAnchor(MinecraftClient mc, BlockHitResult hit)
	{
		if (ModConfig.INSTANCE.doubleAnchorMacroSwap)
		{
			int slot = ModConfig.INSTANCE.doubleAnchorMacroExplodeSlot - 1;
			HotbarUtil.select(mc, slot);
		}
		else if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE))
		{
			return false;
		}
		interact(mc, hit);
		return true;
	}

	private static boolean explodeAndDoublePlace(MinecraftClient mc, BlockHitResult hit)
	{
		if (!explodeAnchor(mc, hit))
			return false;

		if (!ModConfig.INSTANCE.doubleAnchorMacroSwap)
		{
			if (mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR))
				interact(mc, hit);
		}
		else if (selectOrRequire(mc, Items.RESPAWN_ANCHOR))
		{
			interact(mc, hit);
		}
		return true;
	}

	private static boolean selectOrRequire(MinecraftClient mc, Item item)
	{
		if (!ModConfig.INSTANCE.doubleAnchorMacroSwap)
			return mc.player.getMainHandStack().isOf(item);
		int slot = findHotbarSlot(mc, item);
		if (slot == -1)
			return false;
		HotbarUtil.select(mc, slot);
		return true;
	}

	private static void interact(MinecraftClient mc, BlockHitResult hit)
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

	// ------------------------------------------------------------------ util

	private static boolean isChargedAnchor(MinecraftClient mc, BlockPos pos)
	{
		BlockState state = mc.world.getBlockState(pos);
		return state.isOf(Blocks.RESPAWN_ANCHOR) && state.get(RespawnAnchorBlock.CHARGES) > 0;
	}

	private static boolean isAnchor(MinecraftClient mc, BlockPos pos)
	{
		return mc.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR);
	}

	private static BlockHitResult topFaceHit(BlockPos pos)
	{
		return new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false);
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
}
