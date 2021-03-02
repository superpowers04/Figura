package net.blancworks.figura.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.awt.*;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

// Used to manage the network operations for Figura.
// Used for sending/receiving data, managing custom packets/networking, that sort.
public class FiguraNetworkManager {

    //This is the key for the session the user has with figura.
    //DO NOT LET PLAYERS ACCESS THIS!!!!!!
    //I mean, you could, but it just runs too many risks of them giving their key to someone who shouldn't have it.
    public static int figuraSessionKey;

    //False until the auth key has been acquired. also false if auth key becomes invalid.
    private static boolean hasObtainedAuth = false;

    public static CompletableFuture currentAuthTask = null;


    public static String GetServerAddress() {
        return "localhost";
    }
    
    //This is set to 
    public static String GetServerURL() {
        return String.format("http://%s:5001", GetServerAddress());
    }

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
            String address = GetServerAddress();
            InetAddress inetAddress = InetAddress.getByName("localhost");
            ClientConnection connection = ClientConnection.connect(inetAddress, 25565, true);
            connection.setPacketListener(new ClientLoginNetworkHandler(connection, MinecraftClient.getInstance(), null, (text) -> {
                System.out.println(text.toString());
            }));
            connection.send(new HandshakeC2SPacket(address, 25565, NetworkState.LOGIN));
            connection.send(new LoginHelloC2SPacket(MinecraftClient.getInstance().getSession().getProfile()));
            
            while(connection.isOpen())
                Thread.sleep(1);
            
            Text dcReason = connection.getDisconnectReason();
            
            if(dcReason instanceof Text){
                Text tc = (Text) dcReason;
                parseAuthKeyFromDisconnectMessage(tc);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        //Auth all done :D
        currentAuthTask = null;
    }


    public static void parseAuthKeyFromDisconnectMessage(Text reason){
        try{
            if(reason.asString().equals("This is the Figura Auth Server!\n")){

                Text keyText = reason.getSiblings().get(1);
                figuraSessionKey = Integer.parseInt(keyText.asString());
                hasObtainedAuth = true;

                LiteralText garbleText = new LiteralText("-------------------------\n\n\n");
                garbleText.setStyle(Style.EMPTY.withFormatting(Formatting.OBFUSCATED));

                reason.getSiblings().set(1, garbleText);
            }
        }
        catch (Exception e){
            System.out.println(e.toString());
        }
    }
    
    public static boolean hasAuthKey() {
        return hasObtainedAuth;
    }

}
