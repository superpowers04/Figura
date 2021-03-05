package net.blancworks.figura.network;

import com.google.gson.JsonObject;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.nbt.CompoundTag;
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

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Base64;
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
                FiguraMod.LOGGER.log(Level.ERROR, text.toString());
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
            FiguraMod.LOGGER.log(Level.ERROR, e);
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
            FiguraMod.LOGGER.log(Level.ERROR, e.toString());
        }
    }
    
    public static boolean hasAuthKey() {
        return hasObtainedAuth;
    }
    
    public static void postModel(){
        String uuidString = MinecraftClient.getInstance().player.getUuid().toString();

        try {
            URL url = new URL(String.format("%s/api/avatar/%s?key=%d", FiguraNetworkManager.GetServerURL(), uuidString, figuraSessionKey));


            CompletableFuture.runAsync(() -> {
                HttpURLConnection httpURLConnection = null;

                try {
                    PlayerData data = PlayerDataManager.localPlayer;

                    CompoundTag infoTag = new CompoundTag();
                    data.toNBT(infoTag);

                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    DataOutputStream nbtDataStream = new DataOutputStream(byteStream);
                    infoTag.write(nbtDataStream);

                    JsonObject finalObject = new JsonObject();

                    finalObject.addProperty("data", Base64.getEncoder().encodeToString(byteStream.toByteArray()));

                    String finalResult = finalObject.toString();

                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("PUT");
                    httpURLConnection.setRequestProperty("Content-Type", "application/json");

                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    //httpURLConnection.connect();
                    OutputStream outStream = httpURLConnection.getOutputStream();
                    OutputStreamWriter outWriter = new OutputStreamWriter(outStream);

                    outWriter.write(finalResult);
                    outWriter.close();

                    //FiguraMod.LOGGER.log(Level.ERROR, httpURLConnection.getResponseMessage());
                } catch (Exception e) {
                    FiguraMod.LOGGER.log(Level.ERROR, e);
                }
            }, Util.getMainWorkerExecutor());
        } catch (Exception e) {
            FiguraMod.LOGGER.log(Level.ERROR, e);
        }

    }

}
