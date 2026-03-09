package top.fpsmaster.ui.screens.oobe;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;

public class OobeDropdown {
    private String label = "";
    private String[] items = new String[0];
    private int selectedIndex;
    private boolean open;
    private boolean enabled = true;
    private float openProgress;
    private final AnimClock clock = new AnimClock();

    public OobeDropdown setLabel(String label) {
        this.label = label;
        return this;
    }

    public OobeDropdown setItems(String[] items) {
        this.items = items == null ? new String[0] : items;
        if (selectedIndex >= this.items.length) {
            selectedIndex = Math.max(0, this.items.length - 1);
        }
        return this;
    }

    public OobeDropdown setSelectedIndex(int selectedIndex) {
        this.selectedIndex = Math.max(0, Math.min(selectedIndex, Math.max(0, items.length - 1)));
        return this;
    }

    public OobeDropdown setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            open = false;
        }
        return this;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void renderInScreen(ScaledGuiScreen screen, float x, float y, float width, float height, int mouseX, int mouseY) {
        float dt = (float) clock.tick();
        float target = open && enabled ? 1f : 0f;
        openProgress += (target - openProgress) * Math.min(1f, dt * 10f);

        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 12, new Color(255, 255, 255, enabled ? 224 : 168).getRGB());
        FPSMaster.fontManager.s16.drawString(label, x + 12f, y + 11f, new Color(58, 68, 96, enabled ? 255 : 140).getRGB());
        String selected = items.length == 0 ? "" : items[selectedIndex];
        FPSMaster.fontManager.s16.drawString(selected, x + width - 64f, y + 11f, new Color(58, 68, 96, enabled ? 255 : 140).getRGB());
        FPSMaster.fontManager.s16.drawString(openProgress > 0.5f ? "^" : "v", x + width - 24f, y + 11f, new Color(58, 68, 96, enabled ? 255 : 140).getRGB());

        if (enabled && screen.consumePressInBounds(x, y, width, height, 0) != null) {
            open = !open;
        }

        if (openProgress > 0.02f && enabled) {
            for (int i = 0; i < items.length; i++) {
                float optionY = y + height + 6f + i * 32f * openProgress;
                int alpha = Math.max(0, Math.min(255, (int) (openProgress * 255f)));
                Rects.rounded(Math.round(x), Math.round(optionY), Math.round(width), 26, 10,
                        (i == selectedIndex ? new Color(122, 139, 255, Math.min(230, alpha)) : new Color(255, 255, 255, Math.min(232, alpha))).getRGB());
                FPSMaster.fontManager.s16.drawString(items[i], x + 12f, optionY + 9f,
                        (i == selectedIndex ? new Color(255, 255, 255, alpha) : new Color(58, 68, 96, alpha)).getRGB());
                if (openProgress > 0.95f && screen.consumePressInBounds(x, optionY, width, 28f, 0) != null) {
                    selectedIndex = i;
                    open = false;
                }
            }

            ScaledGuiScreen.PointerEvent outside = screen.peekAnyPress();
            if (outside != null && openProgress > 0.95f && !Hover.is(x, y, width, height + items.length * 32f + 12f, outside.x, outside.y)) {
                open = false;
            }
        }
    }
}
