package net.blancworks.figura.network;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;
import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.network.messages.MessageRegistry;
import net.blancworks.figura.network.messages.avatar.AvatarUploadMessageSender;
import net.blancworks.figura.network.messages.pings.PingMessageSender;
import net.blancworks.figura.network.messages.pubsub.SubscribeToUsersMessageSender;
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
import org.luaj.vm2.LuaValue;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


public class NewFiguraNetworkManager implements IFiguraNetwork {

    public static CompletableFuture<?> networkTasks;

    //The protocol version for this version of the mod.
    public static final int PROTOCOL_VERSION = 0;

    private static boolean lastNetworkState = false;

    public static int connectionStatus = 0;

    //----- WEBSOCKETS -----

    //The factory that creates all sockets
    public static WebSocketFactory socketFactory;
    //The last socket we were using
    public static WebSocket currWebSocket;
    public static MessageRegistry msgRegistry;

    public static Object authWaitObject = new Object();
    public static ClientConnection authConnection;

    //The JWT token handed to us by the server.
    public static String jwtToken;

    //The time the JWT token was gotten at
    public static Date tokenReceivedTime;

    public static int tokenReauthCooldown = 0;

    //The time tokens last for. (default is 20 minutes)
    public static final int TOKEN_LIFETIME = 1000 * 60 * 20;
    //The time we wait to automatically re-auth once a token has expired. (default is 1 minute)
    public static final int TOKEN_REAUTH_WAIT_TIME = 200;

    //Timeout before a connection with a socket is considered dead.
    public static final int TIMEOUT_SECONDS = 1;

    private static boolean hasInited = false;

    private static final ArrayList<UUID> allSubscriptions = new ArrayList<>();
    private static final ArrayList<UUID> newSubscriptions = new ArrayList<>();

    private static CompletableFuture<?> doTask(Runnable toRun) {
        if (networkTasks == null || networkTasks.isDone()) {
            networkTasks = CompletableFuture.runAsync(toRun);
        } else {
            networkTasks.thenRun(toRun);
        }

        return networkTasks;
    }

    private static CompletableFuture<?> doTask(Supplier<CompletableFuture<?>> toRun) {
        if (networkTasks == null || networkTasks.isDone()) {
            networkTasks = toRun.get();
        } else {
            networkTasks.thenCompose(x -> toRun.get());
        }

        return networkTasks;
    }

    private static <T> CompletableFuture<?> doTaskSupply(Supplier<T> toRun) {
        if (networkTasks == null || networkTasks.isDone()) {
            networkTasks = CompletableFuture.supplyAsync(toRun);
        } else {
            CompletableFuture.supplyAsync(toRun);
        }

        return networkTasks;
    }

    public static void subscribe(UUID playerID) {
        if (!allSubscriptions.contains(playerID) && !newSubscriptions.contains(playerID))
            newSubscriptions.add(playerID);
    }

    @Override
    public void tickNetwork() {
        connectionStatus = 0;

        if ((boolean) Config.entries.get("useLocalServer").value != lastNetworkState && currWebSocket != null) {
            currWebSocket.disconnect();
            lastNetworkState = (boolean) Config.entries.get("useLocalServer").value;
        }

        if (authConnection != null && !authConnection.isOpen())
            authConnection.handleDisconnection();

        if (currWebSocket != null && currWebSocket.isOpen() && !msgRegistry.isEmpty()) {
            connectionStatus = 2;

            if (newSubscriptions.size() > 0) {
                allSubscriptions.addAll(newSubscriptions);

                UUID[] ids = new UUID[newSubscriptions.size()];
                newSubscriptions.toArray(ids);
                newSubscriptions.clear();

                doTask(() -> new SubscribeToUsersMessageSender(ids).sendMessage(currWebSocket));
            }
        } else if (allSubscriptions.size() > 0) {
            newSubscriptions.addAll(allSubscriptions);
            allSubscriptions.clear();
        }

        //If the old token we had is old enough, re-auth us.
        Date currTime = new Date();

        if (tokenReauthCooldown > 0)
            tokenReauthCooldown--;
        else if (tokenReceivedTime != null && currTime.getTime() - tokenReceivedTime.getTime() > TOKEN_LIFETIME) {
            tokenReauthCooldown = TOKEN_REAUTH_WAIT_TIME; //Wait
            connectionStatus = 1;

            //Auth user ASAP
            doTask(() -> authUser(true));
        }
    }

    @Override
    public CompletableFuture<?> getAvatarData(UUID id) {
        doTask(this::ensureConnection);
        return doTask(() -> {
            try {
                if (currWebSocket != null && currWebSocket.isOpen())
                    new UserGetCurrentAvatarMessageSender(id).sendMessage(NewFiguraNetworkManager.currWebSocket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<?> postAvatar() {
        doTask(this::ensureConnection);
        return doTask(() -> {
            if (currWebSocket != null && currWebSocket.isOpen()) {
                //Get NBT tag for local player avatar
                PlayerData data = PlayerDataManager.localPlayer;
                data.isLocalAvatar = false;
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
            }
        });
    }

    @Override
    public CompletableFuture<?> setCurrentUserAvatar(UUID avatarID) {
        return null;
    }

    @Override
    public CompletableFuture<?> deleteAvatar() {
        doTask(this::ensureConnection);
        return doTask(() -> {
            try {
                if (currWebSocket != null && currWebSocket.isOpen()) {
                    new UserDeleteCurrentAvatarMessageSender().sendMessage(currWebSocket);

                    PlayerDataManager.clearLocalPlayer();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<?> checkAvatarHash(UUID playerID, String lastHash) {
        doTask(this::ensureConnection);
        return doTask(() -> {
            try {
                if (currWebSocket != null && currWebSocket.isOpen())
                    new UserGetCurrentAvatarHashMessageSender(playerID).sendMessage(currWebSocket);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    public void sendPing(Queue<CustomScript.LuaPing> pings) {
        PingMessageSender pms = new PingMessageSender(pings);
        doTask(() -> {
            try {
                if (msgRegistry.isEmpty())
                    return;
                if (currWebSocket != null && currWebSocket.isOpen())
                    pms.sendMessage(currWebSocket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    //Minecraft authentication server URL
    public String authServerURL() {
        if ((boolean) Config.entries.get("useLocalServer").value)
            return "localhost";
        return "figuranew.blancworks.org";
    }

    //Main server for distributing files URL
    public String mainServerURL() {
        if ((boolean) Config.entries.get("useLocalServer").value)
            return "http://localhost:6050";
        return "https://figuranew.blancworks.org";
    }

    private static boolean localLastCheck = false;

    //Ensures there is a connection open with the server, if there was not before.
    public CompletableFuture<Void> ensureConnection() {

        if (localLastCheck != (boolean) Config.entries.get("useLocalServer").value || socketFactory == null) {
            localLastCheck = (boolean) Config.entries.get("useLocalServer").value;

            socketFactory = new WebSocketFactory();

            //Don't verify hostname for local servers.
            if (localLastCheck) {
                SSLContext ctx;
                try {
                    ctx = NaiveSSLContext.getInstance("TLS");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                socketFactory.setSSLContext(ctx);
                socketFactory.setVerifyHostname(false);
            }

            socketFactory.setServerName("figuranew.blancworks.org");
        }

        if (currWebSocket == null || !currWebSocket.isOpen()) {
            try {
                lastNetworkState = (boolean) Config.entries.get("useLocalServer").value;
                return openNewConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    //Opens a connection
    public CompletableFuture<Void> openNewConnection() {
        //Ensure user is authed, we need the JWT to verify this user.
        return authUser().thenCompose(unused -> {
            try {
                closeSocketConnection();
                String connectionString = String.format("%s/connect/", mainServerURL());

                FiguraMod.LOGGER.info("Connecting to websocket server " + connectionString);

                WebSocket newSocket = socketFactory.createSocket(connectionString, TIMEOUT_SECONDS * 1000);
                newSocket.setPingInterval(15 * 1000);
                currWebSocket = newSocket;
                msgRegistry = new MessageRegistry();
                FiguraNetworkMessageHandler messageHandler = new FiguraNetworkMessageHandler(this);
                newSocket.addListener(messageHandler);

                newSocket.connect();

                newSocket.sendText(jwtToken);

                messageHandler.sendClientRegistry(newSocket);

                return messageHandler.initializedFuture;
            } catch (Exception e) {
                e.printStackTrace();
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    private void closeSocketConnection() {
        if (currWebSocket == null)
            return;

        if (!currWebSocket.isOpen()) {
            currWebSocket = null;
            return;
        }

        currWebSocket.sendClose(0);
        currWebSocket = null;
    }

    public CompletableFuture<Void> authUser() {
        return authUser(false);
    }

    public CompletableFuture<Void> authUser(boolean force) {
        if (!force && jwtToken != null)
            return CompletableFuture.completedFuture(null);

        if (authConnection != null && !authConnection.isOpen()) {
            authConnection.handleDisconnection();

            if (authConnection != null)
                return CompletableFuture.completedFuture(null);
        }

        try {
            FiguraMod.LOGGER.info("Authenticating with Figura server");

            String address = authServerURL();
            InetAddress inetAddress = InetAddress.getByName(address);

            //Create new connection
            ClientConnection connection = ClientConnection.connect(inetAddress, 25565, true);

            CompletableFuture<Void> disconnectedFuture = new CompletableFuture<>();

            //Set listener/handler
            connection.setPacketListener(
                    new ClientLoginNetworkHandler(connection, MinecraftClient.getInstance(), null, (text) -> FiguraMod.LOGGER.info(text.getString())) {
                        //Handle disconnect message
                        @Override
                        public void onDisconnected(Text reason) {
                            try {
                                Text dcReason = connection.getDisconnectReason();

                                if (dcReason != null) {
                                    parseKickAuthMessage(dcReason);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            //Once connection is closed, yeet this connection so we can make new ones.
                            authConnection = null;

                            disconnectedFuture.complete(null);
                        }
                    });

            //Send packets.
            connection.send(new HandshakeC2SPacket(address, 25565, NetworkState.LOGIN));
            connection.send(new LoginHelloC2SPacket(MinecraftClient.getInstance().getSession().getProfile()));

            authConnection = connection;

            return disconnectedFuture;

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }
}
