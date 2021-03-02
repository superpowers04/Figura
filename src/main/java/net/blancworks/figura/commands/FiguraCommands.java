package net.blancworks.figura.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.network.FiguraNetworkManager;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.NbtCompoundTagArgumentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Util;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FiguraCommands {

    public static void initialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {

            //Load model
            dispatcher.register(
                    literal("figura").then(
                            literal("load_model").then(
                                    argument("file", StringArgumentType.string()).executes(
                                            FiguraCommands::load_model_command
                                    ).suggests(
                                            FiguraCommands::loadModelSuggestions
                                    )
                            )
                    ).then(
                            literal("save_model").then(
                                    argument("file", StringArgumentType.string()).executes(
                                            FiguraCommands::save_model_command
                                    )
                            )
                    ).then(
                            literal("load_model_nbt").then(
                                    argument("file", StringArgumentType.string()).executes(
                                            FiguraCommands::load_model_nbt_command
                                    ).suggests(
                                            FiguraCommands::loadModelNBTSuggestions
                                    )
                            )
                    ).then(
                            literal("load_model_url").then(
                                    argument("url", StringArgumentType.string()).executes(
                                            FiguraCommands::load_model_url_command
                                    )
                            )
                    ).then(
                            literal("post_model").executes(
                                    FiguraCommands::post_model_command
                            )
                    ).then(
                            literal("clear_cache").executes(
                                    FiguraCommands::clear_cache_command
                            )
                    )
            );


        });

    }

    public static int save_model_command(CommandContext ctx) {
        String fileName = (String) ctx.getArgument("file", String.class);

        PlayerData data = PlayerDataManager.localPlayer;

        CompoundTag infoTag = new CompoundTag();
        data.toNBT(infoTag);

        Path outputPath = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").resolve(fileName + ".nbt");

        try {
            if (!Files.exists(outputPath)) {
                Files.createFile(outputPath);
            }

            FileOutputStream outputStream = new FileOutputStream(outputPath.toFile());
            infoTag.write(new DataOutputStream(outputStream));
        } catch (Exception e) {
            System.out.println(e);
        }
        return 1;
    }

    public static int load_model_command(CommandContext ctx) {
        String fileName = (String) ctx.getArgument("file", String.class);
        PlayerDataManager.localPlayer.loadModelFile(fileName);
        return 1;
    }

    public static int load_model_nbt_command(CommandContext ctx) {
        String fileName = (String) ctx.getArgument("file", String.class);
        PlayerDataManager.localPlayer.loadModelFileNBT(fileName);
        return 1;
    }

    public static int load_model_url_command(CommandContext ctx) {
        String url = (String) ctx.getArgument("url", String.class);

        url = url.replace('"', ' ');
        url = url.trim();

        String finalUrl = url;
        CompletableFuture.runAsync(() -> {
            HttpURLConnection httpURLConnection = null;

            try {
                httpURLConnection = (HttpURLConnection) (new URL(finalUrl)).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(false);
                httpURLConnection.connect();
                if (httpURLConnection.getResponseCode() / 100 == 2) {
                    DataInputStream stream = new DataInputStream(httpURLConnection.getInputStream());
                    PlayerDataManager.localPlayer.loadModelFileNBT(stream);
                }
                httpURLConnection.disconnect();
            } catch (Exception e) {
                System.out.println(e);
            }
        }, Util.getMainWorkerExecutor());

        return 1;
    }

    public static int post_model_command(CommandContext ctx) {
        FiguraNetworkManager.postModel();
        return 1;
    }

    public static int clear_cache_command(CommandContext ctx) {

        System.out.println("CLEAR");
        PlayerDataManager.clearCache();

        return 1;
    }


    public static CompletableFuture<Suggestions> loadModelSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        File contentDirectory = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").toFile();

        ArrayList<String> valid_loads = new ArrayList<String>();

        File[] files = contentDirectory.listFiles();

        for (File file : files) {
            String fileName = FilenameUtils.removeExtension(file.getName());

            if (Files.exists(contentDirectory.toPath().resolve(fileName + ".bbmodel")) && Files.exists(contentDirectory.toPath().resolve(fileName + ".png"))) {
                if (valid_loads.contains(fileName))
                    continue;
                valid_loads.add(fileName);
            }
        }


        for (String fileName : valid_loads) {
            builder.suggest(fileName);
        }

        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> loadModelNBTSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        File contentDirectory = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").toFile();

        ArrayList<String> valid_loads = new ArrayList<String>();

        for (File file : contentDirectory.listFiles()) {
            String fileName = file.getName();

            if (fileName.endsWith(".nbt")) {
                if (valid_loads.contains(fileName))
                    continue;
                valid_loads.add(fileName);
            }
        }


        for (String fileName : valid_loads) {
            builder.suggest(fileName);
        }

        return builder.buildFuture();
    }

}
