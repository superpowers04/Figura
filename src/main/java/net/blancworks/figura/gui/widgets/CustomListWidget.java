package net.blancworks.figura.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec2f;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CustomListWidget<T extends Object, T2 extends CustomListEntry> extends AlwaysSelectedEntryListWidget<CustomListEntry> implements AutoCloseable {
    private final Screen parent;
    protected final Set<T> addedObjects = new HashSet<>();
    private List<T> objectList = null;
    private String selectedplayerId = null;
    private boolean scrolling;
    public CustomListWidgetState state;
    public TextFieldWidget searchBox;
    
    public boolean allowSelection = true;
    
    public CustomListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget list, Screen parent, CustomListWidgetState state) {
        super(client, width, height, y1, y2, entryHeight);
        this.state = state;
        this.parent = parent;
        if (list != null) {
            this.objectList = list.objectList;
        }
        this.searchBox = searchBox;
        if(searchBox != null)
            this.filter(searchBox.getText(), false);
        setScrollAmount(0 * Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)));
        
    }

    public int getItemHeight(){
        return itemHeight;
    }

    @Override
    public void setScrollAmount(double amount) {
        super.setScrollAmount(amount);
        int denominator = Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4));
        if (denominator <= 0) {
            state.scrollPercent = 0;
        } else {
            state.scrollPercent = getScrollAmount() / Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4));
        }
    }

    @Override
    protected boolean isFocused() {
        return parent.getFocused() == this;
    }

    public void unselect() {
        super.setSelected(null);
        state.selected = null;
        selectedplayerId = null;
    }

    public void select(T2 entry) {
        this.setSelected(entry);
    }

    @Override
    public void setSelected(@Nullable CustomListEntry entry) {
        super.setSelected(entry);
        state.selected = entry.getEntryObject();
        selectedplayerId = entry.getIdentifier();
    }

    @Override
    protected boolean isSelectedEntry(int index) {
        CustomListEntry selected = this.getSelectedOrNull();
        return selected != null && selected.getIdentifier().equals(getEntry(index).getIdentifier());
    }
    
    @Override
    public int addEntry(CustomListEntry entry) {
        if (addedObjects.contains(entry.getEntryObject())) {
            return 0;
        }
        addedObjects.add((T) entry.getEntryObject());
        int i = super.addEntry(entry);
        if (entry.getIdentifier().equals(selectedplayerId)) {
            setSelected(entry);
        }
        return i;
    }

    @Override
    protected boolean removeEntry(CustomListEntry entry) {
        addedObjects.remove(entry.getEntryObject());
        return super.removeEntry(entry);
    }

    @Override
    protected CustomListEntry remove(int index) {
        addedObjects.remove(getEntry(index).getEntryObject());
        return super.remove(index);
    }

    public void reloadFilters() {
        if(searchBox != null)
            filter(searchBox.getText(), true, false);
    }
    
    public void filter(String searchTerm, boolean refresh) {
        filter(searchTerm, refresh, true);
    }

    private void filter(String searchTerm, boolean refresh, boolean search) {
        this.clearEntries();
        addedObjects.clear();

        if (this.objectList == null || refresh) {
            this.objectList = new ArrayList<>();
        }
        objectList.clear();

        try {
            doFiltering(searchTerm);

            if (state.selected != null && !children().isEmpty() || this.getSelectedOrNull() != null && this.getSelectedOrNull().getEntryObject()!= state.selected) {
                for (CustomListEntry entry : children()) {
                    if (entry.getEntryObject().equals(state.selected)) {
                        setSelected(entry);
                    }
                }
            } else {
                if (this.getSelectedOrNull() == null && !children().isEmpty() && getEntry(0) != null) {
                    setSelected(getEntry(0));
                }
            }

            if (getScrollAmount() > Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4))) {
                setScrollAmount(Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    //Filter objects in the list, add new entries, that sort.
    //By default, filter by name.
    protected void doFiltering(String searchTerm){
        
    }


    @Override
    protected void renderList(MatrixStack matrices, int x, int y, int mouseX, int mouseY, float delta) {
        int itemCount = this.getEntryCount();
        Tessellator tessellator_1 = Tessellator.getInstance();
        BufferBuilder buffer = tessellator_1.getBuffer();

        for (int index = 0; index < itemCount; ++index) {
            int entryTop = this.getRowTop(index) + 2;
            int entryBottom = this.getRowTop(index) + this.itemHeight;
            if (entryBottom >= this.top && entryTop <= this.bottom) {
                int entryHeight = this.itemHeight - 4;
                CustomListEntry entry = this.getEntry(index);
                int rowWidth = this.getRowWidth();
                int entryLeft;
                if (this.isSelectedEntry(index)) {
                    entryLeft = getRowLeft() - 2 + entry.getXOffset();
                    int selectionRight = x + rowWidth + 2;
                    RenderSystem.disableTexture();
                    RenderSystem.setShader(GameRenderer::getPositionShader);
                    float float_2 = this.isFocused() ? 1.0F : 0.5F;
                    RenderSystem.setShaderColor(float_2, float_2, float_2, 1.0F);
                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
                    buffer.vertex(matrix, entryLeft, entryTop + entryHeight + 2, 0.0F).next();
                    buffer.vertex(matrix, selectionRight, entryTop + entryHeight + 2, 0.0F).next();
                    buffer.vertex(matrix, selectionRight, entryTop - 2, 0.0F).next();
                    buffer.vertex(matrix, entryLeft, entryTop - 2, 0.0F).next();
                    tessellator_1.draw();
                    RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
                    buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
                    buffer.vertex(matrix, entryLeft + 1, entryTop + entryHeight + 1, 0.0F).next();
                    buffer.vertex(matrix, selectionRight - 1, entryTop + entryHeight + 1, 0.0F).next();
                    buffer.vertex(matrix, selectionRight - 1, entryTop - 1, 0.0F).next();
                    buffer.vertex(matrix, entryLeft + 1, entryTop - 1, 0.0F).next();
                    tessellator_1.draw();
                    RenderSystem.enableTexture();
                }

                entryLeft = this.getRowLeft();
                entry.render(matrices, index, entryTop, entryLeft, rowWidth, entryHeight, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && Objects.equals(this.getEntryAtPos(mouseX, mouseY), entry), delta);
            }
        }

    }

    @Override
    protected void updateScrollingState(double double_1, double double_2, int int_1) {
        super.updateScrollingState(double_1, double_2, int_1);
        this.scrolling = int_1 == 0 &&
                double_1 >= (double) this.getScrollbarPositionX() &&
                double_1 < (double) (this.getScrollbarPositionX() + 6);
    }

    @Override
    public boolean mouseClicked(double double_1, double double_2, int int_1) {
        this.updateScrollingState(double_1, double_2, int_1);
        if (!this.isMouseOver(double_1, double_2)) {
            return false;
        } else {
            
            if(this.scrolling){
                setFocused(null);
                return true;
            }
            
            if(allowSelection) {
                CustomListEntry entry = this.getEntryAtPos(double_1, double_2);
                if (entry != null) {
                    if (entry.mouseClicked(double_1, double_2, int_1)) {
                        this.setFocused(entry);
                        this.setDragging(true);
                        return true;
                    }
                } else if (int_1 == 0) {
                    this.clickedHeader((int) (double_1 - (double) (this.left + this.width / 2 - this.getRowWidth() / 2)), (int) (double_2 - (double) this.top) + (int) this.getScrollAmount() - 4);
                    return true;
                }
            }

            return this.scrolling;
        }
    }

    public final CustomListEntry getEntryAtPos(double x, double y) {
        int int_5 = MathHelper.floor(y - (double) this.top) - this.headerHeight + (int) this.getScrollAmount() - 4;
        int index = int_5 / this.itemHeight;
        return (x >= (double) getRowLeft() &&
                x <= (double) (getRowLeft() + getRowWidth()) &&
                index >= 0 &&
                int_5 >= 0 &&
                index < this.getEntryCount()) ? this.children().get(index) : null;
    }
    
    public Vec2f getOffsetFromNearestEntry(double x, double y){
        int correctedY = MathHelper.floor(y - (double) this.top) - this.headerHeight + (int) this.getScrollAmount() - 4;
        int index = correctedY / this.itemHeight;
        
        float entryY = (float) (index * (this.itemHeight) + this.top + this.headerHeight + this.getScrollAmount() + 4);
        float entryX = this.getRowLeft();
        
        return new Vec2f((float)x - entryX, (float)y - entryY);
    }

    @Override
    protected int getScrollbarPositionX() {
        return this.left +  this.width - 6;
    }

    @Override
    public int getRowWidth() {
        return this.width - (Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)) > 0 ? 18 : 12);
    }

    @Override
    public int getRowLeft() {
        return left + 6;
    }

    public int getWidth() {
        return width;
    }

    public int getTop() {
        return this.top;
    }

    public Screen getParent() {
        return parent;
    }

    @Override
    protected int getMaxPosition() {
        return super.getMaxPosition() + 4;
    }

    public int getDisplayedCountFor(Set<String> set) {
        int count = 0;
        for (CustomListEntry c : children()) {
            if (set.contains(c.getIdentifier())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void close() throws Exception {}
}