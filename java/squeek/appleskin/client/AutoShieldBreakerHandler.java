package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import squeek.appleskin.ModConfig;

/**
 * Auto Shield Breaker (versi sederhana): saat kamu memukul pemain yang sedang memblok dengan shield,
 * otomatis pindah ke kapak di hotbar supaya shield-nya mati. Opsi Swap Back mengembalikan slot
 * ke item sebelumnya setelah pukulan.
 */
public class AutoShieldBreakerHandler
{
	private static int swapBackSlot = -1;

	public static void init()
	{
		// kembalikan slot di akhir tick, setelah paket serangan terkirim
		ClientTickEvents.END_CLIENT_TICK.register(mc ->
		{
			if (swapBackSlot >= 0)
			{
				if (mc.player != null)
					HotbarUtil.select(mc, swapBackSlot);
				swapBackSlot = -1;
			}
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
		{
			if (!world.isClient() || !ModConfig.INSTANCE.shieldBreakerEnabled || hand != Hand.MAIN_HAND)
				return ActionResult.PASS;

			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.player == null || player != mc.player)
				return ActionResult.PASS;

			if (!(entity instanceof PlayerEntity target) || !target.isBlocking())
				return ActionResult.PASS;

			ItemStack mainHand = player.getMainHandStack();
			if (mainHand.isIn(ItemTags.AXES))
				return ActionResult.PASS;

			if (ModConfig.INSTANCE.shieldBreakerRequireWeapon
				&& !(mainHand.isIn(ItemTags.SWORDS) || mainHand.isOf(Items.MACE)))
				return ActionResult.PASS;

			if (ModConfig.INSTANCE.shieldBreakerRequireAttackKey && !mc.options.attackKey.isPressed())
				return ActionResult.PASS;

			int axeSlot = findAxeSlot(mc);
			if (axeSlot == -1)
				return ActionResult.PASS;

			int previous = player.getInventory().getSelectedSlot();
			HotbarUtil.select(mc, axeSlot);
			if (ModConfig.INSTANCE.shieldBreakerSwapBack && swapBackSlot < 0)
				swapBackSlot = previous;

			return ActionResult.PASS;
		});
	}

	private static int findAxeSlot(MinecraftClient mc)
	{
		for (int i = 0; i < 9; i++)
		{
			if (mc.player.getInventory().getStack(i).isIn(ItemTags.AXES))
				return i;
		}
		return -1;
	}
}
