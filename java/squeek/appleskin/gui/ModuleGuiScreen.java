package squeek.appleskin.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import squeek.appleskin.ModConfig;

import java.io.IOException;

public class ModuleGuiScreen extends Screen
{
    private static final int PANEL_W = 260;
    private static final int ROW_H = 25;
    private static final String[] MODULES = {"Hover Totem", "Fast XP", "Anchor Macro", "Auto Crystal", "Double Anchor", "Macro 198"};

    public ModuleGuiScreen() { super(Text.literal("Tusing Modules")); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        this.renderBackground(context, mouseX, mouseY, delta);
        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - (MODULES.length * ROW_H + 54)) / 2;
        context.fill(x - 1, y - 1, x + PANEL_W + 1, y + MODULES.length * ROW_H + 48, 0xFF11131B);
        context.fill(x, y, x + PANEL_W, y + MODULES.length * ROW_H + 46, 0xFF202431);
        context.drawText(this.textRenderer, "TUSING CLIENT", x + 12, y + 10, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, "Modules  |  Right Shift to close", x + 12, y + 26, 0xFF9CA8C0, false);

        for (int i = 0; i < MODULES.length; i++) {
            int ry = y + 46 + i * ROW_H;
            boolean enabled = isEnabled(i);
            boolean hovered = mouseX >= x + 8 && mouseX <= x + PANEL_W - 8 && mouseY >= ry && mouseY < ry + ROW_H;
            context.fill(x + 8, ry, x + PANEL_W - 8, ry + ROW_H - 2, hovered ? 0xFF343D50 : 0xFF292F3D);
            context.drawText(this.textRenderer, MODULES[i], x + 16, ry + 7, 0xFFE7EAF2, false);
            String state = enabled ? "ON" : "OFF";
            int color = enabled ? 0xFF65E69A : 0xFFFF7777;
            context.drawText(this.textRenderer, state, x + PANEL_W - 42, ry + 7, color, false);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0) {
            int x = (this.width - PANEL_W) / 2;
            int y = (this.height - (MODULES.length * ROW_H + 54)) / 2;
            int row = (int) mouseY - (y + 46);
            if (mouseX >= x + 8 && mouseX <= x + PANEL_W - 8 && row >= 0) {
                int index = row / ROW_H;
                if (index < MODULES.length && row % ROW_H < ROW_H - 2) {
                    setEnabled(index, !isEnabled(index));
                    saveConfig();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isEnabled(int i) {
        var c = ModConfig.INSTANCE;
        return switch (i) {
            case 0 -> c.hoverTotemEnabled;
            case 1 -> c.fastXpEnabled;
            case 2 -> c.anchorMacroEnabled;
            case 3 -> c.autoCrystalEnabled;
            case 4 -> c.doubleAnchorEnabled;
            case 5 -> c.macro198Enabled;
            default -> false;
        };
    }

    private void setEnabled(int i, boolean value) {
        var c = ModConfig.INSTANCE;
        switch (i) {
            case 0 -> c.hoverTotemEnabled = value;
            case 1 -> c.fastXpEnabled = value;
            case 2 -> c.anchorMacroEnabled = value;
            case 3 -> c.autoCrystalEnabled = value;
            case 4 -> c.doubleAnchorEnabled = value;
            case 5 -> c.macro198Enabled = value;
        }
    }

    private void saveConfig() {
        try { ModConfig.INSTANCE.save(); }
        catch (IOException e) { e.printStackTrace(); }
    }

    @Override public boolean shouldPause() { return false; }
}
