package net.blancworks.figura.network;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// Used to manage the network operations for Figura.
// Used for sending/receiving data, managing custom packets/networking, that sort.
public class FiguraNetworkManager {

    //This is the key for the session the user has with figura.
    //DO NOT LET PLAYERS ACCESS THIS!!!!!!
    //I mean, you could, but it just runs too many risks of them giving their key to someone who shouldn't have it.
    public static int figuraSessionKey;

    //False until the auth key has been acquired. also false if auth key becomes invalid.
    private static boolean hasAuthKey = false;
    private static Date lastAuthDate = null;

    public static CompletableFuture currentAuthTask = null;


    //Ticks the network
    //Checks after 20 minutes, attempt to get a fresh key with our previously valid one.
    //Repeat re-grab for fresh key 10 times, or until server returns unauthorized 
    public static void tickNetwork() {

        if (lastAuthDate != null) {
            Date now = new Date();
            long diff = now.getTime() - lastAuthDate.getTime();

            //If diff (time in milliseconds) > 20 minutes
            if (diff > 1000 * 60 * 20) {
                //Set the last time we checked for an auth to be 19 minutes ago
                //This means in 1 minute, we'll check again.
                lastAuthDate = new Date(now.getTime() - 1000 * 60 * 19);

                //Attempt to refresh key.
                refreshKeyValidity(() -> {
                });
            }
        }
    }


    //--AUTH--

    //Spawns async method for authenticating a user. 
    public static CompletableFuture authUser() {
        if (currentAuthTask != null)
            return currentAuthTask;

        return CompletableFuture.runAsync(
                FiguraNetworkManager::asyncAuthUser,
                Util.getMainWorkerExecutor()
        );
    }

    //Asynchronously authenticates the user using the Figura server.
    private static void asyncAuthUser() {

        try {
            String address = getMinecraftAuthServerAddress();
            InetAddress inetAddress = InetAddress.getByName(address);
            ClientConnection connection = ClientConnection.connect(inetAddress, 25565, true);
            connection.setPacketListener(new ClientLoginNetworkHandler(connection, MinecraftClient.getInstance(), null, (text) -> {
                FiguraMod.LOGGER.log(Level.ERROR, text.toString());
            }));
            connection.send(new HandshakeC2SPacket(address, 25565, NetworkState.LOGIN));
            connection.send(new LoginHelloC2SPacket(MinecraftClient.getInstance().getSession().getProfile()));

            while (connection.isOpen())
                Thread.sleep(1);

            Text dcReason = connection.getDisconnectReason();

            if (dcReason instanceof Text) {
                Text tc = (Text) dcReason;
                parseAuthKeyFromDisconnectMessage(tc);
            }
        } catch (Exception e) {
            FiguraMod.LOGGER.log(Level.ERROR, e);
        }

        //Auth all done :D
        currentAuthTask = null;
    }

    public static void parseAuthKeyFromDisconnectMessage(Text reason) {
        try {
            if (reason.asString().equals("This is the Figura Auth Server!\n")) {

                Text keyText = reason.getSiblings().get(1);
                figuraSessionKey = Integer.parseInt(keyText.asString());
                hasAuthKey = true;
                lastAuthDate = new Date();

                LiteralText garbleText = new LiteralText("-------------------------\n\n\n");
                garbleText.setStyle(Style.EMPTY.withFormatting(Formatting.OBFUSCATED));

                reason.getSiblings().set(1, garbleText);
            }
        } catch (Exception e) {
            FiguraMod.LOGGER.log(Level.ERROR, e.toString());
        }
    }

    public static boolean hasAuthKey() {
        return hasAuthKey;
    }

    //Attempts to refresh a key using the figura auth server.
    public static CompletableFuture refreshKeyValidity(Runnable onFinished) {
        return CompletableFuture.runAsync(
                () -> {
                    //Don't do anything if we know we have no valid key already.
                    if (!hasAuthKey) {
                        if (onFinished != null)
                            onFinished.run();
                        return;
                    }
                    try {
                        String uuidString = MinecraftClient.getInstance().player.getUuid().toString();
                        String url = String.format("%s/api/auth/refresh?key=%d&id=%s", getServerURL(), figuraSessionKey, uuidString);
                        HttpURLConnection connection = createReadConnection(url);
                        connection.connect();
                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            String getResult = readStringFromStream(connection.getInputStream());

                            int result = Integer.parseInt(getResult);
                            hasAuthKey = true;
                            lastAuthDate = new Date();
                            figuraSessionKey = result;
                        } else {
                            hasAuthKey = false;
                        }
                        connection.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (onFinished != null)
                        onFinished.run();
                }, Util.getMainWorkerExecutor()
        );
    }

    public CompletableFuture<Boolean> isKeyValid(Runnable onFinished) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        String uuidString = MinecraftClient.getInstance().player.getUuid().toString();
                        String url = String.format("%s/api/auth/check?key=%d&id=%s", getServerURL(), figuraSessionKey, uuidString);
                        HttpURLConnection connection = createReadConnection(url);
                        connection.connect();
                        connection.disconnect();
                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            return true;
                        } else {
                            return false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }, Util.getMainWorkerExecutor()
        );
    }

    //--AVATAR STATUS--

    public static void postModel() {
        String uuidString = MinecraftClient.getInstance().player.getUuid().toString();

        try {
            CompletableFuture.runAsync(() -> {
                HttpURLConnection httpURLConnection = null;

                refreshKeyValidity(null).join();

                //If not authed, auth.
                if (!hasAuthKey) {
                    CompletableFuture future = authUser();
                    future.join();
                }

                try {
                    URL url = new URL(String.format("%s/api/avatar/%s?key=%d", FiguraNetworkManager.getServerURL(), uuidString, figuraSessionKey));
                    PlayerData data = PlayerDataManager.localPlayer;

                    CompoundTag infoTag = new CompoundTag();
                    data.toNBT(infoTag);


                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("PUT");
                    httpURLConnection.setRequestProperty("Content-Type", "application/octet-stream");

                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    
                    OutputStream outStream = httpURLConnection.getOutputStream();
                    DataOutputStream nbtDataStream = new DataOutputStream(outStream);

                    NbtIo.writeCompressed(infoTag, nbtDataStream);

                    outStream.close();

                    FiguraMod.LOGGER.log(Level.DEBUG, httpURLConnection.getResponseMessage());
                } catch (Exception e) {
                    FiguraMod.LOGGER.log(Level.ERROR, e);
                }
            }, Util.getMainWorkerExecutor());
        } catch (Exception e) {
            FiguraMod.LOGGER.log(Level.ERROR, e);
        }
    }

    public static CompletableFuture<String> getAvatarHash(UUID uuid) {
        return CompletableFuture.supplyAsync(
                () -> {
                    return getAvatarHashSync(uuid);
                }, Util.getMainWorkerExecutor()
        );
    }

    private static String getAvatarHashSync(UUID id) {

        try {
            String uuidString = id.toString();
            HttpURLConnection httpURLConnection = createReadConnection(String.format("%s/api/avatar/hash/%s", FiguraNetworkManager.getServerURL(), uuidString));
            httpURLConnection.setConnectTimeout(100);
            httpURLConnection.connect();

            //Only continue if response was OK.
            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return readStringFromStream(httpURLConnection.getInputStream());
            }
            return "";
        } catch (Exception e) {
            //e.printStackTrace();
            return "";
        }
    }


    //Player data stuff
    //Right now just gets avatar, but will eventually get other stuff like report data.

    public static CompletableFuture<PlayerData> getPlayerData(UUID uuid) {
        return null;
    }

    private static PlayerData getData(UUID id) {

        return null;
    }


    //--UTILITY FUNCTIONS--


    //localhost for local testing
    //figura.blancworks.org for proper online use.
    //TODO - Add support for a server list later for people who want to have their own avatar servers
    private static String getServerAddress() {
        if(FabricLoader.getInstance().isDevelopmentEnvironment() && Files.exists(FabricLoader.getInstance().getConfigDir().resolve("figura").resolve("localnetwork.json"))){
            return "localhost:5001";
        }
        return "figura.blancworks.org";
    }
    private static String getMinecraftAuthServerAddress() {
        if(FabricLoader.getInstance().isDevelopmentEnvironment() && Files.exists(FabricLoader.getInstance().getConfigDir().resolve("figura").resolve("localnetwork.json"))){
            return "localhost";
        }
        return "mc.blancworks.org";
    }

    //This is set to 
    public static String getServerURL() {
        return String.format("http://%s", getServerAddress());
    }

    public static String readStringFromStream(InputStream stream) throws IOException {
        //Put JSON into string
        BufferedReader in = new BufferedReader(
                new InputStreamReader(stream));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return content.toString();
    }

    public static HttpURLConnection createConnection(String destination) throws Exception {
        URL dst = new URL(destination);
        HttpURLConnection httpURLConnection = (HttpURLConnection) dst.openConnection(MinecraftClient.getInstance().getNetworkProxy());
        return httpURLConnection;
    }

    public static HttpURLConnection createReadConnection(String destination) throws Exception {
        HttpURLConnection c = createConnection(destination);
        c.setDoInput(true);
        c.setDoOutput(false);
        return c;
    }

}
