package com.hrbnu.se202104;


import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Util {
    public static void writeMessage(Socket socket, String message) throws IOException {
        socket.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().write('\n');
        socket.getOutputStream().flush();
    }
}
