package com.crystalcraft;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class keepAlive {
    private static ServerSocket serverSocket;
    private static Socket clientSocket;

    public static void keepAlive(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        clientSocket = serverSocket.accept();
    }

    public static void stop() throws IOException {
        serverSocket.close();
        if (clientSocket != null) clientSocket.close();
    }
}
