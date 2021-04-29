package net.blancworks.figura.network;

import com.neovisionaries.ws.client.*;
import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.network.messages.avatar.AvatarUploadMessageSender;
import net.blancworks.figura.network.messages.user.UserDeleteCurrentAvatarMessageSender;
import net.blancworks.figura.network.messages.user.UserGetCurrentAvatarHashMessageSender;
import net.blancworks.figura.network.messages.user.UserGetCurrentAvatarMessageSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.text.Text;
import org.apache.logging.log4j.Level;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


public class NewFiguraNetworkManager implements IFiguraNetwork {

    public static CompletableFuture networkTasks;

    //The protocol version for this version of the mod.
    public static final int PROTOCOL_VERSION = 0;

    //----- WEBSOCKETS -----

    //The factory that creates all sockets
    public static WebSocketFactory socketFactory;
    //The last socket we were using
    public static WebSocket currWebSocket;

    //The JWT token handed to us by the server.
    public static String jwtToken;

    //The time the JWT token was gotten at
    public static Date tokenReceivedTime;
    
    //The time tokens last for. (default is 20 minutes)
    public static final int TOKEN_LIFETIME = 1000 * 60 * 20;
    //The time we wait to automatically re-auth once a token has expired. (default is 1 minute)
    public static final int TOKEN_REAUTH_WAIT_TIME = 1000 * 60;
    
    //Timeout before a connection with a socket is considered dead.
    public static final int TIMEOUT_SECONDS = 10;

    private static boolean hasInited = false;
    
    private static CompletableFuture doTask(Runnable toRun) {
        if (networkTasks == null || networkTasks.isDone()) {
            networkTasks = CompletableFuture.runAsync(toRun);
        } else {
            networkTasks.thenRun(toRun);
        }

        return networkTasks;
    }

    private static <T> CompletableFuture doTaskSupply(Supplier<T> toRun) {
        if (networkTasks == null || networkTasks.isDone()) {
            networkTasks = CompletableFuture.supplyAsync(toRun);
        } else {
            CompletableFuture.supplyAsync(toRun);
        }

        return networkTasks;
    }

    @Override
    public void tickNetwork() {
        //If the old token we had is old enough, re-auth us.
        Date currTime = new Date();
        if(tokenReceivedTime != null && currTime.getTime() - tokenReceivedTime.getTime() > TOKEN_LIFETIME){
            tokenReceivedTime = new Date(tokenReceivedTime.getTime() + TOKEN_REAUTH_WAIT_TIME);
            authUser(true);
        }
    }

    @Override
    public CompletableFuture getAvatarData(UUID id) {
        return doTask(()->{
            try {
                ensureConnection();

                new UserGetCurrentAvatarMessageSender(id).sendMessage(NewFiguraNetworkManager.currWebSocket);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        });
    }

    @Override
    public CompletableFuture<UUID> postAvatar() {
        return doTask(() -> {
            try {
                ensureConnection();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            //Get NBT tag for local player avatar
            PlayerData data = PlayerDataManager.localPlayer;
            CompoundTag infoNbt = new CompoundTag();
            data.writeNbt(infoNbt);

            try {
                //Set up streams.
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream nbtDataStream = new DataOutputStream(baos);

                NbtIo.writeCompressed(infoNbt, nbtDataStream);

                byte[] result = baos.toByteArray();

                new AvatarUploadMessageSender(result).sendMessage(currWebSocket);

                nbtDataStream.close();
                baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture setCurrentUserAvatar(UUID avatarID) {
        return null;
    }

    @Override
    public CompletableFuture deleteAvatar() {
        return doTask(() -> {
            try {
                ensureConnection();

                new UserDeleteCurrentAvatarMessageSender().sendMessage(currWebSocket);

                PlayerDataManager.clearLocalPlayer();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        });
    }

    @Override
    public CompletableFuture checkAvatarHash(UUID playerID, String lastHash) {
        return doTask(()->{
            new UserGetCurrentAvatarHashMessageSender(playerID).sendMessage(currWebSocket);
        });
    }

    @Override
    public void parseKickAuthMessage(Text reason) {
        if (reason.asString().equals("This is the Figura Auth Server V2.0!\n")) {

            Text tokenText = reason.getSiblings().get(1);

            jwtToken = tokenText.asString();
            tokenReceivedTime = new Date();
        }
    }

    @Override
    public void onClose() {
        if (currWebSocket != null && currWebSocket.isOpen()) {
            currWebSocket.sendClose();
            currWebSocket.disconnect();
        }
    }

    //Minecraft authentication server URL
    public String authServerURL() {
        if (Config.useLocalServer.value)
            return "localhost";
        return "figuranew.blancworks.org";
    }

    //Main server for distributing files URL
    public String mainServerURL() {
        if (Config.useLocalServer.value)
            return "localhost:6050";
        return "figuranew.blancworks.org";
    }

    private static boolean localLastCheck = false;

    //Ensures there is a connection open with the server, if there was not before.
    public void ensureConnection() throws Exception {

        if (localLastCheck != Config.useLocalServer.value || socketFactory == null) {
            localLastCheck = Config.useLocalServer.value;

            socketFactory = new WebSocketFactory();

            //Don't verify hostname for local servers.
            if (localLastCheck) {
                SSLContext ctx = NaiveSSLContext.getInstance("TLS");

                socketFactory.setSSLContext(ctx);
                socketFactory.setVerifyHostname(false);
            }
        }

        if (currWebSocket == null || currWebSocket.isOpen() == false) {
            currWebSocket = openNewConnection();
        }
    }

    //Opens a connection
    public WebSocket openNewConnection() throws Exception {

        //Ensure user is authed, we need the JWT to verify this user.
        authUser();

        closeSocketConnection();
        WebSocket newSocket = socketFactory.createSocket(String.format("ws://%s/connect/", mainServerURL()), TIMEOUT_SECONDS * 1000);
        newSocket.addListener(new FiguraNetworkMessageHandler(this));

        newSocket.connect();

        newSocket.sendText(jwtToken);

        newSocket.sendText(String.format("{\"protocol\":%d}", PROTOCOL_VERSION));

        return newSocket;
    }

    private void closeSocketConnection() {
        if (currWebSocket == null)
            return;

        if (currWebSocket.isOpen() == false) {
            currWebSocket = null;
            return;
        }

        currWebSocket.sendClose(0);
        currWebSocket = null;
    }

    public void authUser(){
        authUser(false);
    }
    
    public void authUser(boolean force) {

        if (!force && jwtToken != null)
            return;

        try {
            String address = authServerURL();
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

            if (dcReason != null) {
                Text tc = dcReason;
                parseKickAuthMessage(tc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
