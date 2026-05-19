package com.example.lan;

import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class LanMultiplayerGame {
    private static final int PORT = 5050;
    private static final int DISCOVERY_PORT = 5051;
    private static final int CONNECT_TIMEOUT_MS = 2500;
    private static final int WORLD_WIDTH = 900;
    private static final int WORLD_HEIGHT = 600;


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LanMultiplayerGame().showMenu());
    }

    private final JFrame frame = new JFrame("Swing LAN Multiplayer");
    private GameClient client;
    private GameServer server;

    private void showMenu() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(520, 430));
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(null);
        JLabel title = new JLabel("Swing LAN Multiplayer", JLabel.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setBounds(20, 20, 460, 36);

        JButton hostButton = new JButton("Host Game");
        hostButton.setBounds(35, 75, 145, 42);

        JLabel listLabel = new JLabel("Hosted games on your LAN");
        listLabel.setBounds(35, 135, 250, 22);

        DefaultListModel<HostInfo> serverListModel = new DefaultListModel<>();
        JList<HostInfo> serverList = new JList<>(serverListModel);
        serverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane serverScroll = new JScrollPane(serverList);
        serverScroll.setBounds(35, 160, 445, 105);

        JButton refreshButton = new JButton("Refresh List");
        refreshButton.setBounds(35, 280, 145, 36);

        JButton joinSelectedButton = new JButton("Join Selected");
        joinSelectedButton.setBounds(190, 280, 145, 36);

        JTextField ipField = new JTextField("127.0.0.1");
        ipField.setBounds(190, 335, 145, 36);

        JButton joinButton = new JButton("Join IP");
        joinButton.setBounds(345, 335, 135, 36);

        JLabel manualLabel = new JLabel("Manual IP:");
        manualLabel.setBounds(35, 335, 145, 36);

        JLabel hint = new JLabel("No typing needed when a host appears in the list. TCP: " + PORT + "  UDP discovery: " + DISCOVERY_PORT, JLabel.CENTER);
        hint.setBounds(20, 380, 480, 24);

        hostButton.addActionListener(event -> hostGame());
        refreshButton.addActionListener(event -> refreshServerList(serverListModel));
        joinSelectedButton.addActionListener(event -> {
            HostInfo selected = serverList.getSelectedValue();
            if (selected == null) {
                showError("Select a hosted game first, or use Manual IP.");
                return;
            }
            joinGame(selected.host());
        });
        joinButton.addActionListener(event -> joinGame(ipField.getText().trim()));

        panel.add(title);
        panel.add(hostButton);
        panel.add(listLabel);
        panel.add(serverScroll);
        panel.add(refreshButton);
        panel.add(joinSelectedButton);
        panel.add(manualLabel);
        panel.add(ipField);
        panel.add(joinButton);
        panel.add(hint);

        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(520, 440);
        frame.setVisible(true);
        refreshServerList(serverListModel);
    }

    private void refreshServerList(DefaultListModel<HostInfo> serverListModel) {
        serverListModel.clear();
        serverListModel.addElement(new HostInfo("Searching...", "", 0));

        Thread refreshThread = new Thread(() -> {
            List<HostInfo> hosts = DiscoveryClient.findHosts();
            SwingUtilities.invokeLater(() -> {
                serverListModel.clear();
                if (hosts.isEmpty()) {
                    serverListModel.addElement(new HostInfo("No hosted games found", "", 0));
                    return;
                }
                for (HostInfo host : hosts) {
                    serverListModel.addElement(host);
                }
            });
        }, "server-list-refresh");
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private void hostGame() {
        try {
            server = new GameServer(PORT);
            server.start();
            joinGame("127.0.0.1");
        } catch (IOException exception) {
            showError("Could not host on port " + PORT + ": " + exception.getMessage());
        }
    }

    private void joinGame(String host) {
        if (host.isBlank()) {
            showError("Enter the host computer's local IP address.");
            return;
        }

        try {
            client = new GameClient(host, PORT);
            GamePanel gamePanel = new GamePanel(client);
            frame.setContentPane(gamePanel);
            frame.setSize(WORLD_WIDTH, WORLD_HEIGHT);
            frame.setLocationRelativeTo(null);
            gamePanel.requestFocusInWindow();
            client.start();
        } catch (IOException exception) {
            showError("Could not connect to " + host + ":" + PORT + "\n" + exception.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Network Error", JOptionPane.ERROR_MESSAGE);
    }

    private static final class GamePanel extends JPanel {
        private final GameClient client;

        private GamePanel(GameClient client) {
            this.client = client;
            setPreferredSize(new Dimension(WORLD_WIDTH, WORLD_HEIGHT));
            setBackground(new Color(19, 24, 36));
            setFocusable(true);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    setKey(event.getKeyCode(), true);
                }

                @Override
                public void keyReleased(KeyEvent event) {
                    setKey(event.getKeyCode(), false);
                }
            });

            new Timer(16, event -> repaint()).start();
        }

        private void setKey(int keyCode, boolean pressed) {
            if (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_UP) {
                client.setInput("up", pressed);
            } else if (keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_DOWN) {
                client.setInput("down", pressed);
            } else if (keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_LEFT) {
                client.setInput("left", pressed);
            } else if (keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_RIGHT) {
                client.setInput("right", pressed);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double scaleX = getWidth() / (double) WORLD_WIDTH;
            double scaleY = getHeight() / (double) WORLD_HEIGHT;
            g.scale(scaleX, scaleY);

            g.setColor(new Color(34, 43, 60));
            for (int x = 0; x < WORLD_WIDTH; x += 50) {
                g.drawLine(x, 0, x, WORLD_HEIGHT);
            }
            for (int y = 0; y < WORLD_HEIGHT; y += 50) {
                g.drawLine(0, y, WORLD_WIDTH, y);
            }

            Map<Integer, PlayerSnapshot> players = client.players();
            for (PlayerSnapshot player : players.values()) {
                g.setColor(player.color());
                g.fillRoundRect(player.x(), player.y(), 34, 34, 8, 8);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                g.drawString("P" + player.id(), player.x() + 7, player.y() + 22);
            }

            g.setColor(new Color(230, 235, 245));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString("You are P" + client.playerId() + "  |  WASD or arrows to move", 20, 30);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.drawString(client.status(), 20, 52);
        }
    }

    private record PlayerSnapshot(int id, int x, int y, Color color) {
    }

    private record HostInfo(String name, String host, int players) {
        @Override
        public String toString() {
            if (host.isBlank()) {
                return name;
            }
            return name + "  (" + host + ")  -  " + players + " player" + (players == 1 ? "" : "s");
        }
    }

    private static final class DiscoveryClient {
        private static List<HostInfo> findHosts() {
            Map<String, HostInfo> foundHosts = new LinkedHashMap<>();
            byte[] request = "LAN_GAME_DISCOVER".getBytes(StandardCharsets.UTF_8);

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.setSoTimeout(350);

                for (InetAddress address : broadcastAddresses()) {
                    DatagramPacket packet = new DatagramPacket(request, request.length, address, DISCOVERY_PORT);
                    socket.send(packet);
                }

                long deadline = System.currentTimeMillis() + 900;
                while (System.currentTimeMillis() < deadline) {
                    byte[] buffer = new byte[256];
                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(response);
                    } catch (SocketTimeoutException exception) {
                        continue;
                    }

                    HostInfo host = parseResponse(response);
                    if (host != null) {
                        foundHosts.put(host.host(), host);
                    }
                }
            } catch (IOException ignored) {
                // Some networks block UDP broadcast; manual IP join remains available.
            }

            return new ArrayList<>(foundHosts.values());
        }

        private static Set<InetAddress> broadcastAddresses() throws IOException {
            Set<InetAddress> addresses = new HashSet<>();
            addresses.add(InetAddress.getByName("255.255.255.255"));

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null) {
                        addresses.add(broadcast);
                    }
                }
            }
            return addresses;
        }

        private static HostInfo parseResponse(DatagramPacket response) {
            String message = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
            String[] parts = message.split(" ", 4);
            if (parts.length != 4 || !"LAN_GAME_HOST".equals(parts[0])) {
                return null;
            }

            try {
                int port = Integer.parseInt(parts[1]);
                int players = Integer.parseInt(parts[2]);
                if (port != PORT) {
                    return null;
                }
                return new HostInfo(parts[3], response.getAddress().getHostAddress(), players);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
    }

    private static final class GameClient {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private final Map<Integer, PlayerSnapshot> players = new ConcurrentHashMap<>();
        private final Object inputLock = new Object();
        private volatile int playerId = -1;
        private volatile boolean up;
        private volatile boolean down;
        private volatile boolean left;
        private volatile boolean right;
        private volatile String status = "Connecting...";

        private GameClient(String host, int port) throws IOException {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }

        private void start() {
            Thread reader = new Thread(this::readLoop, "client-reader");
            reader.setDaemon(true);
            reader.start();

            Timer inputTimer = new Timer(33, event -> sendInput());
            inputTimer.start();
        }

        private void setInput(String direction, boolean pressed) {
            synchronized (inputLock) {
                switch (direction) {
                    case "up" -> up = pressed;
                    case "down" -> down = pressed;
                    case "left" -> left = pressed;
                    case "right" -> right = pressed;
                    default -> {
                    }
                }
            }
        }

        private void sendInput() {
            boolean sendUp;
            boolean sendDown;
            boolean sendLeft;
            boolean sendRight;
            synchronized (inputLock) {
                sendUp = up;
                sendDown = down;
                sendLeft = left;
                sendRight = right;
            }
            out.println("INPUT " + sendUp + " " + sendDown + " " + sendLeft + " " + sendRight);
        }

        private void readLoop() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException exception) {
                status = "Disconnected: " + exception.getMessage();
            }
        }

        private void handleMessage(String line) {
            if (line.startsWith("WELCOME ")) {
                playerId = Integer.parseInt(line.substring("WELCOME ".length()));
                status = "Connected on port " + PORT;
            } else if (line.startsWith("STATE ")) {
                parseState(line.substring("STATE ".length()));
            }
        }

        private void parseState(String body) {
            Map<Integer, PlayerSnapshot> nextPlayers = new LinkedHashMap<>();
            if (!body.isBlank()) {
                String[] entries = body.split(";");
                for (String entry : entries) {
                    String[] parts = entry.split(",");
                    if (parts.length == 4) {
                        int id = Integer.parseInt(parts[0]);
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        Color color = new Color(Integer.parseInt(parts[3]));
                        nextPlayers.put(id, new PlayerSnapshot(id, x, y, color));
                    }
                }
            }
            players.clear();
            players.putAll(nextPlayers);
        }

        private Map<Integer, PlayerSnapshot> players() {
            List<Integer> ids = new ArrayList<>(players.keySet());
            Collections.sort(ids);
            Map<Integer, PlayerSnapshot> sortedPlayers = new LinkedHashMap<>();
            for (Integer id : ids) {
                sortedPlayers.put(id, players.get(id));
            }
            return sortedPlayers;
        }

        private int playerId() {
            return playerId;
        }

        private String status() {
            return status;
        }
    }

    private static final class GameServer {
        private final ServerSocket serverSocket;
        private final AtomicInteger nextId = new AtomicInteger(1);
        private final Map<Integer, ServerPlayer> players = new ConcurrentHashMap<>();
        private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
        private DatagramSocket discoverySocket;
        private volatile boolean running = true;

        private GameServer(int port) throws IOException {
            serverSocket = new ServerSocket(port);
        }

        private void start() {
            Thread acceptThread = new Thread(this::acceptLoop, "server-accept");
            acceptThread.setDaemon(true);
            acceptThread.start();

            Thread gameThread = new Thread(this::gameLoop, "server-game-loop");
            gameThread.setDaemon(true);
            gameThread.start();

            Thread discoveryThread = new Thread(this::discoveryLoop, "server-discovery");
            discoveryThread.setDaemon(true);
            discoveryThread.start();
        }

        private void acceptLoop() {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    int id = nextId.getAndIncrement();
                    ServerPlayer player = new ServerPlayer(id, 80 + id * 45, 120 + id * 35, colorFor(id));
                    players.put(id, player);

                    ClientHandler handler = new ClientHandler(socket, id, this);
                    clients.add(handler);
                    handler.start();
                } catch (SocketException exception) {
                    running = false;
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }

        private void gameLoop() {
            long lastTick = System.nanoTime();
            while (running) {
                long now = System.nanoTime();
                double deltaSeconds = (now - lastTick) / 1_000_000_000.0;
                lastTick = now;

                updatePlayers(deltaSeconds);
                broadcastState();
                sleep(16);
            }
        }

        private void discoveryLoop() {
            try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
                discoverySocket = socket;
                byte[] buffer = new byte[256];
                while (running) {
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    socket.receive(request);

                    String message = new String(request.getData(), 0, request.getLength(), StandardCharsets.UTF_8);
                    if (!"LAN_GAME_DISCOVER".equals(message)) {
                        continue;
                    }

                    String responseText = "LAN_GAME_HOST " + PORT + " " + players.size() + " " + hostName();
                    byte[] response = responseText.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(response, response.length, request.getAddress(), request.getPort());
                    socket.send(packet);
                }
            } catch (SocketException exception) {
                if (running) {
                    System.err.println("Discovery unavailable on UDP port " + DISCOVERY_PORT + ": " + exception.getMessage());
                }
            } catch (IOException exception) {
                if (running) {
                    exception.printStackTrace();
                }
            }
        }

        private void updatePlayers(double deltaSeconds) {
            int speed = 230;
            for (ServerPlayer player : players.values()) {
                int dx = 0;
                int dy = 0;
                if (player.up) {
                    dy--;
                }
                if (player.down) {
                    dy++;
                }
                if (player.left) {
                    dx--;
                }
                if (player.right) {
                    dx++;
                }

                int move = (int) Math.round(speed * deltaSeconds);
                player.x = clamp(player.x + dx * move, 0, WORLD_WIDTH - 34);
                player.y = clamp(player.y + dy * move, 0, WORLD_HEIGHT - 34);
            }
        }

        private void broadcastState() {
            StringBuilder builder = new StringBuilder("STATE ");
            List<Integer> ids = new ArrayList<>(players.keySet());
            Collections.sort(ids);
            for (int i = 0; i < ids.size(); i++) {
                ServerPlayer player = players.get(ids.get(i));
                if (i > 0) {
                    builder.append(';');
                }
                builder.append(player.id).append(',')
                        .append(player.x).append(',')
                        .append(player.y).append(',')
                        .append(player.color.getRGB());
            }
            String state = builder.toString();
            for (ClientHandler client : clients) {
                client.send(state);
            }
        }

        private void removeClient(int id, ClientHandler handler) {
            players.remove(id);
            clients.remove(handler);
        }

        private static String hostName() {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (IOException exception) {
                return "LAN Host";
            }
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private static Color colorFor(int id) {
            Color[] colors = {
                    new Color(66, 165, 245),
                    new Color(102, 187, 106),
                    new Color(255, 202, 40),
                    new Color(239, 83, 80),
                    new Color(171, 71, 188),
                    new Color(38, 198, 218)
            };
            return colors[(id - 1) % colors.length];
        }

        private static void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class ServerPlayer {
        private final int id;
        private final Color color;
        private volatile int x;
        private volatile int y;
        private volatile boolean up;
        private volatile boolean down;
        private volatile boolean left;
        private volatile boolean right;

        private ServerPlayer(int id, int x, int y, Color color) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    private static final class ClientHandler {
        private final Socket socket;
        private final int playerId;
        private final GameServer server;
        private final PrintWriter out;

        private ClientHandler(Socket socket, int playerId, GameServer server) throws IOException {
            this.socket = socket;
            this.playerId = playerId;
            this.server = server;
            out = new PrintWriter(socket.getOutputStream(), true);
        }

        private void start() {
            out.println("WELCOME " + playerId);
            Thread thread = new Thread(this::readLoop, "server-client-" + playerId);
            thread.setDaemon(true);
            thread.start();
        }

        private void readLoop() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException ignored) {
                // Leaving the game is normal: closing a laptop should not spam a stack trace.
            } finally {
                server.removeClient(playerId, this);
            }
        }

        private void handleMessage(String line) {
            if (!line.startsWith("INPUT ")) {
                return;
            }

            String[] parts = line.split(" ");
            if (parts.length != 5) {
                return;
            }

            ServerPlayer player = server.players.get(playerId);
            if (player == null) {
                return;
            }

            player.up = Boolean.parseBoolean(parts[1]);
            player.down = Boolean.parseBoolean(parts[2]);
            player.left = Boolean.parseBoolean(parts[3]);
            player.right = Boolean.parseBoolean(parts[4]);
        }

        private void send(String message) {
            out.println(message);
        }
    }
}
