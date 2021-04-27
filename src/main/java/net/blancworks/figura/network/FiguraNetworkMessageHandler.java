package net.blancworks.figura.network;

import com.google.common.io.ByteSource;
import com.google.common.io.LittleEndianDataInputStream;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFrame;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.network.messages.AvatarDownloadMessageHandler;
import net.blancworks.figura.network.messages.DebugMessageHandler;
import net.blancworks.figura.network.messages.MessageHandler;

import java.io.DataInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FiguraNetworkMessageHandler extends WebSocketAdapter {

    private final NewFiguraNetworkManager manager;

    private final HashMap<Byte, Supplier<MessageHandler>> allMessageHandlers = new HashMap<Byte, Supplier<MessageHandler>>() {{

        //Avatar download request handler.
        put(
                (byte) -128,
                AvatarDownloadMessageHandler::new
        );
        put(
                (byte) 0,
                DebugMessageHandler::new
        );
    }};

    private MessageHandler lastHandler = null;
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

        //If there was no handler last message
        if (lastHandler == null) {
            //Get a stream for the bytes
            LittleEndianDataInputStream dis = new LittleEndianDataInputStream(ByteSource.wrap(binary).openStream());

            try {
                //Read the first byte, use that as the ID of the handler.
                byte handlerID = dis.readByte();
                int bodyLength = dis.readInt();

                //Get the handler.
                Supplier<MessageHandler> supplier = allMessageHandlers.get(handlerID);

                //If there is a supplier for this ID
                if (supplier != null) {
                    //Get it
                    lastHandler = supplier.get();
                    
                    //Handle the header.
                    lastHandler.handleHeader(dis);

                    if (!lastHandler.expectBody()) {
                        lastHandler = null;
                        skipNext = true;
                    }
                } else {
                    skipNext = true;
                    return;
                }
            } catch (Exception e) {
                skipNext = true;
                e.printStackTrace();
            }
            dis.close();
        } else {
            LittleEndianDataInputStream dis = new LittleEndianDataInputStream(ByteSource.wrap(binary).openStream());

            try {
                lastHandler.handleBody(dis);
            } catch (Exception e) {
                e.printStackTrace();
            }

            dis.close();
        }

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
            
        }

        NewFiguraNetworkManager.jwtToken = null;
    }
}