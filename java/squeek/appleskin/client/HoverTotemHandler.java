package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.ModConfig;
import squeek.appleskin.mixin.HandledScreenAccessor;

import java.io.IOException;

/**
 * Hover totem: di inventory, kalau kursor ada di atas totem,
 * totem dipindah ke offhand dan/atau ke slot hotbar yang dipilih.
 */
public class HoverTotemHandler
{
	private static final int OFFHAND_SLOT = 40;

	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
		"key.appleskin.hover_totem",
		InputUtil.Type.KEYSYM,
		GLFW.GLFW_KEY_J,
		KeyBinding.Category.create(Identifier.of("appleskin", "main"))
	));

	// Satuan: tick (1 tick = 0,05 detik)
	private static int timer = 0;

	public static void init()
	{
		ClientTickEvents.END_CLIENT_TICK.register(HoverTotemHandler::onTick);
	}

	private static void onTick(MinecraftClient mc)
	{
		if (mc.player == null || mc.interactionManager == null)
			return;

		// Toggle on/off dengan keybind
		while (TOGGLE_KEY.wasPressed())
		{
			ModConfig.INSTANCE.hoverTotemEnabled = !ModConfig.INSTANCE.hoverTotemEnabled;
			try
			{
				ModConfig.INSTANCE.save();
			}
			catch (IOException ignored)
			{
			}
			mc.player.sendMessage(Text.literal("HoverTotem: " + (ModConfig.INSTANCE.hoverTotemEnabled ? "ON" : "OFF")), true);
		}

		if (!ModConfig.INSTANCE.hoverTotemEnabled)
			return;

		if (timer > 0)
		{
			timer--;
			return;
		}

		if (!(mc.currentScreen instanceof HandledScreen<?> screen))
			return;

		Slot slot = ((HandledScreenAccessor) screen).appleskin$getFocusedSlot();
		if (slot == null || !slot.getStack().isOf(Items.TOTEM_OF_UNDYING))
			return;

		// Jangan sentuh totem yang sudah ada di offhand
		if (slot.inventory == mc.player.getInventory() && slot.getIndex() == OFFHAND_SLOT)
			return;

		int syncId = screen.getScreenHandler().syncId;

		// 1) Offhand dulu kalau belum ada totem
		if (ModConfig.INSTANCE.hoverTotemOffhand && !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))
		{
			mc.interactionManager.clickSlot(syncId, slot.id, OFFHAND_SLOT, SlotActionType.SWAP, mc.player);
			timer = ModConfig.INSTANCE.hoverTotemDelayTicks;
			return;
		}

		// 2) Lalu hotbar kalau slot pilihan belum ada totem
		if (ModConfig.INSTANCE.hoverTotemHotbar)
		{
			int target = ModConfig.INSTANCE.hoverTotemHotbarSlot - 1;
			if (!mc.player.getInventory().getStack(target).isOf(Items.TOTEM_OF_UNDYING))
			{
				mc.interactionManager.clickSlot(syncId, slot.id, target, SlotActionType.SWAP, mc.player);
				timer = ModConfig.INSTANCE.hoverTotemDelayTicks;
			}
		}
	}
}
