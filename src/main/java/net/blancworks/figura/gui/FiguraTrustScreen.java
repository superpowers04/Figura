package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.gui.widgets.CustomListEntry;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.CustomListWidgetState;
import net.blancworks.figura.gui.widgets.PlayerListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class FiguraTrustScreen extends Screen {

    public Screen parentScreen;

    private TextFieldWidget searchBox;
    private PlayerListWidget playerList;
    private Text tooltip;
    private boolean init = false;
    private boolean filterOptionsShown = false;
    private int paneY;
    private int paneWidth;
    private int rightPaneX;
    private int searchBoxX;
    private int filtersX;
    private int filtersWidth;
    private int searchRowWidth;
    public final Set<String> showModChildren = new HashSet<>();
    
    public CustomListWidgetState playerListState = new CustomListWidgetState();
    public CustomListWidgetState permissionListState = new CustomListWidgetState();

    protected FiguraTrustScreen(Screen parentScreen) {
        super(new LiteralText("Figura Trust Menu"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        
        paneY = 48;
        paneWidth = this.width / 2 - 8;
        rightPaneX = width - paneWidth;

        this.addButton(new ButtonWidget(this.width - 64 - 5, this.height - 20 - 5, 64, 20, new LiteralText("Back"), (buttonWidgetx) -> {
            this.client.openScreen((Screen) parentScreen);
        }));

        int searchBoxWidth = paneWidth - 32 - 22;
        searchBoxX = paneWidth / 2 - searchBoxWidth / 2 - 22 / 2;
        this.searchBox = new TextFieldWidget(this.textRenderer, searchBoxX, 22, searchBoxWidth, 20, this.searchBox, new TranslatableText("modmenu.search"));
        this.searchBox.setChangedListener((string_1) -> this.playerList.filter(string_1, false));
        this.playerList = new PlayerListWidget(this.client, paneWidth, this.height, paneY + 19, this.height - 36, 14, this.searchBox, this.playerList, this, playerListState);
        this.playerList.setLeftPos(0);
        playerList.reloadFilters();

        this.addChild(this.playerList);
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        IntegratedServer server = MinecraftClient.getInstance().getServer();

        int y = 0;

        this.playerList.render(matrices, mouseX, mouseY, delta);
        this.searchBox.render(matrices, mouseX, mouseY, delta);

    }

    @Override
    public void renderBackground(MatrixStack matrices) {
        overlayBackground(0, 0, this.width, this.height, 64, 64, 64, 255, 255);
    }

    static void overlayBackground(int x1, int y1, int x2, int y2, int red, int green, int blue, int startAlpha, int endAlpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Objects.requireNonNull(MinecraftClient.getInstance()).getTextureManager().bindTexture(DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        buffer.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(x1, y2, 0.0D).texture(x1 / 32.0F, y2 / 32.0F).color(red, green, blue, endAlpha).next();
        buffer.vertex(x2, y2, 0.0D).texture(x2 / 32.0F, y2 / 32.0F).color(red, green, blue, endAlpha).next();
        buffer.vertex(x2, y1, 0.0D).texture(x2 / 32.0F, y1 / 32.0F).color(red, green, blue, startAlpha).next();
        buffer.vertex(x1, y1, 0.0D).texture(x1 / 32.0F, y1 / 32.0F).color(red, green, blue, startAlpha).next();
        tessellator.draw();
    }

    public String getSearchInput() {
        return searchBox.getText();
    }

    private boolean updateFiltersX() {
        if ((filtersWidth + textRenderer.getWidth(computeModCountText(true)) + 20) >= searchRowWidth && ((filtersWidth + textRenderer.getWidth(computeModCountText(false)) + 20) >= searchRowWidth || (filtersWidth + 150 + 20) >= searchRowWidth)) {
            filtersX = paneWidth / 2 - filtersWidth / 2;
            return !filterOptionsShown;
        } else {
            filtersX = searchRowWidth - filtersWidth + 1;
            return true;
        }
    }

    private Text computeModCountText(boolean includeLibs) {
        return new LiteralText("DUMMY");
    }

    @Override
    public boolean keyPressed(int int_1, int int_2, int int_3) {
        return super.keyPressed(int_1, int_2, int_3) || this.searchBox.keyPressed(int_1, int_2, int_3);
    }

    @Override
    public boolean charTyped(char char_1, int int_1) {
        return this.searchBox.charTyped(char_1, int_1);
    }

    @Override
    public void tick() {
        this.searchBox.tick();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (playerList.isMouseOver(mouseX, mouseY)) {
            return this.playerList.mouseScrolled(mouseX, mouseY, amount);
        }
        return false;
    }
}
