package squeek.appleskin;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.api.SyntaxError;
import net.fabricmc.loader.api.FabricLoader;
import squeek.appleskin.gui.AutoConfigIntegration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class ModConfig
{
	public static ModConfig INSTANCE;
	public static Jankson JANKSON;
	public static Path PATH;

	public static void init()
	{
		PATH = FabricLoader.getInstance().getConfigDir().resolve("appleskin.json5");
		JANKSON = Jankson.builder().build();
		INSTANCE = new ModConfig();
		try
		{
			INSTANCE.load();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		if (FabricLoader.getInstance().isModLoaded("cloth-config"))
		{
			AutoConfigIntegration.init();
		}
	}

	public void save() throws IOException
	{
		Files.createDirectories(PATH.getParent());
		BufferedWriter writer = Files.newBufferedWriter(PATH);
		writer.write(JANKSON.toJson(INSTANCE).toJson(true, true));
		writer.close();
	}

	public void load() throws IOException, SyntaxError
	{
		if (Files.exists(PATH))
		{
			var object = JANKSON.load(PATH.toFile());
			INSTANCE = JANKSON.fromJson(object, ModConfig.class);
		}
		else
		{
			save();
		}
	}

	@Comment("If true, shows the hunger and saturation values of food in its tooltip while holding SHIFT")
	public boolean showFoodValuesInTooltip = true;

	@Comment("If true, shows the hunger and saturation values of food in its tooltip automatically (without needing to hold SHIFT)")
	public boolean showFoodValuesInTooltipAlways = true;

	@Comment("If true, shows your current saturation level overlayed on the hunger bar")
	public boolean showSaturationHudOverlay = true;

	@Comment("If true, shows the hunger (and saturation if showSaturationHudOverlay is true) that would be restored by food you are currently holding")
	public boolean showFoodValuesHudOverlay = true;

	@Comment("If true, enables the hunger/saturation/health overlays for food in your off-hand")
	public boolean showFoodValuesHudOverlayWhenOffhand = true;

	@Comment("If true, shows your food exhaustion as a progress bar behind the hunger bar")
	public boolean showFoodExhaustionHudUnderlay = true;

	@Comment("If true, shows estimated health restored by food on the health bar")
	public boolean showFoodHealthHudOverlay = true;

	@Comment("If true, health/hunger overlay will shake to match Minecraft's icon animations")
	public boolean showVanillaAnimationsOverlay = true;

	@Comment("Alpha value of the flashing icons at their most visible point (1.0 = fully opaque, 0.0 = fully transparent)")
	public float maxHudOverlayFlashAlpha = 0.65f;

	@Comment("If true, hovering a Totem of Undying in an inventory screen moves it to your offhand and/or hotbar")
	public boolean hoverTotemEnabled = true;

	@Comment("If true, hover totem moves the totem to your offhand first (when the offhand has no totem)")
	public boolean hoverTotemOffhand = true;

	@Comment("If true, hover totem also moves a totem to the chosen hotbar slot")
	public boolean hoverTotemHotbar = true;

	@Comment("Hotbar slot (1-9) that hover totem fills")
	public int hoverTotemHotbarSlot = 9;

	@Comment("Delay between hover totem moves, in ticks (1 tick = 0.05 seconds)")
	public int hoverTotemDelayTicks = 1;

	@Comment("If true, holding right click while holding an XP bottle throws bottles rapidly")
	public boolean fastXpEnabled = true;

	@Comment("Delay between fast XP throws, in ticks (0 = every tick, 1 tick = 0.05 seconds)")
	public int fastXpDelayTicks = 0;

	@Comment("If true, holding right click on a respawn anchor automatically charges and detonates it")
	public boolean anchorMacroEnabled = false;

	@Comment("Delay before switching items, in ticks (0-20)")
	public int anchorSwitchDelay = 0;

	@Comment("Delay before placing glowstone, in ticks (0-20)")
	public int anchorGlowstoneDelay = 0;

	@Comment("Delay before exploding the anchor, in ticks (0-20)")
	public int anchorExplodeDelay = 0;

	@Comment("Hotbar slot (1-9) switched to when detonating (e.g. your totem slot)")
	public int anchorTotemSlot = 1;

	@Comment("After the anchor detonates, switch back to the respawn anchor in your hotbar")
	public boolean anchorSwapBack = true;

	@Comment("After the anchor detonates, switch to an ender pearl and throw it")
	public boolean anchorPearl = false;

	@Comment("Aim the thrown pearl downward (only the throw packet, no visible camera movement)")
	public boolean anchorPearlAimDown = true;

	@Comment("Pitch of the downward pearl throw (0 = straight ahead, 90 = straight down)")
	public int anchorPearlPitch = 80;

	@Comment("Minimum seconds between Anchor Pearl throws (0 = every time)")
	public int anchorPearlCooldown = 10;

	@Comment("Auto Crystal: while holding the activate button with an end crystal, places and breaks crystals automatically")
	public boolean autoCrystalEnabled = false;

	@Comment("Auto Crystal: mouse button that runs it (0 = left, 1 = right, 2 = middle)")
	public int autoCrystalButton = 1;

	@Comment("Auto Crystal: delay between placements, in ticks (0-20)")
	public int autoCrystalPlaceDelay = 0;

	@Comment("Auto Crystal: delay between breaks, in ticks (0-20)")
	public int autoCrystalBreakDelay = 0;

	@Comment("Auto Crystal: chance (%) of placing on a tick where placing is possible")
	public int autoCrystalPlaceChance = 100;

	@Comment("Auto Crystal: chance (%) of breaking on a tick where breaking is possible")
	public int autoCrystalBreakChance = 100;

	@Comment("Auto Crystal: don't crystal while a dead player is nearby")
	public boolean autoCrystalStopOnKill = false;

	@Comment("Auto Crystal: wait until a nearby player has just been hit, so the crystal lands on the next damage tick")
	public boolean autoCrystalDamageTick = false;

	@Comment("Auto Crystal: with Weakness, briefly switch to a sword to break the crystal, then switch back")
	public boolean autoCrystalAntiWeakness = false;

	@Comment("Double Anchor: holding a respawn anchor and right click on a charged anchor sends one extra interaction packet to it")
	public boolean doubleAnchorEnabled = false;

	@Comment("Double Anchor: chance (%) of the extra packet firing")
	public int doubleAnchorChance = 100;

	@Comment("Double Anchor: cover the spent anchor with glowstone afterwards")
	public boolean doubleAnchorGlowstone = false;

	@Comment("Double Anchor: delay before placing that glowstone, in ticks (0-20)")
	public int doubleAnchorGlowstoneDelay = 2;

	@Comment("Double Anchor Macro: GLFW key code that runs the full place+charge+double-explode sequence on the block you look at (0 = none, set in the module GUI)")
	public int doubleAnchorMacroBind = 0;

	@Comment("Double Anchor Macro: keep repeating the sequence while the macro key is held")
	public boolean doubleAnchorMacroHoldRepeat = false;

	@Comment("Double Anchor Macro: delay between stages, in ticks (0-20)")
	public int doubleAnchorMacroDelay = 2;

	@Comment("Double Anchor Macro: switch items automatically for each stage (off = you must already hold the right item)")
	public boolean doubleAnchorMacroSwap = true;

	@Comment("Double Anchor Macro: switch back to your original slot when the sequence finishes")
	public boolean doubleAnchorMacroSwapBack = true;

	@Comment("Double Anchor Macro: hotbar slot (1-9) switched to for the detonations")
	public int doubleAnchorMacroExplodeSlot = 1;

	@Comment("Double Anchor Macro: delay the final explosion until a nearby player is mid damage-tick")
	public boolean doubleAnchorMacroDamageTick = false;

	@Comment("Double Anchor Macro: range for the damage-tick check, in tenths of a block (20-100 = 2.0-10.0)")
	public int doubleAnchorMacroDamageTickRange10 = 60;

	@Comment("Double Anchor Macro: max time to wait for a damage tick, in milliseconds")
	public int doubleAnchorMacroDamageTickMaxWait = 400;

	@Comment("Show an action-bar message (e.g. \"AutoCrystal: ON\") whenever a module is toggled on/off")
	public boolean showToggleNotifications = true;

	@Comment("Macro 198: while holding the macro button, places obsidian + end crystal on the top face you look at and breaks crystals")
	public boolean macro198Enabled = false;

	@Comment("Macro 198: mouse button that runs it (0 = left, 1 = right, 2 = middle)")
	public int macro198Button = 2;

	@Comment("Macro 198: break placed crystals automatically")
	public boolean macro198AutoBreak = true;

	@Comment("Macro 198: delay between placements, in ticks (0-20)")
	public int macro198PlaceDelay = 0;

	@Comment("Macro 198: delay between breaks, in ticks (0-20)")
	public int macro198BreakDelay = 0;

	@Comment("Macro 198: chance (%) of placing on a tick where placing is possible")
	public int macro198PlaceChance = 100;

	@Comment("Macro 198: chance (%) of attacking a crystal on a tick where attacking is possible")
	public int macro198AttackChance = 100;

	@Comment("Macro 198: swing at air when no crystal is in range")
	public boolean macro198AirSwing = false;

	@Comment("Macro 198: chance (%) of an air swing")
	public int macro198AirSwingChance = 20;

	@Comment("Auto Hit Crystal: hold the button with a sword -> places obsidian, switches to a crystal, then runs Auto Crystal")
	public boolean autoHitCrystalEnabled = false;

	@Comment("Auto Hit Crystal: mouse button that runs it (0 = left, 1 = right, 2 = middle)")
	public int autoHitCrystalButton = 1;

	@Comment("Auto Hit Crystal: only start when obsidian can actually be placed on the block you look at")
	public boolean autoHitCrystalCheckPlace = false;

	@Comment("Auto Hit Crystal: delay before switching items, in ticks (0-20)")
	public int autoHitCrystalSwitchDelay = 0;

	@Comment("Auto Hit Crystal: chance (%) of switching on a tick where switching is possible")
	public int autoHitCrystalSwitchChance = 100;

	@Comment("Auto Hit Crystal: delay between obsidian placements, in ticks (0-20)")
	public int autoHitCrystalPlaceDelay = 0;

	@Comment("Auto Hit Crystal: chance (%) of placing obsidian on a tick where placing is possible")
	public int autoHitCrystalPlaceChance = 100;

	@Comment("Auto Hit Crystal: also start while holding a totem")
	public boolean autoHitCrystalWorkWithTotem = false;

	@Comment("Auto Hit Crystal: also start while holding an end crystal")
	public boolean autoHitCrystalWorkWithCrystal = false;

	@Comment("Auto Hit Crystal: if you look at obsidian/bedrock when starting, skip placing obsidian and go straight to crystals")
	public boolean autoHitCrystalSwordSwap = true;

	@Comment("Auto Hit Crystal: keep placing obsidian back-to-back while the button is held, instead of switching to crystal after one placement")
	public boolean autoHitCrystalContinuousObsidian = false;

	@Comment("Auto Shield Breaker: when you hit a player who is blocking with a shield, switch to an axe from the hotbar first")
	public boolean shieldBreakerEnabled = false;

	@Comment("Auto Shield Breaker: only help hits made while you hold the attack button")
	public boolean shieldBreakerRequireAttackKey = true;

	@Comment("Auto Shield Breaker: only help while a sword, axe or mace is in your main hand")
	public boolean shieldBreakerRequireWeapon = true;

	@Comment("Auto Shield Breaker: return to the previously held slot after the hit")
	public boolean shieldBreakerSwapBack = true;

	@Comment("GUI keybinds: GLFW key code that toggles each module (0 = none). Set them in the module GUI.")
	public int hoverTotemBind = 0;
	public int fastXpBind = 0;
	public int anchorMacroBind = 0;
	public int autoCrystalBind = 0;
	public int doubleAnchorBind = 0;
	public int macro198Bind = 0;
	public int autoHitCrystalBind = 0;
	public int shieldBreakerBind = 0;
}
