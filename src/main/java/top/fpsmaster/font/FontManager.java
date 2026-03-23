package top.fpsmaster.font;

import top.fpsmaster.font.FontRendererHook;
import top.fpsmaster.font.impl.UFontRenderer;

import java.util.HashMap;

public class FontManager {

    public UFontRenderer s14;
    public UFontRenderer s16;
    public UFontRenderer s18;
    public UFontRenderer s20;
    public UFontRenderer s22;
    public UFontRenderer s24;
    public UFontRenderer s28;
    public UFontRenderer s36;
    public UFontRenderer s40;

    public void load(){
        releaseFonts();
        fonts.clear();
        s14 = new UFontRenderer("NotoSansSC-Regular",14);
        s16 = new UFontRenderer("NotoSansSC-Regular",16);
        s18 = new UFontRenderer("NotoSansSC-Regular",18);
        s20 = new UFontRenderer("NotoSansSC-Regular",20);
        s22 = new UFontRenderer("NotoSansSC-Regular",22);
        s24 = new UFontRenderer("NotoSansSC-Regular",24);
        s28 = new UFontRenderer("NotoSansSC-Regular",28);
        s36 = new UFontRenderer("NotoSansSC-Regular",36);
        s40 = new UFontRenderer("NotoSansSC-Regular",40);
        EnhancedFontRenderer.releaseAllInstances();
        FontRendererHook.forceRefresh = true;
    }

    HashMap<Integer, UFontRenderer> fonts = new HashMap<>();

    public UFontRenderer getFont(int size) {
        if (fonts.containsKey(size)) {
            return fonts.get(size);
        }
        UFontRenderer harmonyBold = new UFontRenderer("NotoSansSC-Regular", size);
        fonts.put(size, harmonyBold);
        return harmonyBold;
    }

    private void releaseFonts() {
        destroyFont(s14);
        destroyFont(s16);
        destroyFont(s18);
        destroyFont(s20);
        destroyFont(s22);
        destroyFont(s24);
        destroyFont(s28);
        destroyFont(s36);
        destroyFont(s40);
        for (UFontRenderer font : fonts.values()) {
            destroyFont(font);
        }
    }

    private void destroyFont(UFontRenderer font) {
        if (font != null) {
            font.destroy();
        }
    }
}



