package net.blancworks.figura.network;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
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

    private static CompletableFuture networkTasks;

    //----- WEBSOCKETS -----

    //The factory that creates all sockets
    private static WebSocketFactory socketFactory;
    //The last socket we were using
    private static WebSocket currWebSocket;

    //The JWT token handed to us by the server.
    private String jwtToken;

    //Timeout before a connection with a socket is considered dead.
    private static final int timeoutSeconds = 10;

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
            authUser();

            try {
                ensureConnection();
            } catch (Exception e){
                e.printStackTrace();
                return;
            }
            
            currWebSocket.sendText("From Minecraft, with love!");
            currWebSocket.sendBinary(new byte[28]);
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
        newSocket.addListener(new FiguraWebSocketAdapter());

        newSocket.connect();

        newSocket.sendText(jwtToken);

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

    private static class FiguraWebSocketAdapter extends WebSocketAdapter {

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            super.onConnected(websocket, headers);
        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
            super.onBinaryMessage(websocket, binary);
        }

        @Override
        public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onCloseFrame(websocket, frame);
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            currWebSocket = null;
        }

    }

    public void authUser() {
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
