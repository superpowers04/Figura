package net.blancworks.figura.network.messages;

public class MessageIDs {

    //--------------------HANDLERS--------------------
    
    //---AVATARS---
    //Avatar messages are between ID 0-19
    public static final byte AVATAR_PROVIDE_RESPONSE_HANDLER_ID = Byte.MIN_VALUE;
    public static final byte AVATAR_UPLOAD_RESPONSE_HANDLER_ID = Byte.MIN_VALUE + 1;
    public static final byte AVATAR_DELETE_RESPONSE_HANDLER_ID = Byte.MIN_VALUE + 2;
    
    //---USERS---
    //User messages are between ID 20-39
    public static final byte USER_GET_AVATAR_UUID_RESPONSE_HANDLER_ID = Byte.MIN_VALUE + 20;
    public static final byte USER_AVATAR_HASH_RESPONSE_HANDLER_ID = Byte.MIN_VALUE + 21;


    //--------------------SENDERS--------------------
    //---AVATARS---
    //Avatar messages are between ID 0-19
    public static final byte AVATAR_REQUEST_MESSAGE_ID = Byte.MIN_VALUE;
    public static final byte AVATAR_UPLOAD_MESSAGE_ID = Byte.MIN_VALUE + 1;
    public static final byte AVATAR_DELETE_MESSAGE_ID = Byte.MIN_VALUE + 2;

    //---USERS---
    //User messages are between ID 20-39
    public static final byte USER_GET_AVATAR_UUID_MESSAGE_ID = Byte.MIN_VALUE + 20;
    public static final byte USER_SET_CURRENT_AVATAR_MESSAGE_ID = Byte.MIN_VALUE + 21;
    public static final byte USER_DELETE_CURRENT_AVATAR_MESSAGE_ID = Byte.MIN_VALUE + 22;
    public static final byte USER_GET_CURRENT_AVATAR_MESSAGE_ID = Byte.MIN_VALUE + 23;
    public static final byte USER_GET_CURRENT_AVATAR_HASH_MESSAGE_ID = Byte.MIN_VALUE + 24;

}
