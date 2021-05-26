package net.blancworks.figura.lua.api.nameplate;

import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.access.FiguraTextAccess;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.lua.api.math.VectorAPI;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

public class NamePlateAPI {
    public static final String ENTITY = "ENTITY";
    public static final String CHAT = "CHAT";
    public static final String TABLIST = "LIST";

    public static Identifier getID() {
        return new Identifier("default", "nameplate");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        ScriptLocalAPITable producedTable = new ScriptLocalAPITable(script, new LuaTable() {{
            set(ENTITY, getTableForPart(ENTITY, script));
            set(CHAT, getTableForPart(CHAT, script));
            set(TABLIST, getTableForPart(TABLIST, script));
        }});

        return producedTable;
    }

    public static ReadOnlyLuaTable getTableForPart(String accessor, CustomScript script) {
        NamePlateAPI.NamePlateTable producedTable = new NamePlateAPI.NamePlateTable(accessor, script);
        return producedTable;
    }

    private static class NamePlateTable extends ScriptLocalAPITable {
        String accessor;

        public NamePlateTable(String accessor, CustomScript script) {
            super(script);
            this.accessor = accessor;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();
            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeNameplateCustomization(accessor).position);
                }
            });

            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeNameplateCustomization(accessor).position = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetScript.getOrMakeNameplateCustomization(accessor).enabled);
                }
            });

            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    NamePlateCustomization customization = targetScript.getOrMakeNameplateCustomization(accessor);

                    if (arg.isnil()) {
                        customization.enabled = false;
                        return NIL;
                    }

                    customization.enabled = arg.checkboolean();
                    return NIL;
                }
            });

            ret.set("getScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeNameplateCustomization(accessor).scale);
                }
            });

            ret.set("setScale", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeNameplateCustomization(accessor).scale = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("setText", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    NamePlateCustomization customization = targetScript.getOrMakeNameplateCustomization(accessor);

                    if (arg.isnil()) {
                        customization.text = null;
                        return NIL;
                    }

                    customization.text = arg.checkjstring().replaceAll("[\n\r]", "");

                    return NIL;
                }
            });

            ret.set("getText", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetScript.getOrMakeNameplateCustomization(accessor).text);
                }
            });

            ret.set("setColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    NamePlateCustomization customization = targetScript.getOrMakeNameplateCustomization(accessor);

                    if (arg.isnil()) {
                        customization.color = null;
                        return NIL;
                    }

                    Vector3f color = LuaVector.checkOrNew(arg).asV3f();
                    customization.color = ((Math.round(color.getX() * 255) & 0xFF) << 16) | ((Math.round(color.getY() * 255) & 0xFF) << 8) | (Math.round(color.getZ() * 255) & 0xFF);
                    return NIL;
                }
            });

            ret.set("getColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return VectorAPI.RGBfromInt(targetScript.getOrMakeNameplateCustomization(accessor).color);
                }
            });

            ret.set("setFormatting", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    NamePlateCustomization customization = targetScript.getOrMakeNameplateCustomization(accessor);

                    if (arg.isnil()) {
                        customization.bold = null;
                        customization.italic = null;
                        customization.obfuscated = null;
                        customization.strikethrough = null;
                        customization.underline = null;
                        return NIL;
                    }

                    LuaTable formatting = arg.checktable();

                    ArrayList<String> arguments = new ArrayList<>();

                    for (int i = 1; i <= formatting.length(); i++) {
                        arguments.add(formatting.get(i).checkjstring());
                    }

                    if (arguments.contains("BOLD"))
                        customization.bold = true;
                    if (arguments.contains("ITALIC"))
                        customization.italic = true;
                    if (arguments.contains("OBFUSCATED"))
                        customization.obfuscated = true;
                    if (arguments.contains("STRIKETHROUGH"))
                        customization.strikethrough = true;
                    if (arguments.contains("UNDERLINE"))
                        customization.underline = true;

                    return NIL;
                }
            });

            ret.set("getFormatting", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    NamePlateCustomization customization = targetScript.getOrMakeNameplateCustomization(accessor);

                    LuaTable formatting = new LuaTable();

                    if (customization.bold != null)
                        formatting.insert(0, LuaValue.valueOf("BOLD"));
                    if (customization.italic != null)
                        formatting.insert(0, LuaValue.valueOf("ITALIC"));
                    if (customization.obfuscated != null)
                        formatting.insert(0, LuaValue.valueOf("OBFUSCATED"));
                    if (customization.strikethrough != null)
                        formatting.insert(0, LuaValue.valueOf("STRIKETHROUGH"));
                    if (customization.underline != null)
                        formatting.insert(0, LuaValue.valueOf("UNDERLINE"));

                    return formatting;
                }
            });

            return ret;
        }
    }

    public static boolean applyFormattingRecursive(LiteralText text, UUID uuid, String playerName, NamePlateCustomization nameplateData, PlayerData currentData) {
        //save siblings
        ArrayList<Text> siblings = new ArrayList<>(text.getSiblings());

        //transform already transformed text
        if (((FiguraTextAccess) text).figura$getFigura()) {
            //transform the text
            Text transformed = applyNameplateFormatting(text, uuid, nameplateData, currentData);

            //set text as transformed
            ((FiguraTextAccess) text).figura$setText(((LiteralText) transformed).getRawString());

            //flag as figura text
            ((FiguraTextAccess) text).figura$setFigura(true);

            //set text style
            text.setStyle(transformed.getStyle());

            //add sibling texts
            transformed.getSiblings().forEach(((LiteralText) text)::append);

            return true;
        }
        //otherwise transform when playername
        else if (text.getRawString().contains(playerName)) {
            //save original style
            Style style = text.getStyle();

            //split the text
            String[] textSplit = text.getRawString().split(Pattern.quote(playerName), 2);

            Text playerNameSplitted = new LiteralText(playerName).setStyle(style);

            //transform the text
            Text transformed = applyNameplateFormatting(playerNameSplitted, uuid, nameplateData, currentData);

            //return the text
            if (!textSplit[0].equals("")) {
                //set pre text
                ((FiguraTextAccess) text).figura$setText(textSplit[0]);

                //set style
                text.setStyle(style);

                //append new text
                text.append(transformed);
            }
            else {
                //set text as transformed
                ((FiguraTextAccess) text).figura$setText(((LiteralText) transformed).getRawString());

                //flag as figura text
                ((FiguraTextAccess) text).figura$setFigura(true);

                //set text style
                text.setStyle(transformed.getStyle());

                //add sibling texts
                transformed.getSiblings().forEach(((LiteralText) text)::append);
            }

            //add post text
            if (textSplit.length > 1 && !textSplit[1].equals("")) {
                text.append(textSplit[1]).setStyle(style);
            }

            //append siblings back if not from figura
            for (Text sibling : siblings) {
                if (!((FiguraTextAccess) sibling).figura$getFigura())
                    text.append(sibling);
            }

            return true;
        }
        else {
            //then iterate through children
            for (Text sibling : siblings) {
                if (applyFormattingRecursive((LiteralText) sibling, uuid, playerName, nameplateData, currentData))
                    return true;
            }
        }

        return false;
    }

    public static Text applyNameplateFormatting(Text text, UUID uuid, NamePlateCustomization nameplateData, PlayerData currentData) {
        //dummy playername text
        LiteralText formattedText = new LiteralText(((LiteralText) text).getRawString());
        formattedText.setStyle(text.getStyle());

        //mark text as figura text
        ((FiguraTextAccess) formattedText).figura$setFigura(true);

        if (currentData != null) {
            //apply nameplate formatting
            if (nameplateData != null && currentData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_NAMEPLATE_MOD_ID)) {
                //dummy style
                Style style = Style.EMPTY;

                //set color and properties
                if (nameplateData.color != null)
                    style = style.withColor(TextColor.fromRgb(nameplateData.color));

                if (nameplateData.bold != null)
                    style = style.withBold(nameplateData.bold);

                if (nameplateData.italic != null)
                    style = style.withItalic(nameplateData.italic);

                if (nameplateData.underline != null)
                    style = style.withUnderline(nameplateData.underline);

                if (nameplateData.strikethrough != null && nameplateData.strikethrough)
                    style = style.withFormatting(Formatting.STRIKETHROUGH);

                if (nameplateData.obfuscated != null && nameplateData.obfuscated)
                    style = style.withFormatting(Formatting.OBFUSCATED);

                //set extra styles
                Style originalStyle = text.getStyle();
                style = style.withClickEvent(originalStyle.getClickEvent());
                style = style.withHoverEvent(originalStyle.getHoverEvent());
                style = style.withInsertion(originalStyle.getInsertion());
                style = style.withFont(originalStyle.getFont());

                //set text, if not null
                if (nameplateData.text != null)
                    ((FiguraTextAccess) formattedText).figura$setText(nameplateData.text);

                //apply new style
                formattedText.setStyle(style);
            }
        }

        //add badges
        //font
        Identifier font = (boolean) Config.entries.get("nameTagIcon").value ? FiguraMod.FIGURA_FONT : Style.DEFAULT_FONT_ID;
        String badges = " ";

        //the mark
        if (currentData != null && currentData.model != null)
            badges += PlayerDataManager.getDataForPlayer(uuid).model.getRenderComplexity() < currentData.getTrustContainer().getFloatSetting(PlayerTrustManager.MAX_COMPLEXITY_ID) ? "△" : "▲";

        //special badges
        if (FiguraMod.special.contains(uuid))
            badges += "✭";

        //append badges
        if (!badges.equals(" ")) {
            //create badges text
            LiteralText badgesText = new LiteralText(badges);

            //set formatting
            badgesText.setStyle(Style.EMPTY.withExclusiveFormatting(Formatting.WHITE).withFont(font));

            //flag as figura text
            ((FiguraTextAccess) badgesText).figura$setFigura(true);

            //append
            formattedText.append(badgesText);
        }

        return formattedText;
    }
}
