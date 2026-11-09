package squeek.appleskin.gui;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.DummyConfigSerializer;
import net.minecraft.util.ActionResult;
import squeek.appleskin.ModConfig;

import java.io.IOException;

@Config(name = "appleskin")
public class AutoConfigIntegration implements ConfigData
{
	public static void init()
	{
		var holder = AutoConfig.register(AutoConfigIntegration.class, DummyConfigSerializer::new);
		holder.registerSaveListener((manager, data) -> {
			ModConfig.INSTANCE.maxHudOverlayFlashAlpha = data.maxHudOverlayFlashAlpha;
			ModConfig.INSTANCE.showFoodHealthHudOverlay = data.showFoodHealthHudOverlay;
			ModConfig.INSTANCE.showFoodExhaustionHudUnderlay = data.showFoodExhaustionHudUnderlay;
			ModConfig.INSTANCE.showFoodValuesInTooltip = data.showFoodValuesInTooltip;
			ModConfig.INSTANCE.showFoodValuesHudOverlay = data.showFoodValuesHudOverlay;
			ModConfig.INSTANCE.showFoodValuesInTooltipAlways = data.showFoodValuesInTooltipAlways;
			ModConfig.INSTANCE.showSaturationHudOverlay = data.showSaturationHudOverlay;
			ModConfig.INSTANCE.showVanillaAnimationsOverlay = data.showVanillaAnimationsOverlay;
			ModConfig.INSTANCE.showFoodValuesHudOverlayWhenOffhand = data.showFoodValuesHudOverlayWhenOffhand;
			ModConfig.INSTANCE.hoverTotemEnabled = data.hoverTotemEnabled;
			ModConfig.INSTANCE.hoverTotemOffhand = data.hoverTotemOffhand;
			ModConfig.INSTANCE.hoverTotemHotbar = data.hoverTotemHotbar;
			ModConfig.INSTANCE.hoverTotemHotbarSlot = data.hoverTotemHotbarSlot;
			ModConfig.INSTANCE.hoverTotemDelayTicks = data.hoverTotemDelayTicks;
			ModConfig.INSTANCE.fastXpEnabled = data.fastXpEnabled;
			ModConfig.INSTANCE.fastXpDelayTicks = data.fastXpDelayTicks;
			ModConfig.INSTANCE.anchorMacroEnabled = data.anchorMacroEnabled;
			ModConfig.INSTANCE.anchorSwitchDelay = data.anchorSwitchDelay;
			ModConfig.INSTANCE.anchorGlowstoneDelay = data.anchorGlowstoneDelay;
			ModConfig.INSTANCE.anchorExplodeDelay = data.anchorExplodeDelay;
			ModConfig.INSTANCE.anchorTotemSlot = data.anchorTotemSlot;
			ModConfig.INSTANCE.anchorSwapBack = data.anchorSwapBack;
			ModConfig.INSTANCE.anchorPearl = data.anchorPearl;
			ModConfig.INSTANCE.anchorPearlAimDown = data.anchorPearlAimDown;
			ModConfig.INSTANCE.anchorPearlPitch = data.anchorPearlPitch;
			ModConfig.INSTANCE.anchorPearlCooldown = data.anchorPearlCooldown;
			ModConfig.INSTANCE.autoCrystalEnabled = data.autoCrystalEnabled;
			ModConfig.INSTANCE.autoCrystalButton = data.autoCrystalButton;
			ModConfig.INSTANCE.autoCrystalPlaceDelay = data.autoCrystalPlaceDelay;
			ModConfig.INSTANCE.autoCrystalBreakDelay = data.autoCrystalBreakDelay;
			ModConfig.INSTANCE.autoCrystalPlaceChance = data.autoCrystalPlaceChance;
			ModConfig.INSTANCE.autoCrystalBreakChance = data.autoCrystalBreakChance;
			ModConfig.INSTANCE.autoCrystalStopOnKill = data.autoCrystalStopOnKill;
			ModConfig.INSTANCE.autoCrystalDamageTick = data.autoCrystalDamageTick;
			ModConfig.INSTANCE.autoCrystalAntiWeakness = data.autoCrystalAntiWeakness;
			ModConfig.INSTANCE.doubleAnchorEnabled = data.doubleAnchorEnabled;
			ModConfig.INSTANCE.macro198Enabled = data.macro198Enabled;
			ModConfig.INSTANCE.macro198Button = data.macro198Button;
			ModConfig.INSTANCE.macro198AutoBreak = data.macro198AutoBreak;
			ModConfig.INSTANCE.macro198PlaceDelay = data.macro198PlaceDelay;
			ModConfig.INSTANCE.macro198BreakDelay = data.macro198BreakDelay;
			ModConfig.INSTANCE.macro198PlaceChance = data.macro198PlaceChance;
			ModConfig.INSTANCE.macro198AttackChance = data.macro198AttackChance;
			ModConfig.INSTANCE.macro198AirSwing = data.macro198AirSwing;
			ModConfig.INSTANCE.macro198AirSwingChance = data.macro198AirSwingChance;
			ModConfig.INSTANCE.autoHitCrystalEnabled = data.autoHitCrystalEnabled;
			ModConfig.INSTANCE.autoHitCrystalRange10 = data.autoHitCrystalRange10;
			ModConfig.INSTANCE.autoHitCrystalRotate = data.autoHitCrystalRotate;
			ModConfig.INSTANCE.autoHitCrystalDelayMs = data.autoHitCrystalDelayMs;
			try
			{
				ModConfig.INSTANCE.save();
				return ActionResult.SUCCESS;
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		});
		holder.registerLoadListener((manager, data) -> {
			data.maxHudOverlayFlashAlpha = ModConfig.INSTANCE.maxHudOverlayFlashAlpha;
			data.showFoodHealthHudOverlay = ModConfig.INSTANCE.showFoodHealthHudOverlay;
			data.showFoodExhaustionHudUnderlay = ModConfig.INSTANCE.showFoodExhaustionHudUnderlay;
			data.showFoodValuesInTooltip = ModConfig.INSTANCE.showFoodValuesInTooltip;
			data.showFoodValuesHudOverlay = ModConfig.INSTANCE.showFoodValuesHudOverlay;
			data.showFoodValuesInTooltipAlways = ModConfig.INSTANCE.showFoodValuesInTooltipAlways;
			data.showSaturationHudOverlay = ModConfig.INSTANCE.showSaturationHudOverlay;
			data.showVanillaAnimationsOverlay = ModConfig.INSTANCE.showVanillaAnimationsOverlay;
			data.showFoodValuesHudOverlayWhenOffhand = ModConfig.INSTANCE.showFoodValuesHudOverlayWhenOffhand;
			data.hoverTotemEnabled = ModConfig.INSTANCE.hoverTotemEnabled;
			data.hoverTotemOffhand = ModConfig.INSTANCE.hoverTotemOffhand;
			data.hoverTotemHotbar = ModConfig.INSTANCE.hoverTotemHotbar;
			data.hoverTotemHotbarSlot = ModConfig.INSTANCE.hoverTotemHotbarSlot;
			data.hoverTotemDelayTicks = ModConfig.INSTANCE.hoverTotemDelayTicks;
			data.fastXpEnabled = ModConfig.INSTANCE.fastXpEnabled;
			data.fastXpDelayTicks = ModConfig.INSTANCE.fastXpDelayTicks;
			data.anchorMacroEnabled = ModConfig.INSTANCE.anchorMacroEnabled;
			data.anchorSwitchDelay = ModConfig.INSTANCE.anchorSwitchDelay;
			data.anchorGlowstoneDelay = ModConfig.INSTANCE.anchorGlowstoneDelay;
			data.anchorExplodeDelay = ModConfig.INSTANCE.anchorExplodeDelay;
			data.anchorTotemSlot = ModConfig.INSTANCE.anchorTotemSlot;
			data.anchorSwapBack = ModConfig.INSTANCE.anchorSwapBack;
			data.anchorPearl = ModConfig.INSTANCE.anchorPearl;
			data.anchorPearlAimDown = ModConfig.INSTANCE.anchorPearlAimDown;
			data.anchorPearlPitch = ModConfig.INSTANCE.anchorPearlPitch;
			data.anchorPearlCooldown = ModConfig.INSTANCE.anchorPearlCooldown;
			data.autoCrystalEnabled = ModConfig.INSTANCE.autoCrystalEnabled;
			data.autoCrystalButton = ModConfig.INSTANCE.autoCrystalButton;
			data.autoCrystalPlaceDelay = ModConfig.INSTANCE.autoCrystalPlaceDelay;
			data.autoCrystalBreakDelay = ModConfig.INSTANCE.autoCrystalBreakDelay;
			data.autoCrystalPlaceChance = ModConfig.INSTANCE.autoCrystalPlaceChance;
			data.autoCrystalBreakChance = ModConfig.INSTANCE.autoCrystalBreakChance;
			data.autoCrystalStopOnKill = ModConfig.INSTANCE.autoCrystalStopOnKill;
			data.autoCrystalDamageTick = ModConfig.INSTANCE.autoCrystalDamageTick;
			data.autoCrystalAntiWeakness = ModConfig.INSTANCE.autoCrystalAntiWeakness;
			data.doubleAnchorEnabled = ModConfig.INSTANCE.doubleAnchorEnabled;
			data.macro198Enabled = ModConfig.INSTANCE.macro198Enabled;
			data.macro198Button = ModConfig.INSTANCE.macro198Button;
			data.macro198AutoBreak = ModConfig.INSTANCE.macro198AutoBreak;
			data.macro198PlaceDelay = ModConfig.INSTANCE.macro198PlaceDelay;
			data.macro198BreakDelay = ModConfig.INSTANCE.macro198BreakDelay;
			data.macro198PlaceChance = ModConfig.INSTANCE.macro198PlaceChance;
			data.macro198AttackChance = ModConfig.INSTANCE.macro198AttackChance;
			data.macro198AirSwing = ModConfig.INSTANCE.macro198AirSwing;
			data.macro198AirSwingChance = ModConfig.INSTANCE.macro198AirSwingChance;
			data.autoHitCrystalEnabled = ModConfig.INSTANCE.autoHitCrystalEnabled;
			data.autoHitCrystalRange10 = ModConfig.INSTANCE.autoHitCrystalRange10;
			data.autoHitCrystalRotate = ModConfig.INSTANCE.autoHitCrystalRotate;
			data.autoHitCrystalDelayMs = ModConfig.INSTANCE.autoHitCrystalDelayMs;
			return ActionResult.SUCCESS;
		});
		holder.load();
	}

	@ConfigEntry.Gui.Excluded
	private static final ModConfig DEFAULTS = new ModConfig();

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesInTooltip = DEFAULTS.showFoodValuesInTooltip;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesInTooltipAlways = DEFAULTS.showFoodValuesInTooltipAlways;

	@ConfigEntry.Gui.Tooltip()
	public boolean showSaturationHudOverlay = DEFAULTS.showSaturationHudOverlay;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesHudOverlay = DEFAULTS.showFoodValuesHudOverlay;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesHudOverlayWhenOffhand = DEFAULTS.showFoodValuesHudOverlayWhenOffhand;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodExhaustionHudUnderlay = DEFAULTS.showFoodExhaustionHudUnderlay;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodHealthHudOverlay = DEFAULTS.showFoodHealthHudOverlay;

	@ConfigEntry.Gui.Tooltip()
	public boolean showVanillaAnimationsOverlay = DEFAULTS.showVanillaAnimationsOverlay;

	@ConfigEntry.Gui.Tooltip()
	public float maxHudOverlayFlashAlpha = DEFAULTS.maxHudOverlayFlashAlpha;

	@ConfigEntry.Gui.Tooltip()
	public boolean hoverTotemEnabled = DEFAULTS.hoverTotemEnabled;

	@ConfigEntry.Gui.Tooltip()
	public boolean hoverTotemOffhand = DEFAULTS.hoverTotemOffhand;

	@ConfigEntry.Gui.Tooltip()
	public boolean hoverTotemHotbar = DEFAULTS.hoverTotemHotbar;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 1, max = 9)
	public int hoverTotemHotbarSlot = DEFAULTS.hoverTotemHotbarSlot;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 10)
	public int hoverTotemDelayTicks = DEFAULTS.hoverTotemDelayTicks;

	@ConfigEntry.Gui.Tooltip()
	public boolean fastXpEnabled = DEFAULTS.fastXpEnabled;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 10)
	public int fastXpDelayTicks = DEFAULTS.fastXpDelayTicks;

	@ConfigEntry.Gui.Tooltip()
	public boolean anchorMacroEnabled = DEFAULTS.anchorMacroEnabled;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 20)
	public int anchorSwitchDelay = DEFAULTS.anchorSwitchDelay;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 20)
	public int anchorGlowstoneDelay = DEFAULTS.anchorGlowstoneDelay;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 20)
	public int anchorExplodeDelay = DEFAULTS.anchorExplodeDelay;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 1, max = 9)
	public int anchorTotemSlot = DEFAULTS.anchorTotemSlot;

	@ConfigEntry.Gui.Tooltip()
	public boolean anchorSwapBack = DEFAULTS.anchorSwapBack;

	@ConfigEntry.Gui.Tooltip()
	public boolean anchorPearl = DEFAULTS.anchorPearl;

	@ConfigEntry.Gui.Tooltip()
	public boolean anchorPearlAimDown = DEFAULTS.anchorPearlAimDown;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 90)
	public int anchorPearlPitch = DEFAULTS.anchorPearlPitch;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 60)
	public int anchorPearlCooldown = DEFAULTS.anchorPearlCooldown;

	@ConfigEntry.Gui.Tooltip()
	public boolean autoCrystalEnabled = DEFAULTS.autoCrystalEnabled;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 2)
	public int autoCrystalButton = DEFAULTS.autoCrystalButton;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 20)
	public int autoCrystalPlaceDelay = DEFAULTS.autoCrystalPlaceDelay;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 20)
	public int autoCrystalBreakDelay = DEFAULTS.autoCrystalBreakDelay;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
	public int autoCrystalPlaceChance = DEFAULTS.autoCrystalPlaceChance;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
	public int autoCrystalBreakChance = DEFAULTS.autoCrystalBreakChance;

	@ConfigEntry.Gui.Tooltip()
	public boolean autoCrystalStopOnKill = DEFAULTS.autoCrystalStopOnKill;

	@ConfigEntry.Gui.Tooltip()
	public boolean autoCrystalDamageTick = DEFAULTS.autoCrystalDamageTick;

	@ConfigEntry.Gui.Tooltip()
	public boolean autoCrystalAntiWeakness = DEFAULTS.autoCrystalAntiWeakness;

	@ConfigEntry.Gui.Tooltip()
	public boolean doubleAnchorEnabled = DEFAULTS.doubleAnchorEnabled;

	@ConfigEntry.Gui.Tooltip()
	public boolean macro198Enabled = DEFAULTS.macro198Enabled;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 2)
	public int macro198Button = DEFAULTS.macro198Button;

	@ConfigEntry.Gui.Tooltip()
	public boolean macro198AutoBreak = DEFAULTS.macro198AutoBreak;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 20)
	public int macro198PlaceDelay = DEFAULTS.macro198PlaceDelay;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 20)
	public int macro198BreakDelay = DEFAULTS.macro198BreakDelay;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
	public int macro198PlaceChance = DEFAULTS.macro198PlaceChance;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
	public int macro198AttackChance = DEFAULTS.macro198AttackChance;

	@ConfigEntry.Gui.Tooltip()
	public boolean macro198AirSwing = DEFAULTS.macro198AirSwing;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
	public int macro198AirSwingChance = DEFAULTS.macro198AirSwingChance;

	@ConfigEntry.Gui.Tooltip()
	public boolean autoHitCrystalEnabled = DEFAULTS.autoHitCrystalEnabled;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 20, max = 60)
	public int autoHitCrystalRange10 = DEFAULTS.autoHitCrystalRange10;

	@ConfigEntry.Gui.Tooltip()
	public boolean autoHitCrystalRotate = DEFAULTS.autoHitCrystalRotate;

	@ConfigEntry.Gui.Tooltip()
	@ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
	public int autoHitCrystalDelayMs = DEFAULTS.autoHitCrystalDelayMs;
}
