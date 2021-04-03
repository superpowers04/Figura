package net.blancworks.figura.commands;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Util;
import org.apache.commons.io.FilenameUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
                                            FiguraCommands::executeLoadModel
                                    ).suggests(
                                            FiguraCommands::loadModelSuggestions
                                    )
                            )
                    ).then(
                            literal("save_model").then(
                                    argument("file", StringArgumentType.string()).executes(
                                            FiguraCommands::executeSaveModel
                                    )
                            )
                    ).then(
                            literal("load_model_nbt").then(
                                    argument("file", StringArgumentType.string()).executes(
                                            FiguraCommands::executeLoadModelNbt
                                    ).suggests(
                                            FiguraCommands::loadModelNBTSuggestions
                                    )
                            )
                    ).then(
                            literal("load_model_url").then(
                                    argument("url", StringArgumentType.string()).executes(
                                            FiguraCommands::executeLoadModelUrl
                                    )
                            )
                    ).then(
                            literal("post_model").executes(
                                    FiguraCommands::postModelCommand
                            )
                    ).then(
                            literal("clear_cache").executes(
                                    FiguraCommands::executeClearCache
                            )
                    )
            );
        });
    }

    public static int executeSaveModel(CommandContext ctx) {
        String fileName = (String) ctx.getArgument("file", String.class);

        PlayerData data = PlayerDataManager.localPlayer;

        CompoundTag infoTag = new CompoundTag();
        data.writeNbt(infoTag);

        Path outputPath = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").resolve(fileName + ".nbt");

        try {
            if (!Files.exists(outputPath)) {
                Files.createFile(outputPath);
            }

            FileOutputStream outputStream = new FileOutputStream(outputPath.toFile());
            NbtIo.writeCompressed(infoTag, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static int executeLoadModel(CommandContext ctx) {
        String fileName = (String) ctx.getArgument("file", String.class);
        PlayerDataManager.lastLoadedFileName = fileName;
        PlayerDataManager.localPlayer.loadModelFile(fileName);
        return 1;
    }

    public static int executeLoadModelNbt(CommandContext ctx) {
        String fileName = (String) ctx.getArgument("file", String.class);
        PlayerDataManager.localPlayer.loadModelFileNbt(fileName);
        return 1;
    }

    public static int executeLoadModelUrl(CommandContext ctx) {
        String url = (String) ctx.getArgument("url", String.class);

        url = url.replace('"', ' ');
        url = url.trim();

        String finalUrl = url;
        CompletableFuture.runAsync(() -> {
            HttpURLConnection httpURLConnection;

            try {
                httpURLConnection = (HttpURLConnection) (new URL(finalUrl)).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(false);
                httpURLConnection.connect();
                if (httpURLConnection.getResponseCode() / 100 == 2) {
                    DataInputStream stream = new DataInputStream(httpURLConnection.getInputStream());
                    PlayerDataManager.localPlayer.loadModelFileNbt(stream);
                }
                httpURLConnection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Util.getMainWorkerExecutor());

        return 1;
    }

    public static int postModelCommand(CommandContext ctx) {
        FiguraNetworkManager.postModel();
        return 1;
    }

    public static int executeClearCache(CommandContext ctx) {

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
