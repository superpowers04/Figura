package net.blancworks.figura.network;

// Used to manage the network operations for Figura.
// Used for sending/receiving data, managing custom packets/networking, that sort.
public class FiguraNetworkManager {
    
    //This is the key for the session the user has with figura.
    //DO NOT LET PLAYERS ACCESS THIS!!!!!!
    public static int figuraSessionKey;
    
    
    
    
    
    public static String GetServerURL(){
        return "http://localhost:5001";
    }
}
