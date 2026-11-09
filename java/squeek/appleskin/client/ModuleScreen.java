package squeek.appleskin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.ModConfig;

import java.lang.reflect.Field;
import java.util.List;

/**
 * GUI custom bergaya "Tusing Client": panel gelap, aksen ungu, tab Combat/Misc,
 * daftar modul di kiri, pengaturan modul di kanan (toggle, slider, bind).
 *
 * Input dibaca lewat polling GLFW di render(), jadi tidak bergantung pada
 * signature event mouse/keyboard milik Screen.
 */
public class ModuleScreen extends Screen
{
	// ---------------------------------------------------------------- warna

	private static final int BG = 0xFF15151B;
	private static final int TITLE_BG = 0xFF0F0F14;
	private static final int LEFT_BG = 0xFF18181F;
	private static final int ROW_BG = 0xFF1E1E26;
	private static final int ITEM_SELECTED_BG = 0xFF22222B;
	private static final int ACCENT = 0xFF7C5CFF;
	private static final int ACCENT_LIGHT = 0xFFB8A6FF;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int TEXT_DIM = 0xFF8A8A96;
	private static final int BULLET_DIM = 0xFF55555F;
	private static final int TRACK_OFF = 0xFF2B2B33;

	// ------------------------------------------------------------- ukuran

	private static final int TITLE_H = 18;
	private static final int TAB_H = 17;
	private static final int ITEM_H = 20;
	private static final int HEAD_H = 24;
	private static final int ROW_H = 20;
	private static final int SLIDER_H = 28;
	private static final int GAP = 2;

	private static final String[] TABS = {"Combat", "Misc"};

	// ------------------------------------------------------------ data modul

	static final class Setting
	{
		static final int TOGGLE = 0;
		static final int NUMBER = 1;
		static final int KEYBIND = 2;

		final String label;
		final Field field;
		final int kind;
		final int min;
		final int max;
		final boolean tenths;

		Setting(String label, String fieldName, int kind, int min, int max, boolean tenths)
		{
			this.label = label;
			this.kind = kind;
			this.min = min;
			this.max = max;
			this.tenths = tenths;
			try
			{
				this.field = ModConfig.class.getField(fieldName);
			}
			catch (NoSuchFieldException e)
			{
				throw new IllegalStateException("Missing config field " + fieldName, e);
			}
		}

		boolean getBool()
		{
			try
			{
				return field.getBoolean(ModConfig.INSTANCE);
			}
			catch (IllegalAccessException e)
			{
				return false;
			}
		}

		void setBool(boolean value)
		{
			try
			{
				field.setBoolean(ModConfig.INSTANCE, value);
			}
			catch (IllegalAccessException ignored)
			{
			}
		}

		int getInt()
		{
			try
			{
				return field.getInt(ModConfig.INSTANCE);
			}
			catch (IllegalAccessException e)
			{
				return min;
			}
		}

		void setInt(int value)
		{
			try
			{
				field.setInt(ModConfig.INSTANCE, value);
			}
			catch (IllegalAccessException ignored)
			{
			}
		}

		String format(int value)
		{
			if (tenths)
				return (value / 10) + "." + (value % 10);
			return Integer.toString(value);
		}
	}

	static final class Module
	{
		final String name;
		final int tab;
		final Setting enabled;
		final Setting bind;
		final List<Setting> others;

		Module(String name, int tab, Setting enabled, Setting bind, List<Setting> others)
		{
			this.name = name;
			this.tab = tab;
			this.enabled = enabled;
			this.bind = bind;
			this.others = others;
		}
	}

	private static Setting toggle(String label, String field)
	{
		return new Setting(label, field, Setting.TOGGLE, 0, 0, false);
	}

	private static Setting number(String label, String field, int min, int max)
	{
		return new Setting(label, field, Setting.NUMBER, min, max, false);
	}

	private static Setting tenths(String label, String field, int min, int max)
	{
		return new Setting(label, field, Setting.NUMBER, min, max, true);
	}

	private static Setting bind(String field)
	{
		return new Setting("Bind", field, Setting.KEYBIND, 0, 0, false);
	}

	static final List<Module> MODULES = List.of(
		new Module("AutoCrystal", 0, toggle("Enabled", "autoCrystalEnabled"), bind("autoCrystalBind"), List.of(
			number("Button (0/1/2)", "autoCrystalButton", 0, 2),
			number("PlaceDelay (tick)", "autoCrystalPlaceDelay", 0, 20),
			number("BreakDelay (tick)", "autoCrystalBreakDelay", 0, 20),
			number("PlaceChance (%)", "autoCrystalPlaceChance", 0, 100),
			number("BreakChance (%)", "autoCrystalBreakChance", 0, 100),
			toggle("StopOnKill", "autoCrystalStopOnKill"),
			toggle("DamageTick", "autoCrystalDamageTick"),
			toggle("AntiWeakness", "autoCrystalAntiWeakness"))),
		new Module("AnchorMacro", 0, toggle("Enabled", "anchorMacroEnabled"), bind("anchorMacroBind"), List.of(
			number("SwitchDelay (tick)", "anchorSwitchDelay", 0, 20),
			number("GlowstoneDelay (tick)", "anchorGlowstoneDelay", 0, 20),
			number("ExplodeDelay (tick)", "anchorExplodeDelay", 0, 20),
			number("TotemSlot", "anchorTotemSlot", 1, 9),
			toggle("SwapBack", "anchorSwapBack"),
			toggle("Pearl", "anchorPearl"),
			toggle("PearlAimDown", "anchorPearlAimDown"),
			number("PearlPitch", "anchorPearlPitch", 0, 90),
			number("PearlCooldown (s)", "anchorPearlCooldown", 0, 60))),
		new Module("HoverTotem", 0, toggle("Enabled", "hoverTotemEnabled"), bind("hoverTotemBind"), List.of(
			toggle("Offhand", "hoverTotemOffhand"),
			toggle("Hotbar", "hoverTotemHotbar"),
			number("HotbarSlot", "hoverTotemHotbarSlot", 1, 9),
			number("Delay (tick)", "hoverTotemDelayTicks", 0, 10))),
		new Module("AutoHitCrystal", 0, toggle("Enabled", "autoHitCrystalEnabled"), bind("autoHitCrystalBind"), List.of(
			number("Button (0/1/2)", "autoHitCrystalButton", 0, 2),
			toggle("CheckPlace", "autoHitCrystalCheckPlace"),
			number("SwitchDelay (tick)", "autoHitCrystalSwitchDelay", 0, 20),
			number("SwitchChance (%)", "autoHitCrystalSwitchChance", 0, 100),
			number("PlaceDelay (tick)", "autoHitCrystalPlaceDelay", 0, 20),
			number("PlaceChance (%)", "autoHitCrystalPlaceChance", 0, 100),
			toggle("WorkWithTotem", "autoHitCrystalWorkWithTotem"),
			toggle("WorkWithCrystal", "autoHitCrystalWorkWithCrystal"),
			toggle("SwordSwap", "autoHitCrystalSwordSwap"))),
		new Module("AutoShieldBreaker", 0, toggle("Enabled", "shieldBreakerEnabled"), bind("shieldBreakerBind"), List.of(
			toggle("RequireAttackKey", "shieldBreakerRequireAttackKey"),
			toggle("RequireWeapon", "shieldBreakerRequireWeapon"),
			toggle("SwapBack", "shieldBreakerSwapBack"))),
		new Module("DoubleAnchor", 0, toggle("Enabled", "doubleAnchorEnabled"), bind("doubleAnchorBind"), List.of(
			number("Chance (%)", "doubleAnchorChance", 0, 100),
			toggle("GlowstoneCover", "doubleAnchorGlowstone"),
			number("CoverDelay (tick)", "doubleAnchorGlowstoneDelay", 0, 20),
			new Setting("MacroBind", "doubleAnchorMacroBind", Setting.KEYBIND, 0, 0, false),
			toggle("MacroHoldRepeat", "doubleAnchorMacroHoldRepeat"),
			number("MacroDelay (tick)", "doubleAnchorMacroDelay", 0, 20),
			toggle("MacroSwap", "doubleAnchorMacroSwap"),
			toggle("MacroSwapBack", "doubleAnchorMacroSwapBack"),
			number("MacroExplodeSlot", "doubleAnchorMacroExplodeSlot", 1, 9),
			toggle("MacroDamageTick", "doubleAnchorMacroDamageTick"),
			tenths("MacroDmgTickRange", "doubleAnchorMacroDamageTickRange10", 20, 100),
			number("MacroDmgTickWait (ms)", "doubleAnchorMacroDamageTickMaxWait", 0, 2000))),
		new Module("Macro198", 0, toggle("Enabled", "macro198Enabled"), bind("macro198Bind"), List.of(
			number("Button (0/1/2)", "macro198Button", 0, 2),
			toggle("AutoBreak", "macro198AutoBreak"),
			number("PlaceDelay (tick)", "macro198PlaceDelay", 0, 20),
			number("BreakDelay (tick)", "macro198BreakDelay", 0, 20),
			number("PlaceChance (%)", "macro198PlaceChance", 0, 100),
			number("AttackChance (%)", "macro198AttackChance", 0, 100),
			toggle("AirSwing", "macro198AirSwing"),
			number("AirSwingChance (%)", "macro198AirSwingChance", 0, 100))),
		new Module("FastXP", 1, toggle("Enabled", "fastXpEnabled"), bind("fastXpBind"), List.of(
			number("Delay (tick)", "fastXpDelayTicks", 0, 10)))
	);

	// ------------------------------------------------------------- state GUI

	private static int selected = 0;

	private boolean wasDown;
	private boolean wasRightDown;
	private Setting dragSlider;
	private boolean dragScroll;
	private int dragStartY;
	private float scrollStart;
	private float scroll;
	private Setting listening;
	private final boolean[] ignoreKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];

	public ModuleScreen()
	{
		super(Text.literal("Tusing Client"));
	}

	// ------------------------------------------------------------------ util

	private static boolean in(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private int textW(String s)
	{
		return this.textRenderer.getWidth(s);
	}

	private void text(DrawContext c, String s, int x, int y, int color)
	{
		c.drawText(this.textRenderer, s, x, y, color, false);
	}

	private void drawSwitch(DrawContext c, int x, int y, boolean on)
	{
		c.fill(x, y, x + 22, y + 10, on ? ACCENT : TRACK_OFF);
		int knobX = on ? x + 12 : x;
		c.fill(knobX, y, knobX + 10, y + 10, TEXT);
	}

	static String keyName(int key)
	{
		if (key <= 0)
			return "NONE";
		if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25)
			return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
		switch (key)
		{
			case GLFW.GLFW_KEY_SPACE: return "SPACE";
			case GLFW.GLFW_KEY_ENTER: return "ENTER";
			case GLFW.GLFW_KEY_TAB: return "TAB";
			case GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
			case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
			case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
			case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
			case GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
			case GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
			case GLFW.GLFW_KEY_CAPS_LOCK: return "CAPS";
			case GLFW.GLFW_KEY_UP: return "UP";
			case GLFW.GLFW_KEY_DOWN: return "DOWN";
			case GLFW.GLFW_KEY_LEFT: return "LEFT";
			case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
			case GLFW.GLFW_KEY_INSERT: return "INSERT";
			case GLFW.GLFW_KEY_HOME: return "HOME";
			case GLFW.GLFW_KEY_END: return "END";
			case GLFW.GLFW_KEY_PAGE_UP: return "PGUP";
			case GLFW.GLFW_KEY_PAGE_DOWN: return "PGDN";
			default: break;
		}
		String name = GLFW.glfwGetKeyName(key, 0);
		return name != null ? name.toUpperCase() : "KEY" + key;
	}

	private static int rowHeight(Setting s)
	{
		return s.kind == Setting.NUMBER ? SLIDER_H : ROW_H;
	}

	private void startListening(long window, Setting setting)
	{
		listening = setting;
		for (int k = 32; k <= GLFW.GLFW_KEY_LAST; k++)
			ignoreKeys[k] = GLFW.glfwGetKey(window, k) == GLFW.GLFW_PRESS;
	}

	/** Tombol pertama yang ditekan setelah klik Bind jadi bind-nya. Backspace/Delete = NONE. Esc menutup GUI. */
	private void pollListening(long window)
	{
		if (listening == null)
			return;
		for (int k = 32; k <= GLFW.GLFW_KEY_LAST; k++)
		{
			boolean pressed = GLFW.glfwGetKey(window, k) == GLFW.GLFW_PRESS;
			if (!pressed)
			{
				ignoreKeys[k] = false;
				continue;
			}
			if (ignoreKeys[k] || k == GLFW.GLFW_KEY_ESCAPE)
				continue;
			boolean clear = k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE;
			listening.setInt(clear ? 0 : k);
			ModuleGuiHandler.markDirty();
			listening = null;
			return;
		}
	}

	// ---------------------------------------------------------------- render

	@Override
	public void render(DrawContext c, int mouseX, int mouseY, float delta)
	{
		long window = this.client.getWindow().getHandle();
		boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		boolean clicked = down && !wasDown;
		boolean rightClicked = rightDown && !wasRightDown;
		wasDown = down;
		wasRightDown = rightDown;

		if (!down)
		{
			if (dragSlider != null)
				ModuleGuiHandler.markDirty();
			dragSlider = null;
			dragScroll = false;
		}

		pollListening(window);

		if (selected < 0 || selected >= MODULES.size())
			selected = 0;
		Module module = MODULES.get(selected);

		// ---- ukuran panel
		int pw = Math.min(330, this.width - 8);
		int ph = Math.min(232, this.height - 8);
		int leftW = Math.min(88, pw / 3);
		int px = (this.width - pw) / 2;
		int py = (this.height - ph) / 2;

		// ---- panel + title bar
		c.fill(px, py, px + pw, py + ph, BG);
		c.fill(px, py, px + pw, py + TITLE_H, TITLE_BG);
		c.fill(px, py + TITLE_H, px + pw, py + TITLE_H + 1, ACCENT);
		text(c, "Tusing Client", px + 8, py + (TITLE_H - 8) / 2, ACCENT);

		int closeX = px + pw - 15;
		int closeY = py + (TITLE_H - 9) / 2;
		c.fill(closeX, closeY, closeX + 9, closeY + 9, ACCENT);
		if (clicked && in(mouseX, mouseY, closeX - 3, closeY - 3, 15, 15))
		{
			this.client.setScreen(null);
			return;
		}

		int bodyY = py + TITLE_H + 1;
		c.fill(px, bodyY, px + leftW, py + ph, LEFT_BG);

		// ---- tab Combat / Misc
		int tabX = px + 8;
		for (int i = 0; i < TABS.length; i++)
		{
			int w = textW(TABS[i]);
			boolean active = MODULES.get(selected).tab == i;
			text(c, TABS[i], tabX, bodyY + 4, active ? ACCENT : TEXT_DIM);
			if (active)
				c.fill(tabX, bodyY + 14, tabX + w, bodyY + 15, ACCENT);
			if (clicked && in(mouseX, mouseY, tabX - 2, bodyY, w + 4, TAB_H))
			{
				for (int m = 0; m < MODULES.size(); m++)
				{
					if (MODULES.get(m).tab == i)
					{
						selected = m;
						break;
					}
				}
				scroll = 0;
				listening = null;
				return;
			}
			tabX += w + 10;
		}

		// ---- daftar modul
		int itemY = bodyY + TAB_H + 4;
		for (int m = 0; m < MODULES.size(); m++)
		{
			Module item = MODULES.get(m);
			if (item.tab != module.tab)
				continue;

			boolean isSelected = m == selected;
			if (isSelected)
			{
				c.fill(px, itemY, px + leftW, itemY + ITEM_H, ITEM_SELECTED_BG);
				c.fill(px, itemY, px + 2, itemY + ITEM_H, ACCENT);
			}
			int bulletY = itemY + ITEM_H / 2 - 1;
			c.fill(px + 8, bulletY, px + 11, bulletY + 3, isSelected ? ACCENT : BULLET_DIM);
			int nameColor = isSelected ? TEXT : (item.enabled.getBool() ? ACCENT_LIGHT : TEXT_DIM);
			text(c, item.name, px + 16, itemY + (ITEM_H - 8) / 2, nameColor);

			if (clicked && in(mouseX, mouseY, px, itemY, leftW, ITEM_H))
			{
				selected = m;
				scroll = 0;
				listening = null;
				return;
			}
			itemY += ITEM_H + GAP;
		}

		// ---- kanan: header modul (nama + toggle utama)
		int rx = px + leftW;
		int rw = pw - leftW;
		text(c, module.name, rx + 10, bodyY + (HEAD_H - 8) / 2 + 1, TEXT);
		int switchX = rx + rw - 10 - 22;
		int switchY = bodyY + (HEAD_H - 10) / 2 + 1;
		drawSwitch(c, switchX, switchY, module.enabled.getBool());
		if (clicked && in(mouseX, mouseY, switchX - 4, switchY - 4, 30, 18))
		{
			module.enabled.setBool(!module.enabled.getBool());
			ModuleGuiHandler.markDirty();
		}

		// ---- kanan: baris pengaturan (bisa di-scroll)
		List<Setting> rows = new java.util.ArrayList<>();
		rows.add(module.bind);
		rows.addAll(module.others);

		int viewTop = bodyY + HEAD_H + 4;
		int viewBottom = py + ph - 4;
		int viewH = viewBottom - viewTop;
		int contentH = 0;
		for (int i = 0; i < rows.size(); i++)
			contentH += rowHeight(rows.get(i)) + (i > 0 ? GAP : 0);
		int maxScroll = Math.max(0, contentH - viewH);

		// scrollbar (geser thumb-nya dengan jari/mouse)
		int barX = rx + rw - 6;
		int thumbH = 0;
		int thumbY = viewTop;
		if (maxScroll > 0)
		{
			thumbH = Math.max(16, viewH * viewH / contentH);
			thumbY = viewTop + (int) (scroll / maxScroll * (viewH - thumbH));
			if (clicked && in(mouseX, mouseY, barX - 4, viewTop, 10, viewH))
			{
				dragScroll = true;
				dragStartY = mouseY;
				scrollStart = scroll;
			}
			if (dragScroll && down && viewH > thumbH)
				scroll = scrollStart + (mouseY - dragStartY) * (float) maxScroll / (viewH - thumbH);
		}
		scroll = MathHelper.clamp(scroll, 0.0F, (float) maxScroll);
		if (maxScroll > 0)
			thumbY = viewTop + (int) (scroll / maxScroll * (viewH - thumbH));

		int rowX = rx + 6;
		int rowW = rw - 6 - (maxScroll > 0 ? 10 : 6);

		c.enableScissor(rx, viewTop, rx + rw, viewBottom);
		int offset = 0;
		for (int i = 0; i < rows.size(); i++)
		{
			Setting s = rows.get(i);
			int h = rowHeight(s);
			int ry = viewTop - (int) scroll + offset;
			offset += h + GAP;
			if (ry + h < viewTop || ry > viewBottom)
				continue;

			boolean hover = in(mouseX, mouseY, rowX, ry, rowW, h) && mouseY >= viewTop && mouseY < viewBottom;
			c.fill(rowX, ry, rowX + rowW, ry + h, ROW_BG);

			if (s.kind == Setting.TOGGLE)
			{
				text(c, s.label, rowX + 6, ry + (h - 8) / 2, TEXT_DIM);
				drawSwitch(c, rowX + rowW - 6 - 22, ry + (h - 10) / 2, s.getBool());
				if (clicked && hover)
				{
					s.setBool(!s.getBool());
					ModuleGuiHandler.markDirty();
				}
			}
			else if (s.kind == Setting.KEYBIND)
			{
				text(c, s.label, rowX + 6, ry + (h - 8) / 2, TEXT_DIM);
				String value = listening == s ? "..." : keyName(s.getInt());
				text(c, value, rowX + rowW - 6 - textW(value), ry + (h - 8) / 2, listening == s ? ACCENT : TEXT);
				if (hover && clicked)
					startListening(window, s);
				else if (hover && rightClicked)
				{
					s.setInt(0);
					listening = null;
					ModuleGuiHandler.markDirty();
				}
			}
			else
			{
				int value = MathHelper.clamp(s.getInt(), s.min, s.max);
				String valueText = s.format(value);
				text(c, s.label, rowX + 6, ry + 5, TEXT_DIM);
				text(c, valueText, rowX + rowW - 6 - textW(valueText), ry + 5, TEXT);

				int barLeft = rowX + 6;
				int barW = rowW - 12;
				int barY = ry + h - 9;
				if (clicked && hover)
					dragSlider = s;
				if (dragSlider == s && down)
				{
					float frac = MathHelper.clamp((mouseX - barLeft) / (float) barW, 0.0F, 1.0F);
					value = s.min + Math.round(frac * (s.max - s.min));
					s.setInt(value);
				}

				float frac = (value - s.min) / (float) (s.max - s.min);
				int fillX = barLeft + (int) (frac * barW);
				c.fill(barLeft, barY, barLeft + barW, barY + 2, TRACK_OFF);
				c.fill(barLeft, barY, fillX, barY + 2, ACCENT);
				c.fill(fillX - 1, barY - 3, fillX + 2, barY + 5, TEXT);
			}
		}
		c.disableScissor();

		if (maxScroll > 0)
		{
			c.fill(barX, viewTop, barX + 3, viewBottom, TRACK_OFF);
			c.fill(barX, thumbY, barX + 3, thumbY + thumbH, ACCENT);
		}
	}
}
