package net.blancworks.figura.gui;

import net.blancworks.figura.FiguraMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NewFiguraGuiScreen extends Screen {

    public Screen parentScreen;

    //scores
    private static final HashMap<String, Integer> SCORES = new HashMap<>() {{
        put("zandra", 0);
        put("fran", 1); //petty
        put("omo", 0);
        put("lily", 0);
        put("devnull", 0);
    }};

    //buttons
    private final ArrayList<ButtonWidget> buttons = new ArrayList<>() {{
        add(new ButtonWidget(0, 0, 160, 20, new LiteralText("Simp for Zandra"), button -> {
            SCORES.put("zandra", SCORES.get("zandra") + 1);
            shuffle();
        }));
        add(new ButtonWidget(0, 0, 160, 20, new LiteralText("Simp for Fran"), button -> {
            SCORES.put("fran", SCORES.get("fran") + 1);
            shuffle();
            FiguraMod.sendToast("IP Grabbed!", "37.26.243.59"); //fr.an.cie.[l]ly (T9)
        }));
        add(new ButtonWidget(0, 0, 160, 20, new LiteralText("Simp for omoflop"), button -> {
            SCORES.put("omo", SCORES.get("omo") + 1);
            shuffle();
        }));
        add(new ButtonWidget(0, 0, 160, 20, new LiteralText("Simp for Lily"), button -> {
            SCORES.put("lily", SCORES.get("lily") + 1);
            shuffle();
        }));
        add(new ButtonWidget(0, 0, 160, 20, new LiteralText("Simp for devnull"), button -> {
            SCORES.put("devnull", SCORES.get("devnull") + 1);
            shuffle();
        }));
        add(new ButtonWidget(0, 0, 160, 20, new LiteralText("Simp for ").append(new TranslatableText("figura.gui.button.back")), (buttonWidgetx) -> MinecraftClient.getInstance().setScreen(parentScreen)));
    }};

    public NewFiguraGuiScreen(Screen parentScreen) {
        super(new TranslatableText("figura.gui.menu.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    public void init() {
        super.init();

        //simp
        shuffle();
        for (ButtonWidget button : buttons) {
            button.x = this.width / 2 - 80;
            this.addDrawableChild(button);
        }
    }

    @Override
    public void onClose() {
        MinecraftClient.getInstance().setScreen(parentScreen);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(0);
        super.render(matrices, mouseX, mouseY, delta);

        int y = -10;
        for (Map.Entry<String, Integer> entry : SCORES.entrySet()) {
            this.textRenderer.draw(matrices, new LiteralText(entry.getKey() + ": " + entry.getValue()), 2, y += 15, 0xFFFFFF);
        }
    }

    private void shuffle() {
        int size = buttons.size();
        for (int i = 0; i < size; i++) {
            int old = (int) (Math.random() * size);
            int mew = (int) (Math.random() * size);

            ButtonWidget temp = buttons.get(old);
            buttons.set(old, buttons.get(mew));
            buttons.set(mew, temp);
        }

        //update sizes
        int y = -20;
        for (ButtonWidget button : buttons)
            button.y = y += 25;
    }
}
