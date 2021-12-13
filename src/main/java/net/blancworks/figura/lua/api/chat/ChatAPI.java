package net.blancworks.figura.lua.api.chat;

import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.mixin.ChatHudAccessorMixin;
import net.blancworks.figura.mixin.ChatScreenAccessorMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.List;

public class ChatAPI {

    public static Identifier getID() {
        return new Identifier("default", "chat");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{
            final boolean isHost = script.avatarData == AvatarDataManager.localPlayer;

            set("sendMessage", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    //only send messages for local player
                    if (!isHost) return NIL;

                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    if (player != null) {
                        //trim messages to mimic vanilla behaviour
                        String message = arg.tojstring().trim();
                        if (!message.isEmpty()) player.sendChatMessage(message);
                    }

                    return NIL;
                }
            });

            set("getMessage", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    //get message list
                    List<ChatHudLine<Text>> chat = ((ChatHudAccessorMixin) MinecraftClient.getInstance().inGameHud.getChatHud()).getMessages();

                    //access message
                    try {
                        //index - 1 to keep it within lua syntax
                        return LuaValue.valueOf(chat.get(arg.checkint() - 1).getText().getString());
                    } catch (Exception ignored) {
                        return NIL;
                    }
                }
            });

            set("getInputText", new ZeroArgFunction() {
                public LuaValue call() {
                    if (MinecraftClient.getInstance().currentScreen instanceof ChatScreen chatScreen) {
                        String message = ((ChatScreenAccessorMixin) chatScreen).getChatField().getText();
                        return message == null || message.equals("") ? NIL : LuaString.valueOf(message);
                    }

                    return NIL;
                }
            });

            set("setFiguraCommandPrefix", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.commandPrefix = arg.isnil() ? "\u0000" : arg.checkjstring();
                    return NIL;
                }
            });

            set("isOpen", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return isHost ? LuaValue.valueOf(MinecraftClient.getInstance().currentScreen instanceof ChatScreen) : FALSE;
                }
            });

        }});
    }
}
