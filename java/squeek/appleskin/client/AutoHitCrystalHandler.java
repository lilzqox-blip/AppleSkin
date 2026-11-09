package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.ModConfig;

import java.io.IOException;

/**
 * Auto Hit Crystal: otomatis memukul end crystal terdekat dalam jangkauan.
 * Opsi rotate: kamera menoleh ke crystal dulu, lalu memukul di tick berikutnya.
 */
public class AutoHitCrystalHandler
{
	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
		"key.appleskin.auto_hit_crystal", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, AutoCrystalHandler.CATEGORY));

	private static int lastCrystalId = -1;
	private static long tickCounter;
	private static long lastTargetTick = Long.MIN_VALUE;
	private static long lastHitMs;

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
			mc.player.sendMessage(Text.literal("Auto Hit Crystal: " + (ModConfig.INSTANCE.autoHitCrystalEnabled ? "ON" : "OFF")), true);
		}

		tickCounter++;

		if (!ModConfig.INSTANCE.autoHitCrystalEnabled)
		{
			lastCrystalId = -1;
			return;
		}

		if (mc.currentScreen != null || mc.player.isUsingItem())
			return;

		if (System.currentTimeMillis() - lastHitMs < ModConfig.INSTANCE.autoHitCrystalDelayMs)
			return;

		Vec3d eye = mc.player.getEyePos();
		double range = ModConfig.INSTANCE.autoHitCrystalRange10 / 10.0;
		double limit = range * range;

		EndCrystalEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (Entity entity : mc.world.getEntities())
		{
			if (entity instanceof EndCrystalEntity crystal && crystal.isAlive())
			{
				double d = eye.squaredDistanceTo(new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ()));
				if (d <= limit && d < bestDist)
				{
					best = crystal;
					bestDist = d;
				}
			}
		}

		if (best == null)
		{
			lastCrystalId = -1;
			return;
		}

		if (ModConfig.INSTANCE.autoHitCrystalRotate)
		{
			rotateToward(mc, best.getBoundingBox().getCenter(), 60.0F);
			// pukul hanya kalau crystal yang sama sudah jadi target di tick sebelumnya (server sudah menerima rotasi)
			boolean ready = lastCrystalId == best.getId() && lastTargetTick == tickCounter - 1;
			lastCrystalId = best.getId();
			lastTargetTick = tickCounter;
			if (!ready)
				return;
		}

		mc.interactionManager.attackEntity(mc.player, best);
		mc.player.swingHand(Hand.MAIN_HAND);
		lastHitMs = System.currentTimeMillis();
	}

	private static void rotateToward(MinecraftClient mc, Vec3d target, float maxStep)
	{
		Vec3d eye = mc.player.getEyePos();
		double dx = target.x - eye.x;
		double dy = target.y - eye.y;
		double dz = target.z - eye.z;
		float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		float yaw = mc.player.getYaw();
		float pitch = mc.player.getPitch();
		float dYaw = MathHelper.clamp(MathHelper.wrapDegrees(wantYaw - yaw), -maxStep, maxStep);
		float dPitch = MathHelper.clamp(wantPitch - pitch, -maxStep, maxStep);
		mc.player.setYaw(yaw + dYaw);
		mc.player.setPitch(MathHelper.clamp(pitch + dPitch, -90.0F, 90.0F));
	}
}
