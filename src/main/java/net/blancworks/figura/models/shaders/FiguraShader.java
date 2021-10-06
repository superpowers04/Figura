package net.blancworks.figura.models.shaders;

import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import org.luaj.vm2.LuaValue;

import java.io.IOException;

public class FiguraShader extends Shader {

    public FiguraShader(ResourceFactory factory, String name, VertexFormat format) throws IOException {
        super(factory, name, format);
    }

    public void setUniformFromLua(LuaValue name, LuaValue value) {
        try {
            GlUniform uniform = getUniform(name.checkjstring());
            switch (uniform.getDataType()) {
                case 0 -> { //int
                    uniform.set(value.checkint());
                }
                case 4 -> { //float
                    uniform.set(value.tofloat());
                }
                case 8 -> { //2x2 matrix
                    if (value.istable())
                        uniform.set(value.get(1).tofloat(), value.get(2).tofloat(),
                                value.get(3).tofloat(), value.get(4).tofloat());
                }
                case 9 -> { //3x3 matrix
                    if (value.istable())
                        uniform.set(value.get(1).tofloat(), value.get(2).tofloat(), value.get(3).tofloat(),
                                value.get(4).tofloat(), value.get(5).tofloat(), value.get(6).tofloat(),
                                value.get(7).tofloat(), value.get(8).tofloat(), value.get(9).tofloat());
                }
                case 10 -> { //4x4 matrix
                    if (value.istable())
                        uniform.set(value.get(1).tofloat(), value.get(2).tofloat(), value.get(3).tofloat(), value.get(4).tofloat(),
                                value.get(5).tofloat(), value.get(6).tofloat(), value.get(7).tofloat(), value.get(8).tofloat(),
                                value.get(9).tofloat(), value.get(10).tofloat(), value.get(11).tofloat(), value.get(12).tofloat(),
                                value.get(13).tofloat(), value.get(14).tofloat(), value.get(15).tofloat(), value.get(16).tofloat());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
