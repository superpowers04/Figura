package net.blancworks.figura.gui.widgets.permissions;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.gui.widget.ToggleButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class PermissionListToggleEntry extends PermissionListEntry{
    public ToggleButtonWidget widget;

    public PermissionListToggleEntry(TrustContainer.Trust trust, CustomListWidget<?, ?> list, TrustContainer container) {
        super(trust, list, container);

        matchingElement = widget = new ToggleButtonWidget(0, 0, 16 , 16, container.getTrust(trust) == 1) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                container.setTrust(trust, (container.getTrust(trust) + 1) % 2);
                toggled = container.getTrust(trust) == 1;
            }

            @Override
            public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, this.texture);
                RenderSystem.disableDepthTest();

                int i = this.u;
                int j = this.v;
                if (this.toggled) {
                    i += this.pressedUOffset;
                }

                if (this.isHovered()) {
                    j += this.hoverVOffset;
                }

                drawTexture(matrices, this.x, this.y, i, j, this.width, this.height, 64, 32);
                RenderSystem.enableDepthTest();
            }
        };
        
        widget.setTextureUV(0, 0, 16, 16, new Identifier("figura", "textures/gui/togglebox.png"));

        if(((PermissionListWidget) list).getCurrentContainer().locked)
            widget.active = false;
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);
        
        widget.x = x + 2 + (rowWidth / 2);
        widget.y = y + 2;
        if(!widget.active)
            widget.setTextureUV(32, 0, 16, 16, new Identifier("figura", "textures/gui/togglebox.png"));
        
        widget.render(matrices, mouseX, mouseY, delta);
    }

}
