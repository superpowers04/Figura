package net.blancworks.figura.mixin;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessorMixin {

    @Accessor("KEYS_BY_ID")
    static Map<String, KeyBinding> getKeysById() {
        throw new AssertionError();
    }
}
