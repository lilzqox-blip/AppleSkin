package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.ModConfig;

import java.io.IOException;

/**
 * Double Anchor: pegang respawn anchor, lihat anchor yang sudah ter-charge, tahan klik kanan ->
 * kirim satu paket interaksi tambahan ke anchor itu (satu kali per anchor).
 */
public class DoubleAnchorHandler
{
	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
		"key.appleskin.double_anchor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, AutoCrystalHandler.CATEGORY));

	private static BlockPos pos;
	private static int count;

	public static void init()
	{
		ClientTickEvents.END_CLIENT_TICK.register(DoubleAnchorHandler::onTick);
	}

	private static void onTick(MinecraftClient mc)
	{
		if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null)
			return;

		while (TOGGLE_KEY.wasPressed())
		{
			ModConfig.INSTANCE.doubleAnchorEnabled = !ModConfig.INSTANCE.doubleAnchorEnabled;
			try
			{
				ModConfig.INSTANCE.save();
			}
			catch (IOException ignored)
			{
			}
			mc.player.sendMessage(Text.literal("Double Anchor: " + (ModConfig.INSTANCE.doubleAnchorEnabled ? "ON" : "OFF")), true);
		}

		if (!ModConfig.INSTANCE.doubleAnchorEnabled)
		{
			pos = null;
			count = 0;
			return;
		}

		if (mc.currentScreen != null || !mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR))
			return;

		if (!(mc.crosshairTarget instanceof BlockHitResult hit) || !isAnchorCharged(mc, hit.getBlockPos()))
			return;

		if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS)
			return;

		if (hit.getBlockPos().equals(pos))
		{
			if (count >= 1)
				return;
		}
		else
		{
			pos = hit.getBlockPos();
			count = 0;
		}

		mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
		count++;
	}

	private static boolean isAnchorCharged(MinecraftClient mc, BlockPos blockPos)
	{
		BlockState state = mc.world.getBlockState(blockPos);
		return state.isOf(Blocks.RESPAWN_ANCHOR) && state.get(RespawnAnchorBlock.CHARGES) > 0;
	}
}
