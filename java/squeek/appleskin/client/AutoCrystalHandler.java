package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.ModConfig;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto Crystal: tahan tombol aktif (default klik kanan) sambil memegang end crystal ->
 * otomatis pasang crystal di obsidian/bedrock yang dilihat dan hancurkan crystal yang dilihat.
 */
public class AutoCrystalHandler
{
	/** Kategori keybind bersama untuk AutoCrystal, DoubleAnchor dan Macro198. */
	public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("appleskin", "combat_macros"));

	/** Diset true oleh modul kita sendiri saat memasang block, supaya tidak dibatalkan callback di bawah. */
	public static boolean bypassUseCancel = false;

	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
		"key.appleskin.auto_crystal", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

	private static int placeClock;
	private static int breakClock;

	public static void init()
	{
		ClientTickEvents.END_CLIENT_TICK.register(AutoCrystalHandler::onTick);

		// Batalkan klik kanan bawaan ke obsidian/bedrock saat pegang crystal (modul yang memasangnya)
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
		{
			if (!world.isClient() || bypassUseCancel || !ModConfig.INSTANCE.autoCrystalEnabled)
				return ActionResult.PASS;
			if (hand == Hand.MAIN_HAND && player.getMainHandStack().isOf(Items.END_CRYSTAL))
			{
				BlockState state = world.getBlockState(hitResult.getBlockPos());
				if (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK))
					return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});
	}

	private static void onTick(MinecraftClient mc)
	{
		if (mc.player == null || mc.world == null || mc.interactionManager == null)
			return;

		while (TOGGLE_KEY.wasPressed())
		{
			ModConfig.INSTANCE.autoCrystalEnabled = !ModConfig.INSTANCE.autoCrystalEnabled;
			try
			{
				ModConfig.INSTANCE.save();
			}
			catch (IOException ignored)
			{
			}
			if (ModConfig.INSTANCE.showToggleNotifications)
				mc.player.sendMessage(Text.literal("Auto Crystal: " + (ModConfig.INSTANCE.autoCrystalEnabled ? "ON" : "OFF")), true);
		}

		if (!ModConfig.INSTANCE.autoCrystalEnabled)
			return;

		crystalTick(mc);
	}

	/** Satu tick logika Auto Crystal (tanpa cek enabled). Juga dipanggil oleh Auto Hit Crystal. */
	public static void crystalTick(MinecraftClient mc)
	{
		if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null)
			return;

		boolean dontPlace = placeClock != 0;
		boolean dontBreak = breakClock != 0;

		if (ModConfig.INSTANCE.autoCrystalStopOnKill && isDeadBodyNearby(mc))
			return;

		int roll = ThreadLocalRandom.current().nextInt(1, 101);

		if (dontPlace)
			placeClock--;
		if (dontBreak)
			breakClock--;

		if (mc.player.isUsingItem())
			return;

		if (ModConfig.INSTANCE.autoCrystalDamageTick && damageTickCheck(mc))
			return;

		if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), ModConfig.INSTANCE.autoCrystalButton) != GLFW.GLFW_PRESS)
		{
			placeClock = 0;
			breakClock = 0;
			return;
		}

		if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL))
			return;

		// Pasang crystal di obsidian/bedrock yang sedang dilihat
		if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK)
		{
			if (!dontPlace && roll <= ModConfig.INSTANCE.autoCrystalPlaceChance)
			{
				BlockPos pos = hit.getBlockPos();
				BlockState state = mc.world.getBlockState(pos);
				if ((state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK)) && canPlaceCrystal(mc, pos))
				{
					placeBlock(mc, hit);
					placeClock = ModConfig.INSTANCE.autoCrystalPlaceDelay;
				}
			}
		}

		// Hancurkan crystal (atau slime) yang sedang dilihat
		int breakRoll = ThreadLocalRandom.current().nextInt(1, 101);
		if (mc.crosshairTarget instanceof EntityHitResult entityHit)
		{
			if (!dontBreak && breakRoll <= ModConfig.INSTANCE.autoCrystalBreakChance)
			{
				Entity entity = entityHit.getEntity();
				if (!(entity instanceof EndCrystalEntity || entity instanceof SlimeEntity))
					return;

				int previousSlot = mc.player.getInventory().getSelectedSlot();
				boolean swapped = false;

				if (ModConfig.INSTANCE.autoCrystalAntiWeakness && cantBreakCrystal(mc))
				{
					int sword = findHotbarTool(mc);
					if (sword != -1)
					{
						HotbarUtil.select(mc, sword);
						swapped = true;
					}
				}

				mc.interactionManager.attackEntity(mc.player, entity);
				mc.player.swingHand(Hand.MAIN_HAND);
				breakClock = ModConfig.INSTANCE.autoCrystalBreakDelay;

				if (swapped)
					HotbarUtil.select(mc, previousSlot);
			}
		}
	}

	// ---------------------------------------------------------------- helpers

	private static void placeBlock(MinecraftClient mc, BlockHitResult hit)
	{
		bypassUseCancel = true;
		try
		{
			mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
		}
		finally
		{
			bypassUseCancel = false;
		}
		mc.player.swingHand(Hand.MAIN_HAND);
	}

	/** Crystal bisa dipasang di atas block ini: dua block di atasnya kosong dan tidak ada entity di sana. */
	public static boolean canPlaceCrystal(MinecraftClient mc, BlockPos base)
	{
		BlockPos up = base.up();
		if (!mc.world.getBlockState(up).isAir() || !mc.world.getBlockState(up.up()).isAir())
			return false;
		Box box = new Box(up.getX(), up.getY(), up.getZ(), up.getX() + 1.0, up.getY() + 2.0, up.getZ() + 1.0);
		return mc.world.getOtherEntities(null, box).isEmpty();
	}

	private static boolean isDeadBodyNearby(MinecraftClient mc)
	{
		for (PlayerEntity p : mc.world.getPlayers())
		{
			if (p != mc.player && p.isDead() && p.squaredDistanceTo(mc.player) < 64.0)
				return true;
		}
		return false;
	}

	private static boolean damageTickCheck(MinecraftClient mc)
	{
		boolean someoneHurt = mc.world.getPlayers().stream()
			.filter(e -> e != mc.player)
			.filter(e -> e.squaredDistanceTo(mc.player) < 36)
			.filter(e -> e.getLastAttacker() == null)
			.filter(e -> !e.isOnGround())
			.anyMatch(e -> e.hurtTime >= 2);
		return someoneHurt && !(mc.player.getAttacking() instanceof PlayerEntity);
	}

	private static boolean isTool(ItemStack stack)
	{
		return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES)
			|| stack.isIn(ItemTags.PICKAXES) || stack.isIn(ItemTags.SHOVELS);
	}

	private static boolean cantBreakCrystal(MinecraftClient mc)
	{
		StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
		StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
		return !(weakness == null
			|| (strength != null && strength.getAmplifier() > weakness.getAmplifier())
			|| isTool(mc.player.getMainHandStack()));
	}

	private static int findHotbarTool(MinecraftClient mc)
	{
		for (int i = 0; i < 9; i++)
		{
			if (mc.player.getInventory().getStack(i).isIn(ItemTags.SWORDS))
				return i;
		}
		for (int i = 0; i < 9; i++)
		{
			if (isTool(mc.player.getInventory().getStack(i)))
				return i;
		}
		return -1;
	}
}
