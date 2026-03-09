package top.fpsmaster.ui.screens.oobe;

import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.impl.interfaces.ComboDisplay;
import top.fpsmaster.features.impl.interfaces.CoordsDisplay;
import top.fpsmaster.features.impl.interfaces.CPSDisplay;
import top.fpsmaster.features.impl.interfaces.DirectionDisplay;
import top.fpsmaster.features.impl.interfaces.FPSDisplay;
import top.fpsmaster.features.impl.interfaces.InventoryDisplay;
import top.fpsmaster.features.impl.interfaces.Keystrokes;
import top.fpsmaster.features.impl.interfaces.PingDisplay;
import top.fpsmaster.features.impl.optimizes.OldAnimations;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.features.impl.render.ItemPhysics;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;
import java.io.IOException;

public class OobeScreen extends ScaledGuiScreen {
    private static final int PAGE_COUNT = 8;
    private static final double[] SCALE_VALUES = new double[]{0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0};
    private static final String[] SCALE_LABELS = new String[]{"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x", "2.5x", "3.0x"};
    private static final String[] GREETINGS = new String[]{"Hello", "你好", "Welcome", "欢迎", "Bonjour", "こんにちは"};
    private static final ResourceLocation PREVIEW_IMAGE = new ResourceLocation("client/background/panorama_1/panorama_0.png");

    private final AnimClock animClock = new AnimClock();
    private final float[] featureExpand = new float[]{0f, 0f, 0f, 0f};
    private final OobeDropdown scaleDropdown = new OobeDropdown();

    private int page;
    private int languageValue;
    private boolean fixedScaleEnabled;
    private int fixedScaleIndex;
    private boolean scaleDropdownOpen;
    private int tutorialIndex;
    private int expandedFeature = -1;
    private boolean antiCheatEnabled;
    private boolean anonymousDataEnabled;
    private boolean enterGuide = true;
    private int qaStep;
    private final int[] qaAnswers = new int[]{-1, -1, -1};
    private String backgroundChoice;
    private boolean loginSkipped = true;
    private float pageMotion;
    private int pageMotionDirection = 1;

    private TextField accountField;
    private TextField passwordField;
    private OobeButton backButton;
    private OobeButton nextButton;
    private OobeButton tutorialPrevButton;
    private OobeButton tutorialNextButton;
    private OobeButton loginButton;
    private OobeButton skipLoginButton;

    @Override
    public void initGui() {
        super.initGui();
        animClock.reset();
        backButton = new OobeButton("Back", false, () -> {
            if (page > 0) {
                page--;
                pageMotion = 1f;
                pageMotionDirection = -1;
                scaleDropdownOpen = false;
            }
        });
        nextButton = new OobeButton("Next", true, this::onNext);
        tutorialPrevButton = new OobeButton("Prev", false, () -> tutorialIndex = (tutorialIndex + 2) % 3);
        tutorialNextButton = new OobeButton("Next", true, () -> tutorialIndex = (tutorialIndex + 1) % 3);
        loginButton = new OobeButton("Sign in", true, () -> loginSkipped = false);
        skipLoginButton = new OobeButton("Skip", false, () -> loginSkipped = true);
        languageValue = ClientSettings.language.getValue();
        fixedScaleEnabled = ClientSettings.fixedScaleEnabled.getValue();
        fixedScaleIndex = ClientSettings.fixedScale.getValue();
        antiCheatEnabled = FPSMaster.configManager.configure.antiCheatEnabled;
        anonymousDataEnabled = FPSMaster.configManager.configure.anonymousDataEnabled;
        backgroundChoice = FPSMaster.configManager.configure.background == null ? "panorama_1" : FPSMaster.configManager.configure.background;
        setPreviewLanguage(languageValue);
        scaleDropdown.setItems(SCALE_LABELS).setSelectedIndex(fixedScaleIndex).setEnabled(fixedScaleEnabled);
        accountField = new TextField(FPSMaster.fontManager.s18, key("oobe.login.account.placeholder"), new Color(255, 255, 255, 235).getRGB(), Color.WHITE.getRGB(), 32);
        passwordField = new TextField(FPSMaster.fontManager.s18, true, key("oobe.login.password.placeholder"), new Color(255, 255, 255, 235).getRGB(), Color.WHITE.getRGB(), 32);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        float dt = (float) animClock.tick();
        updateAnimations(dt);

        renderBackground();
        drawFrame();
        renderProgress();
        renderCurrentPage(mouseX, mouseY);
        renderFooter(mouseX, mouseY);
    }

    private void drawFrame() {
        Rects.roundedBorder(Math.round(frameLeft()), Math.round(frameTop()), Math.round(frameWidth()), Math.round(frameHeight()), 24, 1f,
                new Color(255, 255, 255, 142).getRGB(), new Color(255, 255, 255, 156).getRGB());
    }

    private void renderBackground() {
        Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(244, 247, 255, 255));
        Rects.fill(0f, 0f, guiWidth, guiHeight * 0.58f, new Color(248, 250, 255, 255));
        Rects.fill(0f, guiHeight * 0.42f, guiWidth, guiHeight * 0.58f, new Color(232, 238, 252, 168));
    }

    private void updateAnimations(float dt) {
        float speed = Math.min(1f, dt * 6f);
        for (int i = 0; i < featureExpand.length; i++) {
            float target = expandedFeature == i ? 1f : 0f;
            featureExpand[i] += (target - featureExpand[i]) * speed;
        }
        pageMotion += (0f - pageMotion) * Math.min(1f, dt * 8.5f);
        accountField.updateCursorCounter();
        passwordField.updateCursorCounter();
    }

    private void renderProgress() {
        float x = frameRight() - 18f - PAGE_COUNT * 14f;
        float y = frameTop() + 14f;
        FPSMaster.fontManager.s14.drawString((page + 1) + " / " + PAGE_COUNT, x - 42f, y - 1f, new Color(220, 228, 255, 130).getRGB());
        for (int i = 0; i < PAGE_COUNT; i++) {
            int color = i == page ? new Color(104, 117, 247, 255).getRGB() : (i < page ? new Color(104, 117, 247, 120).getRGB() : new Color(255, 255, 255, 60).getRGB());
            Rects.rounded(Math.round(x + i * 14f), Math.round(y), 7, 7, 4, color);
        }
    }

    private void renderCurrentPage(int mouseX, int mouseY) {
        GL11.glPushMatrix();
        GL11.glTranslatef(pageMotionDirection * pageMotion * 28f, 0f, 0f);
        switch (page) {
            case 0:
                renderLanguagePage(mouseX, mouseY);
                break;
            case 1:
                renderScalePage(mouseX, mouseY);
                break;
            case 2:
                renderTutorialPage(mouseX, mouseY);
                break;
            case 3:
                renderFeaturesPage(mouseX, mouseY);
                break;
            case 4:
                renderLoginPage(mouseX, mouseY);
                break;
            case 5:
                renderOptionsPage(mouseX, mouseY);
                break;
            case 6:
                renderGuideEntryPage(mouseX, mouseY);
                break;
            case 7:
                renderQaPage(mouseX, mouseY);
                break;
            default:
                break;
        }
        GL11.glPopMatrix();
    }

    private void renderLanguagePage(int mouseX, int mouseY) {
        float x = leftColumnX();
        float y = pageTop();
        FPSMaster.fontManager.s18.drawString(animatedGreeting(), x, y - 28f, accentText().getRGB());
        FPSMaster.fontManager.s28.drawString(key("oobe.language.title"), x, y, titleText().getRGB());
        FPSMaster.fontManager.s18.drawString(key("oobe.language.desc"), x, y + 34f, bodyText().getRGB());

        renderPill(x, y + 86f, 108f, 30f, languageValue == 1, key("oobe.language.zh"));
        renderPill(x + 120f, y + 86f, 108f, 30f, languageValue == 0, "English");
        handleLanguageClicks(x, y + 86f);
    }

    private void renderScalePage(int mouseX, int mouseY) {
        float leftX = leftColumnX();
        float topY = pageTop();
        float rightWidth = clamp(frameWidth() * 0.34f, 300f, 360f);
        float previewX = frameRight() - rightWidth - pageSidePadding();
        float previewY = topY - 4f;
        FPSMaster.fontManager.s28.drawString(key("oobe.scale.title"), leftX, topY, titleText().getRGB());
        FPSMaster.fontManager.s18.drawString(key("oobe.scale.desc"), leftX, topY + 34f, bodyText().getRGB());

        renderPill(leftX, topY + 86f, 140f, 30f, fixedScaleEnabled, key("oobe.scale.fixed"));
        renderPill(leftX + 152f, topY + 86f, 146f, 30f, !fixedScaleEnabled, key("oobe.scale.follow"));

        float dropdownY = topY + 130f;
        scaleDropdown.setLabel(key("oobe.scale.label")).setItems(SCALE_LABELS).setSelectedIndex(fixedScaleIndex).setEnabled(fixedScaleEnabled);
        scaleDropdown.renderInScreen(this, leftX, dropdownY, clamp(frameWidth() * 0.22f, 190f, 232f), 34f, mouseX, mouseY);
        fixedScaleIndex = scaleDropdown.getSelectedIndex();

        float previewWidth = rightWidth;
        float previewHeight = previewWidth * 0.7f;
        drawCard(previewX, previewY, previewWidth, previewHeight, new Color(22, 31, 47, 215));
        Images.draw(PREVIEW_IMAGE, previewX, previewY, previewWidth, previewHeight, -1);
        Rects.fill(previewX, previewY, previewWidth, previewHeight, new Color(8, 12, 20, 85));
        renderKeystrokesPreview(previewX + 24f, previewY + 24f);
        FPSMaster.fontManager.s14.drawString(key("oobe.scale.preview"), previewX + 18f, previewY + previewHeight - 20f, inverseMutedText().getRGB());
        FPSMaster.fontManager.s16.drawString(key("oobe.scale.tip"), previewX, previewY + previewHeight + 18f, mutedText().getRGB());

        handleScaleClicks(leftX, topY, dropdownY, mouseX, mouseY);
    }

    private void renderKeystrokesPreview(float x, float y) {
        float scale = 0.8f + fixedScaleIndex * 0.08f;
        float size = 30f * scale;
        float gap = 6f * scale;
        renderKey(x + size + gap, y, size, "W");
        renderKey(x, y + size + gap, size, "A");
        renderKey(x + size + gap, y + size + gap, size, "S");
        renderKey(x + (size + gap) * 2f, y + size + gap, size, "D");
    }

    private void renderKey(float x, float y, float size, String text) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(size), Math.round(size), 8, new Color(18, 24, 36, 220));
        FPSMaster.fontManager.s18.drawCenteredString(text, x + size / 2f, y + size / 2f - 4f, Color.WHITE.getRGB());
    }

    private void renderTutorialPage(int mouseX, int mouseY) {
        float x = leftColumnX();
        float y = pageTop();
        float cardWidth = compactLayout() ? clamp(frameWidth() - pageSidePadding() * 2f, 420f, 620f) : clamp(frameWidth() * 0.58f, 620f, 760f);
        float cardHeight = clamp(frameHeight() * 0.24f, 208f, 250f);
        FPSMaster.fontManager.s28.drawString(key("oobe.tutorial.title"), x, y, titleText().getRGB());
        FPSMaster.fontManager.s18.drawString(key("oobe.tutorial.desc"), x, y + 34f, bodyText().getRGB());

        String[][] slides = new String[][]{{key("oobe.tutorial.1.title"), key("oobe.tutorial.1.desc")}, {key("oobe.tutorial.2.title"), key("oobe.tutorial.2.desc")}, {key("oobe.tutorial.3.title"), key("oobe.tutorial.3.desc")}};

        drawCard(x, y + 74f, cardWidth, cardHeight, new Color(255, 255, 255, 238));
        FPSMaster.fontManager.s14.drawString((tutorialIndex + 1) + " / " + slides.length, x + 24f, y + 96f, mutedText().getRGB());
        FPSMaster.fontManager.s28.drawString(slides[tutorialIndex][0], x + 24f, y + 122f, panelTitleText().getRGB());
        FPSMaster.fontManager.s18.drawString(slides[tutorialIndex][1], x + 24f, y + 158f, mutedText().getRGB());
        float buttonY = y + 74f + cardHeight - 48f;
        tutorialPrevButton.setText(key("oobe.tutorial.prev")).renderInScreen(this, x + 24f, buttonY, 114f, 30f, mouseX, mouseY);
        tutorialNextButton.setText(key("oobe.tutorial.next")).renderInScreen(this, x + 150f, buttonY, 114f, 30f, mouseX, mouseY);
    }

    private void renderFeaturesPage(int mouseX, int mouseY) {
        float x = leftColumnX();
        float y = pageTop() - 12f;
        float gap = clamp(frameWidth() * 0.03f, 20f, 36f);
        boolean singleColumn = compactLayout() || frameWidth() < 980f;
        float cardWidth = singleColumn ? clamp(frameWidth() - pageSidePadding() * 2f, 420f, 760f) : (frameWidth() - pageSidePadding() * 2f - gap) / 2f;
        float rowGap = clamp(frameHeight() * 0.035f, 22f, 36f);
        FPSMaster.fontManager.s28.drawString(key("oobe.features.title"), x, y, titleText().getRGB());
        FPSMaster.fontManager.s18.drawString(key("oobe.features.desc"), x, y + 34f, bodyText().getRGB());

        String[][] cards = new String[][]{{key("oobe.features.performance.title"), key("oobe.features.performance.desc"), key("oobe.features.performance.detail")}, {key("oobe.features.animations.title"), key("oobe.features.animations.desc"), key("oobe.features.animations.detail")}, {key("oobe.features.hud.title"), key("oobe.features.hud.desc"), key("oobe.features.hud.detail")}, {key("oobe.features.background.title"), key("oobe.features.background.desc"), key("oobe.features.background.detail")}};

        for (int i = 0; i < cards.length; i++) {
            float cardX = singleColumn ? x : x + (i % 2) * (cardWidth + gap);
            float cardY = singleColumn ? y + 76f + i * (98f + rowGap) : y + 76f + (i / 2) * (112f + rowGap);
            float detailHeight = clamp(frameHeight() * 0.075f, 42f, 56f) * featureExpand[i];
            float collapsedHeight = 92f;
            drawCard(cardX, cardY, cardWidth, collapsedHeight + detailHeight, new Color(255, 255, 255, 236));
            FPSMaster.fontManager.s22.drawString(cards[i][0], cardX + 18f, cardY + 18f, panelTitleText().getRGB());
            FPSMaster.fontManager.s16.drawString(cards[i][1], cardX + 18f, cardY + 46f, mutedText().getRGB());
            if (featureExpand[i] > 0.02f) {
                int alpha = Math.min(255, Math.max(0, (int) (featureExpand[i] * 255f)));
                Rects.fill(cardX + 18f, cardY + 70f, cardWidth - 36f, 1f, new Color(27, 35, 48, Math.max(18, alpha / 5)));
                FPSMaster.fontManager.s14.drawString(cards[i][2], cardX + 18f, cardY + 80f, new Color(mutedText().getRed(), mutedText().getGreen(), mutedText().getBlue(), alpha).getRGB());
            }
            if (consumePressInBounds(cardX, cardY, cardWidth, collapsedHeight + detailHeight, 0) != null) {
                expandedFeature = expandedFeature == i ? -1 : i;
            }
        }
    }

    private void renderLoginPage(int mouseX, int mouseY) {
        float x = leftColumnX();
        float y = pageTop();
        FPSMaster.fontManager.s28.drawString(key("oobe.login.title"), x, y, titleText().getRGB());
        FPSMaster.fontManager.s18.drawString(key("oobe.login.desc"), x, y + 34f, bodyText().getRGB());

        drawTextField(accountField, x, y + 84f, 320f, 38f);
        drawTextField(passwordField, x, y + 136f, 320f, 38f);
        FPSMaster.fontManager.s16.drawString(key("oobe.login.forgot"), x, y + 192f, accentText().getRGB());
        FPSMaster.fontManager.s16.drawString(key("oobe.login.register"), x + 116f, y + 192f, accentText().getRGB());
        loginButton.setText(key("oobe.login.submit")).renderInScreen(this, x, y + 228f, 88f, 28f, mouseX, mouseY);
        skipLoginButton.setText(key("oobe.login.skip")).renderInScreen(this, x + 100f, y + 228f, 88f, 28f, mouseX, mouseY);
    }

    private void renderOptionsPage(int mouseX, int mouseY) {
        float leftX = leftColumnX();
        float topY = pageTop() - 12f;
        float leftWidth = compactLayout() ? clamp(frameWidth() * 0.44f, 240f, 320f) : clamp(frameWidth() * 0.34f, 300f, 360f);
        float leftHeight = clamp(frameHeight() * 0.56f, 300f, 420f);
        float rightX = compactLayout() ? leftX : leftX + leftWidth + clamp(frameWidth() * 0.04f, 28f, 44f);
        float rightTop = compactLayout() ? topY + leftHeight + 20f : topY;
        float rightWidth = compactLayout() ? clamp(frameWidth() - pageSidePadding() * 2f, 300f, 520f) : clamp(frameWidth() * 0.36f, 320f, 380f);
        drawCard(leftX, topY, leftWidth, leftHeight, new Color(40, 68, 162, 230));
        Rects.fill(leftX, topY, leftWidth, leftHeight, new Color(82, 109, 234, 170));
        FPSMaster.fontManager.s18.drawString(key("oobe.options.step"), leftX + 22f, topY + 26f, inverseMutedText().getRGB());
        FPSMaster.fontManager.s28.drawString(key("oobe.options.cover.title"), leftX + 22f, topY + 66f, inverseTitleText().getRGB());
        FPSMaster.fontManager.s18.drawString(key("oobe.options.cover.desc"), leftX + 22f, topY + 106f, inverseBodyText().getRGB());

        FPSMaster.fontManager.s28.drawString(key("oobe.options.title"), rightX, rightTop + 12f, titleText().getRGB());
        FPSMaster.fontManager.s18.drawString(key("oobe.options.desc"), rightX, rightTop + 46f, bodyText().getRGB());
        renderToggleCard(rightX, rightTop + 104f, rightWidth, key("oobe.options.anticheat"), antiCheatEnabled);
        renderToggleCard(rightX, rightTop + 168f, rightWidth, key("oobe.options.anonymous"), anonymousDataEnabled);

        if (consumePressInBounds(rightX, rightTop + 104f, rightWidth, 48f, 0) != null) {
            antiCheatEnabled = !antiCheatEnabled;
        }
        if (consumePressInBounds(rightX, rightTop + 168f, rightWidth, 48f, 0) != null) {
            anonymousDataEnabled = !anonymousDataEnabled;
        }
    }

    private void renderGuideEntryPage(int mouseX, int mouseY) {
        float x = leftColumnX();
        float y = pageTop();
        FPSMaster.fontManager.s28.drawString(key("oobe.guide.title"), x, y, titleText().getRGB());
        FPSMaster.fontManager.s18.drawString(key("oobe.guide.desc"), x, y + 34f, bodyText().getRGB());
        float choiceWidth = clamp(frameWidth() * 0.36f, 280f, 360f);
        renderChoiceCard(x, y + 96f, choiceWidth, enterGuide, key("oobe.guide.enter"));
        renderChoiceCard(x, y + 152f, choiceWidth, !enterGuide, key("oobe.guide.skip"));
        if (consumePressInBounds(x, y + 96f, choiceWidth, 42f, 0) != null) {
            enterGuide = true;
        }
        if (consumePressInBounds(x, y + 152f, choiceWidth, 42f, 0) != null) {
            enterGuide = false;
        }

        float sideWidth = clamp(frameWidth() * 0.2f, 190f, 220f);
        float sideX = frameRight() - sideWidth - pageSidePadding();
        drawCard(sideX, y + 86f, sideWidth, 84f, new Color(255, 255, 255, 238));
        FPSMaster.fontManager.s18.drawString(key("oobe.next"), sideX + 22f, y + 110f, panelTitleText().getRGB());
        FPSMaster.fontManager.s16.drawString(enterGuide ? key("oobe.guide.result.enter") : key("oobe.guide.result.skip"), sideX + 22f, y + 138f, accentText().getRGB());
    }

    private void renderQaPage(int mouseX, int mouseY) {
        float x = leftColumnX();
        float y = pageTop() - 4f;
        float cardWidth = clamp(frameWidth() * 0.6f, 640f, 760f);
        float resultWidth = clamp(frameWidth() * 0.2f, 190f, 220f);
        FPSMaster.fontManager.s28.drawString(key("oobe.qa.title"), x, y, titleText().getRGB());
        FPSMaster.fontManager.s18.drawString(key("oobe.qa.desc"), x, y + 34f, bodyText().getRGB());

        String[][] questions = new String[][]{{key("oobe.qa.1.question"), key("oobe.qa.1.a"), key("oobe.qa.1.b"), key("oobe.qa.1.c")}, {key("oobe.qa.2.question"), key("oobe.qa.2.a"), key("oobe.qa.2.b"), key("oobe.qa.2.c")}, {key("oobe.qa.3.question"), key("oobe.qa.3.a"), key("oobe.qa.3.b"), key("oobe.qa.3.c")}};

        drawCard(x, y + 76f, cardWidth, clamp(frameHeight() * 0.33f, 236f, 280f), new Color(255, 255, 255, 238));
        FPSMaster.fontManager.s14.drawString((qaStep + 1) + " / " + questions.length, x + 22f, y + 100f, mutedText().getRGB());
        FPSMaster.fontManager.s28.drawString(questions[qaStep][0], x + 22f, y + 132f, panelTitleText().getRGB());
        if (qaStep == 1) {
            renderBackgroundPreviewChoices(x + 22f, y + 178f, questions[qaStep], mouseX, mouseY);
        }
        for (int i = 1; i <= 3 && qaStep != 1; i++) {
            float optionY = y + 178f + (i - 1) * 42f;
            boolean selected = qaAnswers[qaStep] == i - 1;
            drawCard(x + 22f, optionY, cardWidth - 44f, 32f, selected ? new Color(122, 139, 255, 220) : new Color(247, 249, 255, 245));
            FPSMaster.fontManager.s16.drawString(questions[qaStep][i], x + 36f, optionY + 10f, selected ? Color.WHITE.getRGB() : panelTitleText().getRGB());
            if (consumePressInBounds(x + 22f, optionY, cardWidth - 44f, 32f, 0) != null) {
                qaAnswers[qaStep] = i - 1;
                applyQaAnswer();
                if (qaStep < questions.length - 1) {
                    qaStep++;
                }
            }
        }

        float resultX = frameRight() - resultWidth - pageSidePadding();
        drawCard(resultX, y + 86f, resultWidth, 110f, new Color(255, 255, 255, 238));
        FPSMaster.fontManager.s18.drawString(key("oobe.qa.result"), resultX + 22f, y + 110f, panelTitleText().getRGB());
        FPSMaster.fontManager.s16.drawString(key("oobe.background") + ": " + backgroundLabel(), resultX + 22f, y + 138f, mutedText().getRGB());
        FPSMaster.fontManager.s16.drawString(key("oobe.features.count") + ": " + getFeatureCountLabel(), resultX + 22f, y + 162f, mutedText().getRGB());
    }

    private void renderFooter(int mouseX, int mouseY) {
        float y = frameBottom() - 46f;
        backButton.setText(key("oobe.back")).setEnabled(page > 0).renderInScreen(this, frameLeft() + 16f, y, 84f, 30f, mouseX, mouseY);
        nextButton.setText(getNextLabel()).renderInScreen(this, frameRight() - 100f, y, 84f, 30f, mouseX, mouseY);
    }

    private void onNext() {
        if (page == 6 && !enterGuide) {
            finishOobe();
            return;
        }
        if (page >= PAGE_COUNT - 1) {
            finishOobe();
            return;
        }
        page++;
        pageMotion = 1f;
        pageMotionDirection = 1;
        scaleDropdownOpen = false;
    }

    private void finishOobe() {
        applySelections();
        mc.displayGuiScreen(new MainMenu());
    }

    private void applySelections() {
        ClientSettings.language.setValue(languageValue);
        ClientSettings.fixedScaleEnabled.setValue(fixedScaleEnabled);
        ClientSettings.fixedScale.setValue(fixedScaleIndex);

        FPSMaster.configManager.configure.background = backgroundChoice;
        FPSMaster.configManager.configure.antiCheatEnabled = antiCheatEnabled;
        FPSMaster.configManager.configure.anonymousDataEnabled = anonymousDataEnabled;
        FPSMaster.configManager.configure.oobeCompleted = true;

        applyDefaultModules();
        if (enterGuide) {
            applyQaModules();
        }

        try {
            FPSMaster.configManager.saveConfig("default");
        } catch (FileException ignored) {
        }
    }

    private void applyDefaultModules() {
        setModuleEnabled(Performance.class, true);
        setModuleEnabled(OldAnimations.class, true);
        setModuleEnabled(ItemPhysics.class, true);
        setModuleEnabled(FPSDisplay.class, true);
        setModuleEnabled(Keystrokes.class, true);
        setModuleEnabled(CPSDisplay.class, true);
        setModuleEnabled(ComboDisplay.class, false);
        setModuleEnabled(PingDisplay.class, false);
        setModuleEnabled(DirectionDisplay.class, false);
        setModuleEnabled(CoordsDisplay.class, false);
        setModuleEnabled(InventoryDisplay.class, false);
    }

    private void applyQaModules() {
        if (qaAnswers[0] == 2) {
            setModuleEnabled(FPSDisplay.class, false);
            setModuleEnabled(Keystrokes.class, false);
            setModuleEnabled(CPSDisplay.class, false);
            setModuleEnabled(ComboDisplay.class, false);
            setModuleEnabled(PingDisplay.class, false);
            setModuleEnabled(DirectionDisplay.class, true);
        }
        if (qaAnswers[1] == 0) {
            backgroundChoice = "classic";
        } else if (qaAnswers[1] == 1) {
            backgroundChoice = "shader";
        } else if (qaAnswers[1] == 2) {
            backgroundChoice = "panorama_3";
        }
        if (qaAnswers[2] == 0) {
            setModuleEnabled(FPSDisplay.class, true);
            setModuleEnabled(Keystrokes.class, true);
            setModuleEnabled(CPSDisplay.class, true);
            setModuleEnabled(ComboDisplay.class, true);
            setModuleEnabled(PingDisplay.class, false);
            setModuleEnabled(CoordsDisplay.class, false);
            setModuleEnabled(InventoryDisplay.class, false);
        } else if (qaAnswers[2] == 1) {
            setModuleEnabled(FPSDisplay.class, false);
            setModuleEnabled(Keystrokes.class, false);
            setModuleEnabled(CPSDisplay.class, false);
            setModuleEnabled(ComboDisplay.class, false);
            setModuleEnabled(PingDisplay.class, true);
            setModuleEnabled(CoordsDisplay.class, true);
            setModuleEnabled(InventoryDisplay.class, true);
        } else if (qaAnswers[2] == 2) {
            setModuleEnabled(FPSDisplay.class, false);
            setModuleEnabled(Keystrokes.class, false);
            setModuleEnabled(CPSDisplay.class, false);
            setModuleEnabled(ComboDisplay.class, false);
            setModuleEnabled(PingDisplay.class, false);
            setModuleEnabled(CoordsDisplay.class, false);
            setModuleEnabled(InventoryDisplay.class, false);
            setModuleEnabled(DirectionDisplay.class, false);
        }
    }

    private void setModuleEnabled(Class<?> type, boolean enabled) {
        Module module = FPSMaster.moduleManager.getModule(type);
        if (module != null) {
            module.set(enabled);
        }
    }

    private void applyQaAnswer() {
        if (qaStep == 0) {
            int answer = qaAnswers[0];
            if (answer == 0) {
                setFeatureCount(4);
            } else if (answer == 1) {
                setFeatureCount(5);
            } else if (answer == 2) {
                setFeatureCount(3);
            }
        }
        if (qaStep == 1) {
        if (qaAnswers[1] == 0) {
            backgroundChoice = "classic";
        } else if (qaAnswers[1] == 1) {
            backgroundChoice = "shader";
        } else if (qaAnswers[1] == 2) {
            backgroundChoice = "panorama_3";
        }
        }
    }

    private int featureCount = 5;

    private void setFeatureCount(int count) {
        featureCount = count;
    }

    private String getFeatureCountLabel() {
        return isChinese() ? featureCount + " " + key("oobe.features.count.unit") : String.valueOf(featureCount);
    }

    private String backgroundLabel() {
        if ("classic".equals(backgroundChoice)) {
            return key("oobe.background.classic");
        }
        if ("shader".equals(backgroundChoice)) {
            return key("oobe.background.shader");
        }
        if ("panorama_2".equals(backgroundChoice)) {
            return key("oobe.background.panorama2");
        }
        if ("panorama_3".equals(backgroundChoice)) {
            return key("oobe.background.panorama3");
        }
        return key("oobe.background.panorama1");
    }

    private String animatedGreeting() {
        int index = (int) ((System.currentTimeMillis() / 1400L) % GREETINGS.length);
        return GREETINGS[index];
    }

    private String getNextLabel() {
        if (page == PAGE_COUNT - 1 || (page == 6 && !enterGuide)) {
            return key("oobe.finish");
        }
        return key("oobe.next");
    }

    private void renderPill(float x, float y, float width, float height, boolean active, String label) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 16,
                active ? new Color(104, 117, 247, 230) : new Color(255, 255, 255, 220));
        FPSMaster.fontManager.s14.drawCenteredString(label, x + width / 2f, y + height / 2f - 3f,
                active ? Color.WHITE.getRGB() : new Color(42, 52, 78).getRGB());
    }

    private void renderFooterButton(float x, float y, float width, float height, String label, boolean muted) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 16,
                muted ? new Color(255, 255, 255, 200) : new Color(104, 117, 247, 235));
        FPSMaster.fontManager.s16.drawCenteredString(label, x + width / 2f, y + height / 2f - 4f,
                muted ? new Color(42, 52, 78).getRGB() : Color.WHITE.getRGB());
    }

    private void renderChoiceCard(float x, float y, float width, boolean selected, String label) {
        drawCard(x, y, width, 38f, selected ? new Color(122, 139, 255, 230) : new Color(255, 255, 255, 232));
        FPSMaster.fontManager.s14.drawString(label, x + 14f, y + 13f, selected ? Color.WHITE.getRGB() : new Color(42, 52, 78).getRGB());
    }

    private void renderToggleCard(float x, float y, float width, String label, boolean enabled) {
        drawCard(x, y, width, 42f, new Color(255, 255, 255, 232));
        FPSMaster.fontManager.s14.drawString(label, x + 14f, y + 14f, panelTitleText().getRGB());
        Rects.rounded(Math.round(x + width - 56f), Math.round(y + 11f), 34, 18, 9, enabled ? new Color(104, 117, 247, 235) : new Color(208, 214, 228, 255));
        Rects.rounded(Math.round(x + width - (enabled ? 41f : 55f)), Math.round(y + 13f), 14, 14, 7, Color.WHITE);
    }

    private void drawCard(float x, float y, float width, float height, Color color) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 18, color);
    }

    private void drawTextField(TextField field, float x, float y, float width, float height) {
        field.drawTextBox(x, y, width, height);
        PointerEvent outsideClick = peekAnyPress();
        if (outsideClick != null && !Hover.is(x, y, width, height, outsideClick.x, outsideClick.y)) {
            field.setFocused(false);
        }
        PointerEvent click = consumePressInBounds(x, y, width, height, 0);
        if (click != null) {
            field.mouseClicked(click.x, click.y, click.button);
        }
    }

    private void handleLanguageClicks(float x, float y) {
        if (consumePressInBounds(x, y, 116f, 34f, 0) != null) {
            languageValue = 1;
            setPreviewLanguage(languageValue);
            updateTextFieldPlaceholders();
        }
        if (consumePressInBounds(x + 128f, y, 116f, 34f, 0) != null) {
            languageValue = 0;
            setPreviewLanguage(languageValue);
            updateTextFieldPlaceholders();
        }
    }

    private void handleScaleClicks(float leftX, float topY, float dropdownY, int mouseX, int mouseY) {
        if (consumePressInBounds(leftX, topY + 86f, 156f, 34f, 0) != null) {
            fixedScaleEnabled = true;
        }
        if (consumePressInBounds(leftX + 168f, topY + 86f, 160f, 34f, 0) != null) {
            fixedScaleEnabled = false;
            scaleDropdownOpen = false;
        }
    }

    private void renderBackgroundPreviewChoices(float x, float y, String[] question, int mouseX, int mouseY) {
        String[] previewIds = new String[]{"classic", "shader", "panorama_3"};
        float width = clamp(frameWidth() * 0.6f, 640f, 760f) - 44f;
        for (int i = 0; i < 3; i++) {
            float optionY = y + i * 42f;
            drawCard(x, optionY, width, 32f, qaAnswers[qaStep] == i ? new Color(122, 139, 255, 220) : new Color(247, 249, 255, 245));
            renderMiniBackgroundPreview(x + 8f, optionY + 5f, 48f, 22f, previewIds[i]);
            FPSMaster.fontManager.s16.drawString(question[i + 1], x + 68f, optionY + 10f, qaAnswers[qaStep] == i ? Color.WHITE.getRGB() : new Color(42, 52, 78).getRGB());
            if (consumePressInBounds(x, optionY, width, 32f, 0) != null) {
                qaAnswers[qaStep] = i;
                applyQaAnswer();
                if (qaStep < 2) {
                    qaStep++;
                }
            }
        }
    }

    private void renderMiniBackgroundPreview(float x, float y, float width, float height, String id) {
        if ("classic".equals(id)) {
            Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 8, new Color(43, 50, 65).getRGB());
            return;
        }
        if ("shader".equals(id)) {
            Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 8, new Color(46, 71, 173).getRGB());
            Rects.fill(x, y, width, height, new Color(143, 160, 255, 72));
            return;
        }
        Images.draw(new ResourceLocation("client/background/panorama_3/panorama_0.png"), x, y, width, height, -1);
    }

    private void updateTextFieldPlaceholders() {
        accountField.placeHolder = key("oobe.login.account.placeholder");
        passwordField.placeHolder = key("oobe.login.password.placeholder");
    }

    private void setPreviewLanguage(int value) {
        try {
            FPSMaster.i18n.read(value == 1 ? "zh_cn" : "en_us");
        } catch (FileException ignored) {
        }
    }

    private String key(String key) {
        return FPSMaster.i18n.get(key);
    }

    private boolean isChinese() {
        return languageValue == 1;
    }

    private float frameLeft() {
        return 12f;
    }

    private float frameTop() {
        return 12f;
    }

    private float frameWidth() {
        return guiWidth - 24f;
    }

    private float frameHeight() {
        return guiHeight - 24f;
    }

    private float frameRight() {
        return frameLeft() + frameWidth();
    }

    private float frameBottom() {
        return frameTop() + frameHeight();
    }

    private float pageSidePadding() {
        return clamp(frameWidth() * 0.055f, 30f, 68f);
    }

    private float pageTop() {
        return frameTop() + clamp(frameHeight() * 0.095f, 60f, 92f);
    }

    private float leftColumnX() {
        return frameLeft() + pageSidePadding();
    }

    private boolean compactLayout() {
        return frameWidth() < 920f;
    }

    private Color titleText() {
        return new Color(27, 35, 48);
    }

    private Color bodyText() {
        return new Color(110, 119, 136);
    }

    private Color mutedText() {
        return new Color(110, 119, 136);
    }

    private Color panelTitleText() {
        return new Color(24, 32, 54);
    }

    private Color accentText() {
        return new Color(104, 117, 247);
    }

    private Color inverseTitleText() {
        return new Color(245, 248, 255);
    }

    private Color inverseBodyText() {
        return new Color(245, 248, 255, 215);
    }

    private Color inverseMutedText() {
        return new Color(245, 248, 255, 180);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            return;
        }
        accountField.textboxKeyTyped(typedChar, keyCode);
        passwordField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
