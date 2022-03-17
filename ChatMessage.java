package chatapplication;

import java.io.*;

final class ChatMessage implements Serializable {
    private static final long serialVersionUID = 6898543889087L;
    private int type;
    private String message;

    // Here is where you should implement the chat message object.
    // Variables, Constructors, Methods, etc.

    public ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}