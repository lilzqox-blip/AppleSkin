package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import squeek.appleskin.ModConfig;
import squeek.appleskin.mixin.MinecraftClientAccessor;

/**
 * Fast XP: lempar xp bottle dengan cepat. Hanya aktif kalau memegang
 * xp bottle (main hand atau offhand), tidak ada layar terbuka, dan right click ditahan.
 */
public class FastXpHandler
{
	// Satuan: tick (1 tick = 0,05 detik)
	private static int timer = 0;

	public static void init()
	{
		ClientTickEvents.END_CLIENT_TICK.register(FastXpHandler::onTick);
	}

	private static void onTick(MinecraftClient mc)
	{
		if (mc.player == null || mc.interactionManager == null)
			return;

		if (!ModConfig.INSTANCE.fastXpEnabled || mc.currentScreen != null || !mc.options.useKey.isPressed())
		{
			timer = 0;
			return;
		}

		Hand hand;
		if (mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE))
			hand = Hand.MAIN_HAND;
		else if (mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE))
			hand = Hand.OFF_HAND;
		else
		{
			timer = 0;
			return;
		}

		// Matikan pengulangan bawaan Minecraft (4 tick) supaya kecepatan murni dari setting
		((MinecraftClientAccessor) mc).appleskin$setItemUseCooldown(10);

		if (timer > 0)
		{
			timer--;
			return;
		}

		mc.interactionManager.interactItem(mc.player, hand);
		mc.player.swingHand(hand);
		timer = ModConfig.INSTANCE.fastXpDelayTicks;
	}
}
