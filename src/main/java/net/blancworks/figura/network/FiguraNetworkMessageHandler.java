package net.blancworks.figura.network;

import com.google.common.io.ByteSource;
import com.google.common.io.LittleEndianDataInputStream;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFrame;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.network.messages.MessageIDs;
import net.blancworks.figura.network.messages.avatar.AvatarProvideResponseHandler;
import net.blancworks.figura.network.messages.MessageHandler;
import net.blancworks.figura.network.messages.avatar.AvatarUploadResponseHandler;
import net.blancworks.figura.network.messages.user.UserAvatarHashProvideResponseHandler;
import net.blancworks.figura.network.messages.user.UserAvatarProvideResponseHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FiguraNetworkMessageHandler extends WebSocketAdapter {

    private final NewFiguraNetworkManager manager;

    private final HashMap<Byte, Supplier<MessageHandler>> allMessageHandlers = new HashMap<Byte, Supplier<MessageHandler>>() {{

        put(
                MessageIDs.AVATAR_PROVIDE_RESPONSE_HANDLER_ID,
                AvatarProvideResponseHandler::new
        );
        put(
                MessageIDs.AVATAR_UPLOAD_MESSAGE_ID,
                AvatarUploadResponseHandler::new
        );
        put(
                MessageIDs.USER_GET_AVATAR_UUID_RESPONSE_HANDLER_ID,
                UserAvatarProvideResponseHandler::new
        );
        put(
                MessageIDs.USER_AVATAR_HASH_RESPONSE_HANDLER_ID,
                UserAvatarHashProvideResponseHandler::new
        );
    }};

    private boolean skipNext = false;

    public FiguraNetworkMessageHandler(NewFiguraNetworkManager manager) {
        this.manager = manager;
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        super.onConnected(websocket, headers);
    }

    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
        super.onBinaryMessage(websocket, binary);

        if (skipNext) {
            skipNext = false;
            return;
        }

        //Get a stream for the bytes
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(ByteSource.wrap(binary).openStream());

        try {
            //Read the first byte, use that as the ID of the handler.
            byte handlerID = dis.readByte();

            //Get the handler.
            Supplier<MessageHandler> supplier = allMessageHandlers.get(handlerID);

            //If there is a supplier for this ID
            if (supplier != null) {
                //Get it
                MessageHandler handler = supplier.get();

                handler.handleMessage(dis);

            } else {
                FiguraMod.LOGGER.error("INVALID MESSAGE HANDLER ID " + handlerID);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        dis.close();
    }

    @Override
    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        super.onCloseFrame(websocket, frame);
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
        super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
        NewFiguraNetworkManager.currWebSocket = null;

        if (closedByServer) {
            FiguraMod.LOGGER.warn("Disconnected from Figura Server with reason '" + serverCloseFrame.getCloseReason() + "'");
            
            if(serverCloseFrame.getCloseReason().equals("Invalid Authentication")){
                NewFiguraNetworkManager.jwtToken = null;
                NewFiguraNetworkManager.tokenReceivedTime = null;
            }
        }
    }
}