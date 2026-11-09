package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.ModConfig;

import java.io.IOException;

/**
 * Membuka GUI modul: tombol Right Shift, atau perintah chat /askin.
 * Juga menyimpan config setelah ada perubahan dari GUI.
 */
public class ModuleGuiHandler
{
	private static final KeyBinding OPEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
		"key.appleskin.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, AutoCrystalHandler.CATEGORY));

	private static boolean pendingOpen;
	private static boolean dirty;
	private static int dirtyTicks;
	private static final boolean[] bindWasDown = new boolean[ModuleScreen.MODULES.size()];

	public static void init()
	{
		ClientTickEvents.END_CLIENT_TICK.register(ModuleGuiHandler::onTick);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommandManager.literal("askin").executes(context ->
			{
				// dibuka di tick berikutnya, setelah layar chat tertutup
				pendingOpen = true;
				return 1;
			})));
	}

	public static void markDirty()
	{
		dirty = true;
	}

	private static void onTick(MinecraftClient mc)
	{
		if (mc.player == null)
			return;

		while (OPEN_KEY.wasPressed())
		{
			if (mc.currentScreen == null)
				pendingOpen = true;
		}

		// bind yang diatur di GUI: tekan tombolnya untuk toggle modul
		for (int i = 0; i < ModuleScreen.MODULES.size(); i++)
		{
			ModuleScreen.Module module = ModuleScreen.MODULES.get(i);
			int key = module.bind.getInt();
			boolean pressed = key > 0 && mc.currentScreen == null
				&& GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
			if (pressed && !bindWasDown[i])
			{
				boolean now = !module.enabled.getBool();
				module.enabled.setBool(now);
				markDirty();
				mc.player.sendMessage(Text.literal(module.name + ": " + (now ? "ON" : "OFF")), true);
			}
			bindWasDown[i] = pressed;
		}

		if (pendingOpen && mc.currentScreen == null)
		{
			pendingOpen = false;
			mc.setScreen(new ModuleScreen());
		}

		if (dirty)
		{
			// simpan setelah GUI ditutup, atau tiap 1 detik selama GUI terbuka
			if (!(mc.currentScreen instanceof ModuleScreen) || ++dirtyTicks >= 20)
			{
				try
				{
					ModConfig.INSTANCE.save();
				}
				catch (IOException ignored)
				{
				}
				dirty = false;
				dirtyTicks = 0;
			}
		}
	}
}
