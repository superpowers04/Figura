package net.blancworks.figura.models.shaders;

import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.io.IOException;

public class FiguraShader extends Shader {

    public FiguraShader(ResourceFactory factory, String name, VertexFormat format) throws IOException {
        super(factory, name, format);
    }

    /**
     * Attempts to set the value of the specified uniform to the specified value.
     * @param name The name of the targeted uniform
     * @param value The value to be passed to the uniform
     * @throws LuaError When the arguments are not valid
     */
    public void setUniformFromLua(LuaValue name, LuaValue value) throws LuaError {
        GlUniform uniform = getUniform(name.checkjstring());
        if (uniform == null) {
            throw new LuaError("No uniform with name " + name.checkjstring() + " exists!");
        } else {
            try {
                value.checknotnil();
                switch (uniform.getDataType()) {
                    case 0 -> { //int
                        uniform.set(value.checkint());
                    }
                    case 4,5,6,7 -> { //float
                        if (value.isnumber()) {
                            uniform.set(value.tofloat());
                        } else {
                            LuaVector v = LuaVector.checkOrNew(value);
                            uniform.setForDataType(v.x(), v.y(), v.z(), v.w());
                        }

                    }
                    case 8 -> { //2x2 matrix
                        value.checktable();
                        uniform.set(value.get(1).tofloat(), value.get(2).tofloat(),
                                value.get(3).tofloat(), value.get(4).tofloat());
                    }
                    case 9 -> { //3x3 matrix
                        value.checktable();
                        uniform.set(value.get(1).tofloat(), value.get(2).tofloat(), value.get(3).tofloat(),
                                value.get(4).tofloat(), value.get(5).tofloat(), value.get(6).tofloat(),
                                value.get(7).tofloat(), value.get(8).tofloat(), value.get(9).tofloat());
                    }
                    case 10 -> { //4x4 matrix
                        value.checktable();
                        uniform.set(value.get(1).tofloat(), value.get(2).tofloat(), value.get(3).tofloat(), value.get(4).tofloat(),
                                value.get(5).tofloat(), value.get(6).tofloat(), value.get(7).tofloat(), value.get(8).tofloat(),
                                value.get(9).tofloat(), value.get(10).tofloat(), value.get(11).tofloat(), value.get(12).tofloat(),
                                value.get(13).tofloat(), value.get(14).tofloat(), value.get(15).tofloat(), value.get(16).tofloat());
                    }
                }
            } catch (LuaError err) {
                throw new LuaError("Invalid arguments for setUniform(). Value should either be a number or a table of numbers, depending on the uniform type.");
            }
        }
    }

}
