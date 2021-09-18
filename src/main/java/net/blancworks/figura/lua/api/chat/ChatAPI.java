package net.blancworks.figura.lua.api.chat;

import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.access.ChatHudAccess;
import net.blancworks.figura.access.ChatScreenAccess;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
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
import java.util.Objects;

public class ChatAPI {

    public static Identifier getID() {
        return new Identifier("default", "chat");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{

            set("sendMessage", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    //only send messages for local player
                    if (script.playerData == PlayerDataManager.localPlayer) {
                        ClientPlayerEntity player = MinecraftClient.getInstance().player;

                        if (player != null) {
                            //trim messages to mimic vanilla behaviour
                            String message = arg.tojstring().trim();
                            if (!message.isEmpty()) player.sendChatMessage(message);
                        }
                    }

                    return NIL;
                }
            });

            set("getMessage", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    //get message list
                    List<ChatHudLine<Text>> chat = ((ChatHudAccess) MinecraftClient.getInstance().inGameHud.getChatHud()).getMessages();

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
                        String message = ((ChatScreenAccess) chatScreen).getChatField().getText();
                        return message == null || Objects.equals(message, "") ? NIL : LuaString.valueOf(message);
                    }

                    return NIL;
                }
            });

            set("setFiguraCommandPrefix", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (arg.isnil()) {
                        script.commandPrefix = "\u0000";
                        return NIL;
                    }

                    script.commandPrefix = arg.checkjstring();
                    return NIL;
                }
            });

        }});
    }
}
