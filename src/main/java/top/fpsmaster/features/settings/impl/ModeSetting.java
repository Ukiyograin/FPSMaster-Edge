package top.fpsmaster.features.settings.impl;

import top.fpsmaster.features.settings.Setting;

import java.util.Objects;

public class ModeSetting extends Setting<Integer> {

    private final String[] modes;

    public ModeSetting(String name, int value, String... modes) {
        super(name, value);
        this.modes = modes;
    }

    public ModeSetting(String name, int value, VisibleCondition visible, String... modes) {
        super(name, value, visible);
        this.modes = modes;
    }

    public void cycle() {
        setValue(normalizeIndex(getValue() + 1));
    }

    public String getMode(int num) {
        return modes[normalizeIndex(num - 1)];
    }

    public boolean isMode(String mode) {
        return Objects.equals(modes[normalizeIndex(getValue())], mode);
    }

    public String getModeName() {
        return modes[normalizeIndex(getValue())];
    }

    public int getMode() {
        return normalizeIndex(getValue());
    }

    public int getModesSize() {
        return modes.length;
    }

    @Override
    public void setValue(Integer value) {
        super.setValue(normalizeIndex(value == null ? 0 : value));
    }

    private int normalizeIndex(int index) {
        if (modes.length == 0) {
            return 0;
        }
        if (index < 0) {
            return 0;
        }
        if (index >= modes.length) {
            return modes.length - 1;
        }
        return index;
    }
}



