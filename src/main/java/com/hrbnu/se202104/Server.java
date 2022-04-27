package com.hrbnu.se202104;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Server {
    private static AtomicInteger COUNTER = new AtomicInteger(0);
    private final ServerSocket server;
    private final Map<Integer, ClientConnection> clients = new ConcurrentHashMap<>();

    // TCP连接的端口号 0~65535
    public Server(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    public void start() throws IOException {
        while (true) {
            Socket socket = server.accept();
            new ClientConnection(COUNTER.incrementAndGet(), this, socket).start();
        }
    }

    public static void main(String[] args) throws IOException {
        new Server(8080).start();
    }

    public void registerClient(ClientConnection clientConnection) {
        clients.put(clientConnection.getClientId(), clientConnection);
        this.clientOnline(clientConnection);
    }

    private String getAllClientInfo() {
        return clients.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue().getClientName()).collect(Collectors.joining(","));
    }

    private void clientOnline(ClientConnection clientConnection) {
        clients.values().forEach(client -> dispatchMessage(client, "系统", "所有人", clientConnection.getClientName() + "上线了！" + getAllClientInfo()));
    }

    private void dispatchMessage(ClientConnection client, String src, String target, String message) {
        try {
            client.sendMessage(src + "对" + target + "说：" + message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(ClientConnection src, Message message) {
        if (message.getId() == 0) {
            clients.values().forEach(client -> dispatchMessage(client, src.getClientName(), "所有人", message.getMessage()));
        } else {
            int targetUser = message.getId();
            ClientConnection target = clients.get(targetUser);
            if (target == null) {
                System.out.println("用户" + targetUser + "不存在！");
            } else {
                dispatchMessage(target, src.getClientName(), "你", message.getMessage());
            }
        }
    }

    public void clientOffline(ClientConnection clientConnection) {
        clients.remove(clientConnection.getClientId());
        clients.values().forEach(client -> dispatchMessage(client, "系统", "所有人", clientConnection.getClientName() + "下线了！" + getAllClientInfo()));
    }

    public static class Message {
        Integer id;
        String message;

        public Message() {
        }

        public Message(Integer id, String message) {
            this.id = id;
            this.message = message;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }


    public static class ClientConnection extends Thread {
        private final Socket socket;
        private final Integer clientId;
        private String clientName;
        private final Server server;

        public ClientConnection(int clientId, Server server, Socket socket) {
            this.clientId = clientId;
            this.socket = socket;
            this.server = server;
        }

        public Integer getClientId() {
            return clientId;
        }

        public String getClientName() {
            return clientName;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (isNotOnlineYet()) {
                        clientName = line;
                        server.registerClient(this);

                    } else {
                        Message message = JSON.parseObject(line, Message.class);
                        server.sendMessage(this, message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                server.clientOffline(this);
            }
        }

        private boolean isNotOnlineYet() {
            return clientName == null;
        }

        public void sendMessage(String message) throws IOException {
            Util.writeMessage(socket, message);
        }
    }
}
