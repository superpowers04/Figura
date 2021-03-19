package net.blancworks.figura.gui.widgets.permissions;

import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.settings.PermissionStringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class PermissionListStringEntry extends PermissionListEntry {

    public TextFieldWidget widget;

    public PermissionListStringEntry(PermissionStringSetting obj, CustomListWidget list) {
        super(obj, list);

        widget = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 0, 0, Text.of("NULL"));
        widget.setChangedListener((str)->{
            PermissionListWidget realList = (PermissionListWidget)list;
            obj.value = str;
            realList.setPermissionValue(obj);
        });
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);

        widget.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void tick(double mouseX, double mouseY) {
        super.tick(mouseX, mouseY);

        if(widget.isMouseOver(mouseX, mouseY))
            widget.tick();
    }
}
