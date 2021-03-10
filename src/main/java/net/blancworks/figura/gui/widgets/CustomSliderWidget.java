package net.blancworks.figura.gui.widgets;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public abstract class CustomSliderWidget extends SliderWidget {
    public CustomSliderWidget(int x, int y, int width, int height, Text text, double value) {
        super(x, y, width, height, text, value);
    }

    public void setValue(double val){
        this.value = val;
    }
    
    public void refreshMessage(){ this.updateMessage(); }
}
