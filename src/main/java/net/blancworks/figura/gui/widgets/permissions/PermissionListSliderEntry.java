package net.blancworks.figura.gui.widgets.permissions;

import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.CustomSliderWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class PermissionListSliderEntry extends PermissionListEntry {

    public CustomSliderWidget widget;

    public PermissionListSliderEntry(TrustContainer.Trust trust, CustomListWidget<?, ?> list, TrustContainer container) {
        super(trust, list, container);

        widget = new CustomSliderWidget(0, 0, 0, 20, Text.of(trust.getValueText(container.getTrust(trust))), MathHelper.getLerpProgress(container.getTrust(trust), trust.min, trust.max)) {
            @Override
            public void updateMessage() {
                setMessage(Text.of(trust.getValueText(container.getTrust(trust))));
            }

            @Override
            public void applyValue() {
                float val = MathHelper.lerp((float) value, trust.min, trust.max);
                if (trust.step > 0) val = (float) (Math.floor(val / trust.step) * trust.step);

                val = MathHelper.clamp(val, trust.min, trust.max);
                if (val >= trust.max) val = Integer.MAX_VALUE - 100;

                container.setTrust(trust, (int) val);
                updateMessage();
            }
        };

        matchingElement = widget;
        if (((PermissionListWidget) list).getCurrentContainer().locked)
            widget.active = false;
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);
        
        matrices.push();
        widget.setWidth(Math.min((rowWidth / 2) - 2, 128));
        widget.x = x + 2 + (rowWidth / 2);
        widget.y = y;
        widget.render(matrices, mouseX, mouseY, delta);
        matrices.pop();
    }
}
