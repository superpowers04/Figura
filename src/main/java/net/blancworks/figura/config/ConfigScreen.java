package net.blancworks.figura.config;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.config.ConfigListWidget.EntryType;
import net.blancworks.figura.config.ConfigManager.Config;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

public class ConfigScreen extends Screen {

    public Screen parentScreen;
    private ConfigListWidget configListWidget;

    public ConfigScreen(Screen parentScreen) {
        super(new TranslatableText("gui.figura.configtitle"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        this.addButton(new ButtonWidget(this.width / 2 - 154, this.height - 29, 150, 20, new TranslatableText("gui.cancel"), (buttonWidgetx) -> {
            ConfigManager.discardConfig();
            this.client.openScreen(parentScreen);
        }));

        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height - 29, 150, 20, new TranslatableText("gui.done"), (buttonWidgetx) -> {
            ConfigManager.applyConfig();
            ConfigManager.saveConfig();
            this.client.openScreen(parentScreen);
        }));

        this.configListWidget = new ConfigListWidget(this, this.client);
        this.children.add(this.configListWidget);

        //generate configs...
        generateConfig(configListWidget);
    }

    @Override
    public void onClose() {
        ConfigManager.applyConfig();
        ConfigManager.saveConfig();
        this.client.openScreen(parentScreen);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //background
        this.renderBackgroundTexture(0);

        //list
        this.configListWidget.render(matrices, mouseX, mouseY, delta);

        //buttons
        super.render(matrices, mouseX, mouseY, delta);

        //screen title
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 12, 16777215);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (configListWidget.focusedBinding != null) {
            configListWidget.focusedBinding.setBoundKey(InputUtil.Type.MOUSE.createFromCode(button));
            configListWidget.focusedBinding = null;

            KeyBinding.updateKeysByCode();

            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (configListWidget.focusedBinding != null) {
            configListWidget.focusedBinding.setBoundKey(keyCode == 256 ? InputUtil.UNKNOWN_KEY: InputUtil.fromKeyCode(keyCode, scanCode));
            configListWidget.focusedBinding = null;

            KeyBinding.updateKeysByCode();

            return true;
        }
        else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void generateConfig(ConfigListWidget configListWidget) {
        configListWidget.addEntry(EntryType.CATEGORY, new TranslatableText("gui.figura.config.nametag"));

        configListWidget.addEntry(EntryType.BOOLEAN, new TranslatableText("gui.figura.config.previewnametag"), new TranslatableText("gui.figura.config.tooltip.previewnametag"), Config.PREVIEW_NAMEPLATE);
        configListWidget.addEntry(EntryType.BOOLEAN, new TranslatableText("gui.figura.config.nametagmods"), new TranslatableText("gui.figura.config.tooltip.nametagmods"), Config.NAMEPLATE_MODIFICATIONS);
        configListWidget.addEntry(EntryType.BOOLEAN, new TranslatableText("gui.figura.config.chatmods"), new TranslatableText("gui.figura.config.tooltip.chatmods"), Config.CHAT_MODIFICATIONS);
        configListWidget.addEntry(EntryType.BOOLEAN, new TranslatableText("gui.figura.config.listmods"), new TranslatableText("gui.figura.config.tooltip.listmods"), Config.PLAYERLIST_MODIFICATIONS);
        configListWidget.addEntry(EntryType.BOOLEAN, new TranslatableText("gui.figura.config.showbadges"), new TranslatableText("gui.figura.config.tooltip.showbadges"), Config.BADGES);
        configListWidget.addEntry(EntryType.BOOLEAN,
                new TranslatableText("gui.figura.config.nametagicon"),
                new TranslatableText("gui.figura.config.tooltip.nametagicon", new LiteralText("△").setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT))),
                Config.BADGE_AS_ICONS
        );


        //category title
        configListWidget.addEntry(EntryType.CATEGORY, new TranslatableText("gui.figura.config.misc"));

        //entries
        List<Text> buttonLocationEntries = Arrays.asList(
                new TranslatableText("gui.figura.config.buttonlocation.topleft"),
                new TranslatableText("gui.figura.config.buttonlocation.topright"),
                new TranslatableText("gui.figura.config.buttonlocation.bottomleft"),
                new TranslatableText("gui.figura.config.buttonlocation.bottomright"),
                new TranslatableText("gui.figura.config.buttonlocation.icon")
        );
        configListWidget.addEntry(EntryType.ENUM, new TranslatableText("gui.figura.config.buttonlocation"), new TranslatableText("gui.figura.config.tooltip.buttonlocation"), Config.FIGURA_BUTTON_LOCATION, buttonLocationEntries);

        List<Text> scriptLogEntries = Arrays.asList(
                new TranslatableText("gui.figura.config.scriptlog.console_chat"),
                new TranslatableText("gui.figura.config.scriptlog.console"),
                new TranslatableText("gui.figura.config.scriptlog.chat")
        );
        configListWidget.addEntry(EntryType.ENUM, new TranslatableText("gui.figura.config.scriptlog"), new TranslatableText("gui.figura.config.tooltip.scriptlog"), Config.SCRIPT_LOG_LOCATION, scriptLogEntries);
        configListWidget.addEntry(EntryType.KEYBIND, new TranslatableText("key.figura.reloadavatar"), new TranslatableText("key.figura.tooltip.reloadavatar"), Config.RELOAD_AVATAR_BUTTON, FiguraMod.reloadAvatar);

        //category title
        configListWidget.addEntry(EntryType.CATEGORY, new TranslatableText("gui.figura.config.actionwheel"));

        //entries
        configListWidget.addEntry(EntryType.KEYBIND, new TranslatableText("key.figura.actionwheel"), new TranslatableText("key.figura.tooltip.actionwheel"), Config.ACTION_WHEEL_BUTTON, FiguraMod.actionWheel);

        List<Text> actionWheelEntries = Arrays.asList(
                new TranslatableText("gui.figura.config.actionwheelpos.mouse"),
                new TranslatableText("gui.figura.config.actionwheelpos.top"),
                new TranslatableText("gui.figura.config.actionwheelpos.bottom"),
                new TranslatableText("gui.figura.config.actionwheelpos.center")
        );
        configListWidget.addEntry(EntryType.ENUM, new TranslatableText("gui.figura.config.actionwheelpos"), new TranslatableText("gui.figura.config.tooltip.actionwheelpos"), Config.ACTION_WHEEL_TITLE_POS, actionWheelEntries);


        //category title
        configListWidget.addEntry(EntryType.CATEGORY, new TranslatableText("gui.figura.config.dev").formatted(Formatting.RED));

        //entries
        //this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.usenewnetwork"), new TranslatableText("gui.figura.config.tooltip.usenewnetwork"), Config.entries.get("useNewNetwork")));
        configListWidget.addEntry(EntryType.BOOLEAN, new TranslatableText("gui.figura.config.uselocalserver"), new TranslatableText("gui.figura.config.tooltip.uselocalserver"), Config.USE_LOCAL_SERVER);
        configListWidget.addEntry(EntryType.BOOLEAN, new TranslatableText("gui.figura.config.formatscript"), new TranslatableText("gui.figura.config.tooltip.formatscript"), Config.FORMAT_SCRIPT_ON_UPLOAD);
        configListWidget.addEntry(EntryType.BOOLEAN, new TranslatableText("gui.figura.config.logothers"), new TranslatableText("gui.figura.config.tooltip.logothers"), Config.LOG_OTHERS_SCRIPT);
        configListWidget.addEntry(EntryType.BOOLEAN,
                new TranslatableText("gui.figura.config.partshitbox"),
                new TranslatableText("gui.figura.config.tooltip.partshitbox", new TranslatableText("gui.figura.config.tooltip.partshitbox.cubes").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xff72b7))), new TranslatableText("gui.figura.config.tooltip.partshitbox.groups").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xaff2ff)))),
                Config.RENDER_DEBUG_PARTS_PIVOT
        );
        configListWidget.addEntry(EntryType.BOOLEAN, new TranslatableText("gui.figura.config.ownnametag"), new TranslatableText("gui.figura.config.tooltip.ownnametag"), Config.RENDER_OWN_NAMEPLATE);
        configListWidget.addEntry(EntryType.INPUT, new TranslatableText("gui.figura.config.path"), new TranslatableText("gui.figura.config.tooltip.path"), Config.MODEL_FOLDER_PATH, ConfigListWidget.FOLDER_PATH);
    }
}