package net.blancworks.figura.lua.api.nameplate;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.access.FiguraTextAccess;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class NamePlateAPI {
    public static final String ENTITY = "ENTITY";
    public static final String CHAT = "CHAT";
    public static final String TABLIST = "LIST";

    public static Identifier getID() {
        return new Identifier("default", "nameplate");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set(ENTITY, getTableForPart(ENTITY, script));
            set(CHAT, getTableForPart(CHAT, script));
            set(TABLIST, getTableForPart(TABLIST, script));
        }};
    }

    public static LuaTable getTableForPart(String accessor, CustomScript targetScript) {
        return new LuaTable() {{
            set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeNameplateCustomization(accessor).position);
                }
            });

            set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeNameplateCustomization(accessor).position = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Boolean enabled = targetScript.getOrMakeNameplateCustomization(accessor).enabled;
                    return enabled == null ? NIL : LuaValue.valueOf(enabled);
                }
            });

            set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetScript.getOrMakeNameplateCustomization(accessor).enabled = arg.isnil() ? null : arg.checkboolean();
                    return NIL;
                }
            });

            set("getScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeNameplateCustomization(accessor).scale);
                }
            });

            set("setScale", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeNameplateCustomization(accessor).scale = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            set("setText", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String string = null;

                    if (!arg.isnil()) {
                        //no ✭ 4 u
                        string = TextUtils.noBadges4U(arg.checkjstring());

                        //allow new lines only on entity
                        if (!accessor.equals(ENTITY))
                            string = string.replaceAll("[\n\r]", " ");

                        //check if nameplate is too large
                        if (string.length() > 65535) {
                            throw new LuaError("Nameplate too long - oopsie!");
                        }
                    }

                    targetScript.getOrMakeNameplateCustomization(accessor).text = string;
                    return NIL;
                }
            });

            set("getText", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    String text = targetScript.getOrMakeNameplateCustomization(accessor).text;
                    return text == null ? NIL : LuaValue.valueOf(text);
                }
            });
        }};
    }

    public static boolean applyFormattingRecursive(LiteralText text, String playerName, NamePlateCustomization nameplateData, AvatarData currentData) {
        //save siblings
        ArrayList<Text> siblings = new ArrayList<>(text.getSiblings());

        //transform already transformed text
        if (((FiguraTextAccess) text).figura$getFigura()) {
            //transform the text
            Text transformed = applyNameplateFormatting(text, nameplateData, currentData);

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
            Text transformed = applyNameplateFormatting(playerNameSplitted, nameplateData, currentData);

            //return the text
            if (!textSplit[0].equals("")) {
                //set pre text
                ((FiguraTextAccess) text).figura$setText(textSplit[0]);

                //set style
                text.setStyle(style);

                //append new text
                text.append(transformed);
            } else {
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
        } else {
            //then iterate through children
            for (Text sibling : siblings) {
                //split args when translatable text
                if (sibling instanceof TranslatableText) {
                    Object[] args = ((TranslatableText) sibling).getArgs();

                    for (Object arg : args) {
                        if (arg instanceof TranslatableText || !(arg instanceof Text))
                            continue;

                        if (NamePlateAPI.applyFormattingRecursive((LiteralText) arg, playerName, nameplateData, currentData)) {
                            return true;
                        }
                    }
                }
                //else check and format literal text
                else if (sibling instanceof LiteralText && applyFormattingRecursive((LiteralText) sibling, playerName, nameplateData, currentData)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Text applyNameplateFormatting(Text text, NamePlateCustomization nameplateData, AvatarData currentData) {
        //dummy playername text
        MutableText formattedText = new LiteralText(((LiteralText) text).getRawString());

        //original style
        Style originalStyle = text.getStyle();
        formattedText.setStyle(originalStyle);

        //mark text as figura text
        ((FiguraTextAccess) formattedText).figura$setFigura(true);

        if (currentData != null) {
            //apply nameplate formatting
            if (nameplateData != null && nameplateData.text != null && currentData.getTrustContainer().getTrust(TrustContainer.Trust.NAMEPLATE_EDIT) == 1) {
                Text jsonText = TextUtils.tryParseJson(nameplateData.text);
                TextUtils.removeClickableObjects((MutableText) jsonText);

                ((FiguraTextAccess) formattedText).figura$setText("");
                formattedText.append(jsonText);
            }
        }

        //add badges
        Text badgesText = getBadges(currentData);

        //append badges
        if ((boolean) Config.BADGES.value && badgesText != null)
            formattedText.append(badgesText);

        return formattedText;
    }

    private static final String LOADING = "\u22EE\u22F0\u22EF\u22F1";
    public static Text getBadges(AvatarData currentData) {
        if (currentData == null) return null;

        //font
        Identifier font = (boolean) Config.BADGE_AS_ICONS.value ? FiguraMod.FIGURA_FONT : Style.DEFAULT_FONT_ID;
        String badges = " ";

        if (currentData.hasAvatar()) {
            //trust
            TrustContainer tc = currentData.getTrustContainer();
            CustomModel model = currentData.model;
            if ((currentData.getComplexity() > tc.getTrust(TrustContainer.Trust.COMPLEXITY)) ||
                    (model != null && (model.animRendered > model.animMaxRender || (!model.animations.isEmpty() && model.animMaxRender == 0)))) {
                currentData.trustIssues = true;
            } else if (currentData.script != null) {
                CustomScript script = currentData.script;
                currentData.trustIssues = (script.customVCP != null && script.customVCP.hasLayers() && tc.getTrust(TrustContainer.Trust.CUSTOM_RENDER_LAYER) == 0) ||
                        (!script.nameplateCustomizations.isEmpty() && tc.getTrust(TrustContainer.Trust.NAMEPLATE_EDIT) == 0) ||
                        (!script.allCustomizations.isEmpty() && tc.getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 0) ||
                        (!script.customSounds.isEmpty() && tc.getTrust(TrustContainer.Trust.CUSTOM_SOUNDS) == 0);
            } else {
                currentData.trustIssues = false;
            }

            //the mark
            if (!currentData.isAvatarLoaded()) {
                if ((boolean) Config.BADGE_AS_ICONS.value)
                    badges += Integer.toHexString(Math.abs(FiguraMod.ticksElapsed) % 16);
                else
                    badges += LOADING.charAt(Math.abs(FiguraMod.ticksElapsed) % 4);
            }
            else if (currentData.script != null && currentData.script.scriptError)
                badges += "▲";
            else if (currentData.trustIssues)
                badges += "!";
            else if (FiguraMod.IS_CHEESE)
                badges += "\uD83E\uDDC0";
            else
                badges += "△";
        }

        //special badges
        if (FiguraMod.VIP.contains(currentData.entityId))
            badges += "✭";

        //return null if no badges
        if (badges.equals(" ")) return null;

        //create badges text
        LiteralText badgesText = new LiteralText(badges);

        //set formatting
        badgesText.setStyle(Style.EMPTY.withExclusiveFormatting(Formatting.WHITE).withFont(font));

        //flag as figura text
        ((FiguraTextAccess) badgesText).figura$setFigura(true);

        return badgesText;
    }
}
