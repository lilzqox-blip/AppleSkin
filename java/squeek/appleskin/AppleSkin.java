package squeek.appleskin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import squeek.appleskin.api.AppleSkinApi;
import squeek.appleskin.client.DebugInfoHudEntry;
import squeek.appleskin.client.AnchorMacroHandler;
import squeek.appleskin.client.AutoCrystalHandler;
import squeek.appleskin.client.DoubleAnchorHandler;
import squeek.appleskin.client.Macro198Handler;
import squeek.appleskin.client.FastXpHandler;
import squeek.appleskin.client.HUDOverlayHandler;
import squeek.appleskin.client.HoverTotemHandler;
import squeek.appleskin.client.TooltipOverlayHandler;
import squeek.appleskin.network.ClientSyncHandler;

public class AppleSkin implements ClientModInitializer
{
	private static KeyBinding clickGuiKey;
	public static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitializeClient()
	{
		ClientSyncHandler.init();
		clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.appleskin.click_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.create(Identifier.of("appleskin", "main"))));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (clickGuiKey.wasPressed()) {
				if (client.currentScreen == null) client.setScreen(new ModuleGuiScreen());
			}
		});
		ModConfig.init();
		HUDOverlayHandler.init();
		TooltipOverlayHandler.init();
		HoverTotemHandler.init();
		FastXpHandler.init();
		AnchorMacroHandler.init();
		AutoCrystalHandler.init();
		DoubleAnchorHandler.init();
		Macro198Handler.init();
		FabricLoader.getInstance().getEntrypointContainers("appleskin", AppleSkinApi.class).forEach(entrypoint -> {
			try
			{
				entrypoint.getEntrypoint().registerEvents();
			}
			catch (Throwable e)
			{
				LOGGER.error("Failed to load entrypoint for mod {}", entrypoint.getProvider().getMetadata().getId(), e);
			}
		});
		DebugHudEntries.register(DebugInfoHudEntry.ENTRY_ID, new DebugInfoHudEntry());
	}
}
