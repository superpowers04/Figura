package net.blancworks.figura.network;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.network.messages.DebugMessageHandler;
import net.blancworks.figura.network.messages.DebugMessageSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.text.Text;
import org.apache.logging.log4j.Level;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
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

    //Timeout before a connection with a socket is considered dead.
    public static final int timeoutSeconds = 10;

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
    }

    @Override
    public CompletableFuture<CompoundTag> getAvatarData(UUID id) {
        return null;
    }

    @Override
    public CompletableFuture<UUID> postAvatar() {
        return doTask(() -> {
            try {
                ensureConnection();
            } catch (Exception e){
                e.printStackTrace();
                return;
            }

            DebugMessageSender sender = new DebugMessageSender();
            
            sender.sendMessage(currWebSocket);
        });
    }

    @Override
    public CompletableFuture setAvatarCurr(UUID avatarID) {
        return null;
    }

    @Override
    public CompletableFuture deleteAvatar() {
        return null;
    }

    @Override
    public CompletableFuture<String> asyncGetAvatarHash(UUID avatarID) {
        return null;
    }

    @Override
    public void parseKickAuthMessage(Text reason) {
        if (reason.asString().equals("This is the Figura Auth Server V2.0!\n")) {

            Text tokenText = reason.getSiblings().get(1);

            jwtToken = tokenText.asString();
        }
    }

    @Override
    public void onClose() {
        if(currWebSocket != null && currWebSocket.isOpen()) {
            currWebSocket.sendClose();
            currWebSocket.disconnect();
        }
    }

    //Minecraft authentication server URL
    public String authServerURL() {
        if (Config.useLocalServer.value)
            return "localhost";
        return "";
    }

    //Main server for distributing files URL
    public String mainServerURL() {
        if (Config.useLocalServer.value)
            return "localhost:6001";
        return "";
    }

    public void setupSocketFactory(WebSocketFactory factory) {
        if (!Config.useLocalServer.value) {

        } else {

        }
    }
    
    private static boolean localLastCheck = false;
    
    //Ensures there is a connection open with the server, if there was not before.
    public void ensureConnection() throws Exception {
        
        if(localLastCheck != Config.useLocalServer.value){
            localLastCheck = Config.useLocalServer.value;

            socketFactory = new WebSocketFactory();

            //Don't verify hostname for local servers.
            if(localLastCheck){
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
        WebSocket newSocket = socketFactory.createSocket(String.format("wss://%s/connect", mainServerURL()), timeoutSeconds * 1000);
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

    public void authUser() {
        
        if(jwtToken != null)
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

            if (dcReason instanceof Text) {
                Text tc = dcReason;
                parseKickAuthMessage(tc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
