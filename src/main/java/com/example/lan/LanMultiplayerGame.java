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
import java.awt.Rectangle;
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
    private static final int PLAYER_SIZE = 34;
    private static final int STARTING_LIVES = 5;
    private static final int CHEST_SIZE = 24;
    private static final int DEFAULT_BULLET_DAMAGE = 14;
    private static final double MOVE_SPEED = 270.0;
    private static final double JUMP_SPEED = 660.0;
    private static final double GRAVITY = 1450.0;
    private static final double FRICTION = 0.82;
    private static final double SHOOT_COOLDOWN_SECONDS = 0.28;
    private static final double BULLET_SPEED = 620.0;
    private static final double BULLET_LIFETIME_SECONDS = 1.7;
    private static final double HIT_FLASH_SECONDS = 0.22;
    private static final double HIT_TEXT_SECONDS = 0.85;
    private static final double RESPAWN_SECONDS = 2.0;
    private static final double CHEST_RESPAWN_SECONDS = 7.0;
    private static final Rectangle[] PLATFORMS = {
            new Rectangle(155, 485, 590, 26),
            new Rectangle(80, 360, 220, 22),
            new Rectangle(600, 360, 220, 22),
            new Rectangle(340, 255, 220, 22),
            new Rectangle(190, 170, 150, 18),
            new Rectangle(560, 170, 150, 18)
    };


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LanMultiplayerGame().showMenu());
    }

    private final JFrame frame = new JFrame("LAN Platform Mayhem");
    private GameClient client;
    private GameServer server;
    private Timer menuRefreshTimer;

    private void showMenu() {
        if (menuRefreshTimer != null) {
            menuRefreshTimer.stop();
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(520, 430));
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(null);
        JLabel title = new JLabel("LAN Platform Mayhem", JLabel.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setBounds(20, 20, 460, 36);

        JButton hostButton = new JButton("Host Game");
        hostButton.setBounds(35, 75, 145, 42);

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setBounds(190, 75, 55, 42);

        JTextField nameField = new JTextField(defaultPlayerName());
        nameField.setBounds(245, 78, 235, 36);

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

        JLabel hint = new JLabel("Your LAN IP: " + localAddressSummary() + "  |  TCP: " + PORT + "  UDP: " + DISCOVERY_PORT, JLabel.CENTER);
        hint.setBounds(20, 380, 480, 24);

        hostButton.addActionListener(event -> hostGame(nameField.getText()));
        refreshButton.addActionListener(event -> refreshServerList(serverListModel));
        joinSelectedButton.addActionListener(event -> {
            HostInfo selected = serverList.getSelectedValue();
            if (selected == null || selected.host().isBlank()) {
                showError("Select a hosted game first, or use Manual IP.");
                return;
            }
            joinGame(selected.host(), nameField.getText());
        });
        joinButton.addActionListener(event -> joinGame(ipField.getText().trim(), nameField.getText()));
        serverList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    HostInfo selected = serverList.getSelectedValue();
                    if (selected != null && !selected.host().isBlank()) {
                        joinGame(selected.host(), nameField.getText());
                    }
                }
            }
        });
        ipField.addActionListener(event -> joinGame(ipField.getText().trim(), nameField.getText()));
        nameField.addActionListener(event -> hostGame(nameField.getText()));

        panel.add(title);
        panel.add(hostButton);
        panel.add(nameLabel);
        panel.add(nameField);
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
        menuRefreshTimer = new Timer(3000, event -> refreshServerList(serverListModel));
        menuRefreshTimer.start();
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

    private void hostGame(String playerName) {
        try {
            server = new GameServer(PORT);
            server.start();
            joinGame("127.0.0.1", playerName);
        } catch (IOException exception) {
            showError("Could not host on port " + PORT + ": " + exception.getMessage());
        }
    }

    private void joinGame(String host, String playerName) {
        if (host.isBlank()) {
            showError("Enter the host computer's local IP address.");
            return;
        }

        try {
            client = new GameClient(host, PORT, sanitizeName(playerName));
            GamePanel gamePanel = new GamePanel(client);
            frame.setContentPane(gamePanel);
            frame.setSize(WORLD_WIDTH, WORLD_HEIGHT);
            frame.setLocationRelativeTo(null);
            gamePanel.requestFocusInWindow();
            client.start();
            if (menuRefreshTimer != null) {
                menuRefreshTimer.stop();
            }
        } catch (IOException exception) {
            showError("Could not connect to " + host + ":" + PORT + "\n" + exception.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Network Error", JOptionPane.ERROR_MESSAGE);
    }

    private static String defaultPlayerName() {
        String user = System.getProperty("user.name", "Player");
        return sanitizeName(user);
    }

    private static String sanitizeName(String name) {
        String clean = name == null ? "" : name.trim()
                .replace(",", "")
                .replace(";", "")
                .replace("|", "")
                .replace("\n", "")
                .replace("\r", "");
        if (clean.isBlank()) {
            return "Player";
        }
        return clean.length() > 16 ? clean.substring(0, 16) : clean;
    }

    private static String localAddressSummary() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();
                    String text = address.getHostAddress();
                    if (!text.contains(":")) {
                        addresses.add(text);
                    }
                }
            }
        } catch (SocketException ignored) {
            return "unknown";
        }
        if (addresses.isEmpty()) {
            return "unknown";
        }
        return String.join(", ", addresses);
    }

    private static final class GamePanel extends JPanel {
        private final GameClient client;

        private GamePanel(GameClient client) {
            this.client = client;
            setLayout(null);
            setPreferredSize(new Dimension(WORLD_WIDTH, WORLD_HEIGHT));
            setBackground(new Color(19, 24, 36));
            setFocusable(true);

            JButton addBotButton = new JButton("Add Bot");
            addBotButton.setBounds(20, 66, 95, 28);
            addBotButton.setFocusable(false);
            addBotButton.addActionListener(event -> {
                client.addBot();
                requestFocusInWindow();
            });
            add(addBotButton);

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
            } else if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_J) {
                client.setInput("attack", pressed);
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

            g.setColor(new Color(86, 94, 112));
            for (Rectangle platform : PLATFORMS) {
                g.fillRoundRect(platform.x, platform.y, platform.width, platform.height, 8, 8);
                g.setColor(new Color(119, 133, 157));
                g.fillRoundRect(platform.x, platform.y, platform.width, 5, 8, 8);
                g.setColor(new Color(86, 94, 112));
            }

            for (ChestSnapshot chest : client.chests()) {
                g.setColor(new Color(184, 124, 50));
                g.fillRoundRect(chest.x(), chest.y(), CHEST_SIZE, CHEST_SIZE, 6, 6);
                g.setColor(new Color(246, 206, 92));
                g.fillRect(chest.x() + 2, chest.y() + 9, CHEST_SIZE - 4, 4);
                g.fillRect(chest.x() + CHEST_SIZE / 2 - 2, chest.y() + 2, 4, CHEST_SIZE - 4);
                g.setColor(new Color(45, 33, 24));
                g.drawRoundRect(chest.x(), chest.y(), CHEST_SIZE, CHEST_SIZE, 6, 6);
            }

            for (BulletSnapshot bullet : client.bullets()) {
                g.setColor(bullet.color());
                g.setStroke(new java.awt.BasicStroke(3));
                g.drawLine(bullet.x1(), bullet.y1(), bullet.x2(), bullet.y2());
                g.setStroke(new java.awt.BasicStroke(1));
            }

            Map<Integer, PlayerSnapshot> players = client.players();
            for (PlayerSnapshot player : players.values()) {
                if (player.lives() <= 0) {
                    continue;
                }
                if (player.y() < -PLAYER_SIZE) {
                    drawSkyArrow(g, player);
                    continue;
                }
                if (player.respawnSeconds() > 0) {
                    continue;
                }

                if (player.hitFlashSeconds() > 0) {
                    float pulse = (float) Math.min(1.0, player.hitFlashSeconds() / HIT_FLASH_SECONDS);
                    g.setColor(new Color(255, 245, 120, 70 + (int) (110 * pulse)));
                    g.fillOval(player.x() - 12, player.y() - 12, PLAYER_SIZE + 24, PLAYER_SIZE + 24);
                    g.setColor(new Color(255, 255, 255, 160));
                    g.drawRoundRect(player.x() - 4, player.y() - 4, PLAYER_SIZE + 8, PLAYER_SIZE + 8, 10, 10);
                }

                drawCharacter(g, player);
                if (player.hitFlashSeconds() > 0) {
                    g.setColor(new Color(255, 255, 255, 185));
                    g.fillRoundRect(player.x() + 5, player.y() + 5, PLAYER_SIZE - 10, PLAYER_SIZE - 10, 6, 6);
                }

                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                g.drawString(player.name(), player.x() - 4, player.y() - 10);
                if (player.hitTextSeconds() > 0) {
                    float lift = (float) ((HIT_TEXT_SECONDS - player.hitTextSeconds()) * 32);
                    g.setColor(new Color(255, 250, 150));
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                    String text = player.hitCombo() + " hit" + (player.hitCombo() == 1 ? "" : "s");
                    int width = g.getFontMetrics().stringWidth(text);
                    g.drawString(text, player.x() + PLAYER_SIZE / 2 - width / 2, (int) (player.y() - 22 - lift));
                }
            }

            g.setColor(new Color(230, 235, 245));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString("Platform Mayhem  |  You are P" + client.playerId(), 20, 30);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            PlayerSnapshot me = players.get(client.playerId());
            String loadout = me == null ? "" : "   Weapon: " + me.weapon() + " " + (me.ammo() < 0 ? "inf" : me.ammo());
            g.drawString("Move: A/D or arrows   Jump: W/Up   Shoot: Space or J" + loadout + "   " + client.status(), 20, 52);
            drawScoreboard(g, players);
            drawRespawnOverlay(g, players.get(client.playerId()));
        }

        private void drawCharacter(Graphics2D g, PlayerSnapshot player) {
            int face = player.facingX() < 0 ? -1 : 1;
            g.setColor(player.color());
            g.fillRoundRect(player.x() + 4, player.y() + 7, PLAYER_SIZE - 8, PLAYER_SIZE - 5, 8, 8);
            g.setColor(new Color(242, 219, 184));
            g.fillOval(player.x() + 7, player.y(), 20, 20);
            g.setColor(Color.WHITE);
            int eyeX = face > 0 ? player.x() + 21 : player.x() + 10;
            g.fillOval(eyeX, player.y() + 7, 4, 4);
            g.setColor(new Color(30, 34, 44));
            int gunX = face > 0 ? player.x() + 25 : player.x() - 12;
            g.fillRoundRect(gunX, player.y() + 18, 22, 6, 4, 4);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.drawString("P" + player.id(), player.x() + 8, player.y() + 31);
        }

        private void drawSkyArrow(Graphics2D g, PlayerSnapshot player) {
            int x = Math.max(24, Math.min(WORLD_WIDTH - 24, player.x() + PLAYER_SIZE / 2));
            int distance = Math.max(0, -player.y());
            g.setColor(player.color());
            int[] xs = {x, x - 12, x + 12};
            int[] ys = {12, 34, 34};
            g.fillPolygon(xs, ys, 3);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            String text = distance + " px";
            int width = g.getFontMetrics().stringWidth(text);
            g.drawString(text, x - width / 2, 50);
        }

        private void drawScoreboard(Graphics2D g, Map<Integer, PlayerSnapshot> players) {
            int x = WORLD_WIDTH - 235;
            int y = 20;
            g.setColor(new Color(10, 13, 20, 180));
            g.fillRoundRect(x, y, 210, 28 + players.size() * 24, 8, 8);
            g.setColor(new Color(230, 235, 245));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString("Scoreboard", x + 12, y + 19);

            List<PlayerSnapshot> sorted = new ArrayList<>(players.values());
            sorted.sort((a, b) -> Integer.compare(b.score(), a.score()));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            for (int i = 0; i < sorted.size(); i++) {
                PlayerSnapshot player = sorted.get(i);
                int rowY = y + 43 + i * 24;
                g.setColor(player.color());
                g.fillRoundRect(x + 12, rowY - 11, 12, 12, 4, 4);
                g.setColor(Color.WHITE);
                String status = player.lives() <= 0
                        ? " out"
                        : player.respawnSeconds() > 0 ? " respawn " + Math.ceil(player.respawnSeconds()) + "s" : "";
                g.drawString(player.name() + "  KOs " + player.score()
                        + "  Lives " + player.lives() + "  " + player.weapon() + status, x + 32, rowY);
            }
        }

        private void drawRespawnOverlay(Graphics2D g, PlayerSnapshot player) {
            if (player == null || player.respawnSeconds() <= 0) {
                return;
            }
            g.setColor(new Color(10, 13, 20, 185));
            g.fillRoundRect(WORLD_WIDTH / 2 - 150, WORLD_HEIGHT / 2 - 38, 300, 76, 10, 10);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            String text = "Respawning in " + (int) Math.ceil(player.respawnSeconds()) + "s";
            int width = g.getFontMetrics().stringWidth(text);
            g.drawString(text, WORLD_WIDTH / 2 - width / 2, WORLD_HEIGHT / 2 + 7);
        }
    }

    private record PlayerSnapshot(
            int id,
            int x,
            int y,
            Color color,
            int damage,
            int score,
            int deaths,
            int lives,
            String name,
            int facingX,
            int facingY,
            double hitFlashSeconds,
            double hitTextSeconds,
            int hitCombo,
            String weapon,
            int ammo,
            double respawnSeconds) {
    }

    private record BulletSnapshot(int x1, int y1, int x2, int y2, Color color) {
    }

    private record ChestSnapshot(int x, int y) {
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
        private final List<BulletSnapshot> bullets = new CopyOnWriteArrayList<>();
        private final List<ChestSnapshot> chests = new CopyOnWriteArrayList<>();
        private final Object inputLock = new Object();
        private volatile int playerId = -1;
        private volatile boolean up;
        private volatile boolean down;
        private volatile boolean left;
        private volatile boolean right;
        private volatile boolean attack;
        private volatile String status = "Connecting...";

        private GameClient(String host, int port, String playerName) throws IOException {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("NAME " + sanitizeName(playerName));
        }

        private void start() {
            Thread reader = new Thread(this::readLoop, "client-reader");
            reader.setDaemon(true);
            reader.start();

            Timer inputTimer = new Timer(33, event -> sendInput());
            inputTimer.start();
        }

        private void addBot() {
            out.println("ADD_BOT");
        }

        private void setInput(String direction, boolean pressed) {
            synchronized (inputLock) {
                switch (direction) {
                    case "up" -> up = pressed;
                    case "down" -> down = pressed;
                    case "left" -> left = pressed;
                    case "right" -> right = pressed;
                    case "attack" -> attack = pressed;
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
            boolean sendAttack;
            synchronized (inputLock) {
                sendUp = up;
                sendDown = down;
                sendLeft = left;
                sendRight = right;
                sendAttack = attack;
            }
            out.println("INPUT " + sendUp + " " + sendDown + " " + sendLeft + " " + sendRight + " " + sendAttack);
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
            List<BulletSnapshot> nextBullets = new ArrayList<>();
            try {
                String[] sections = body.split("\\|", 3);
                String playerSection = sections.length > 0 ? sections[0] : "";
                String bulletSection = sections.length > 1 ? sections[1] : "";
                String chestSection = sections.length > 2 ? sections[2] : "";

                if (!playerSection.isBlank()) {
                    String[] entries = playerSection.split(";");
                    for (String entry : entries) {
                        String[] parts = entry.split(",");
                        if (parts.length == 17) {
                            int id = Integer.parseInt(parts[0]);
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            Color color = new Color(Integer.parseInt(parts[3]));
                            int damage = Integer.parseInt(parts[4]);
                            int score = Integer.parseInt(parts[5]);
                            int deaths = Integer.parseInt(parts[6]);
                            int lives = Integer.parseInt(parts[7]);
                            String name = parts[8];
                            int facingX = Integer.parseInt(parts[9]);
                            int facingY = Integer.parseInt(parts[10]);
                            double hitFlashSeconds = Double.parseDouble(parts[11]);
                            double hitTextSeconds = Double.parseDouble(parts[12]);
                            int hitCombo = Integer.parseInt(parts[13]);
                            String weapon = parts[14];
                            int ammo = Integer.parseInt(parts[15]);
                            double respawnSeconds = Double.parseDouble(parts[16]);
                            nextPlayers.put(id, new PlayerSnapshot(id, x, y, color, damage, score, deaths, lives, name,
                                    facingX, facingY, hitFlashSeconds, hitTextSeconds, hitCombo, weapon, ammo, respawnSeconds));
                        }
                    }
                }
                if (!bulletSection.isBlank()) {
                    String[] entries = bulletSection.split(";");
                    for (String entry : entries) {
                        String[] parts = entry.split(",");
                        if (parts.length == 5) {
                            nextBullets.add(new BulletSnapshot(
                                    Integer.parseInt(parts[0]),
                                    Integer.parseInt(parts[1]),
                                    Integer.parseInt(parts[2]),
                                    Integer.parseInt(parts[3]),
                                    new Color(Integer.parseInt(parts[4]))));
                        }
                    }
                }
                List<ChestSnapshot> nextChests = new ArrayList<>();
                if (!chestSection.isBlank()) {
                    String[] entries = chestSection.split(";");
                    for (String entry : entries) {
                        String[] parts = entry.split(",");
                        if (parts.length == 2) {
                            nextChests.add(new ChestSnapshot(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
                        }
                    }
                }
                chests.clear();
                chests.addAll(nextChests);
            } catch (RuntimeException exception) {
                status = "Ignored bad server state: " + exception.getMessage();
                return;
            }
            players.clear();
            players.putAll(nextPlayers);
            bullets.clear();
            bullets.addAll(nextBullets);
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

        private List<BulletSnapshot> bullets() {
            return new ArrayList<>(bullets);
        }

        private List<ChestSnapshot> chests() {
            return new ArrayList<>(chests);
        }
    }

    private static final class GameServer {
        private final ServerSocket serverSocket;
        private final AtomicInteger nextId = new AtomicInteger(1);
        private final AtomicInteger nextBulletId = new AtomicInteger(1);
        private final Map<Integer, ServerPlayer> players = new ConcurrentHashMap<>();
        private final Map<Integer, ServerBullet> bullets = new ConcurrentHashMap<>();
        private final Map<Integer, ServerChest> chests = new ConcurrentHashMap<>();
        private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
        private double chestTimer;
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
                    ServerPlayer player = new ServerPlayer(id, spawnX(id), spawnY(id), colorFor(id), false);
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

        private void addBot() {
            int id = nextId.getAndIncrement();
            ServerPlayer bot = new ServerPlayer(id, spawnX(id), spawnY(id), colorFor(id), true);
            bot.name = "Bot " + id;
            players.put(id, bot);
        }

        private void gameLoop() {
            long lastTick = System.nanoTime();
            while (running) {
                long now = System.nanoTime();
                double deltaSeconds = (now - lastTick) / 1_000_000_000.0;
                lastTick = now;

                updatePlayers(deltaSeconds);
                updateBullets(deltaSeconds);
                updateChests(deltaSeconds);
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
            for (ServerPlayer player : players.values()) {
                if (player.bot) {
                    updateBot(player, deltaSeconds);
                }

                player.shootCooldown = Math.max(0, player.shootCooldown - deltaSeconds);
                player.hitFlashTimer = Math.max(0, player.hitFlashTimer - deltaSeconds);
                player.hitTextTimer = Math.max(0, player.hitTextTimer - deltaSeconds);
                if (player.hitTextTimer == 0) {
                    player.hitCombo = 0;
                }

                if (player.lives <= 0) {
                    continue;
                }

                if (player.respawnTimer > 0) {
                    player.respawnTimer = Math.max(0, player.respawnTimer - deltaSeconds);
                    if (player.respawnTimer == 0) {
                        respawn(player);
                    }
                    continue;
                }

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

                if (dx != 0) {
                    player.facingX = Integer.signum(dx);
                    player.facingY = 0;
                    player.vx = dx * MOVE_SPEED;
                } else {
                    player.vx *= FRICTION;
                }

                if (player.up && player.grounded) {
                    player.vy = -JUMP_SPEED;
                    player.grounded = false;
                }
                if (player.down && !player.grounded) {
                    player.vy += GRAVITY * 0.75 * deltaSeconds;
                }

                if (player.attackQueued && player.shootCooldown == 0) {
                    player.attackQueued = false;
                    shoot(player);
                }

                player.vy += GRAVITY * deltaSeconds;
                movePlayer(player, deltaSeconds);
                collectChests(player);

                if (player.y > WORLD_HEIGHT + 90 || player.x < -120 || player.x > WORLD_WIDTH + 120) {
                    ringOut(player, null);
                }
            }
        }

        private void updateBot(ServerPlayer bot, double deltaSeconds) {
            bot.botThinkTimer -= deltaSeconds;
            if (bot.botThinkTimer > 0) {
                return;
            }
            bot.botThinkTimer = 0.12;

            ServerPlayer target = nearestTarget(bot);
            bot.up = false;
            bot.down = false;
            bot.left = false;
            bot.right = false;

            if (target == null || bot.respawnTimer > 0) {
                return;
            }

            double botCenterX = bot.x + PLAYER_SIZE / 2.0;
            double botCenterY = bot.y + PLAYER_SIZE / 2.0;
            double targetCenterX = target.x + PLAYER_SIZE / 2.0;
            double targetCenterY = target.y + PLAYER_SIZE / 2.0;
            double dx = targetCenterX - botCenterX;
            double dy = targetCenterY - botCenterY;
            double distance = Math.abs(dx) + Math.abs(dy);
            double preferredDistance = 210 + (bot.id % 3) * 35;
            boolean closeToLeftEdge = bot.x < 45;
            boolean closeToRightEdge = bot.x > WORLD_WIDTH - PLAYER_SIZE - 45;
            boolean targetIsLeft = dx < 0;

            if (closeToLeftEdge) {
                bot.right = true;
            } else if (closeToRightEdge) {
                bot.left = true;
            } else if (Math.abs(dx) > preferredDistance + 35) {
                bot.left = targetIsLeft;
                bot.right = !targetIsLeft;
            } else if (Math.abs(dx) < preferredDistance - 45 && Math.abs(dy) < 110) {
                bot.left = !targetIsLeft;
                bot.right = targetIsLeft;
            } else if (Math.abs(dx) > 45 && Math.abs(dy) > 130) {
                bot.left = targetIsLeft;
                bot.right = !targetIsLeft;
            }

            if (nearPlatformEdge(bot) && bot.grounded) {
                bot.up = true;
                if (!closeToLeftEdge && !closeToRightEdge) {
                    bot.left = !targetIsLeft;
                    bot.right = targetIsLeft;
                }
            } else if (dy < -55 && bot.grounded) {
                bot.up = true;
            }

            if (distance <= 470 && Math.abs(dy) < 95 && hasClearShot(bot, target)) {
                bot.attackQueued = true;
                bot.facingX = dx < 0 ? -1 : 1;
                bot.facingY = 0;
            }
        }

        private boolean hasClearShot(ServerPlayer bot, ServerPlayer target) {
            double y = bot.y + PLAYER_SIZE / 2.0;
            double minX = Math.min(bot.x, target.x);
            double maxX = Math.max(bot.x, target.x);
            for (Rectangle platform : PLATFORMS) {
                boolean crossesPlatform = y >= platform.y && y <= platform.y + platform.height
                        && maxX >= platform.x && minX <= platform.x + platform.width;
                if (crossesPlatform) {
                    return false;
                }
            }
            return true;
        }

        private boolean nearPlatformEdge(ServerPlayer player) {
            int footX = (int) Math.round(player.x + PLAYER_SIZE / 2.0 + player.facingX * 28);
            int footY = (int) Math.round(player.y + PLAYER_SIZE + 8);
            for (Rectangle platform : PLATFORMS) {
                if (platform.contains(footX, footY)) {
                    return false;
                }
            }
            return true;
        }

        private ServerPlayer nearestTarget(ServerPlayer bot) {
            ServerPlayer nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (ServerPlayer player : players.values()) {
                if (player.id == bot.id || player.respawnTimer > 0) {
                    continue;
                }
                double distance = Math.abs(player.x - bot.x) + Math.abs(player.y - bot.y);
                if (distance < nearestDistance) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }
            return nearest;
        }

        private void shoot(ServerPlayer player) {
            WeaponStats stats = statsFor(player.weapon);
            if (stats.ammoLimited && player.ammo <= 0) {
                player.weapon = Weapon.PISTOL;
                player.ammo = -1;
                stats = statsFor(player.weapon);
            }
            player.shootCooldown = stats.cooldown;
            double direction = player.facingX == 0 ? 1 : player.facingX;
            double bulletX = player.x + PLAYER_SIZE / 2.0 + direction * 22;
            double bulletY = player.y + PLAYER_SIZE / 2.0 - 3;
            int bulletId = nextBulletId.getAndIncrement();
            bullets.put(bulletId, new ServerBullet(bulletId, player.id, bulletX, bulletY,
                    direction * stats.bulletSpeed, 0, stats.life, player.color, stats.damage));
            player.vx -= direction * stats.recoil;
            if (stats.ammoLimited) {
                player.ammo--;
                if (player.ammo <= 0) {
                    player.weapon = Weapon.PISTOL;
                    player.ammo = -1;
                }
            }
        }

        private void updateBullets(double deltaSeconds) {
            for (ServerBullet bullet : new ArrayList<>(bullets.values())) {
                double oldX = bullet.x;
                double oldY = bullet.y;
                bullet.life -= deltaSeconds;
                bullet.x += bullet.vx * deltaSeconds;
                bullet.y += bullet.vy * deltaSeconds;
                bullet.lastX = oldX;
                bullet.lastY = oldY;

                if (bullet.life <= 0 || bullet.x < -50 || bullet.x > WORLD_WIDTH + 50 || bullet.y < -50 || bullet.y > WORLD_HEIGHT + 50) {
                    bullets.remove(bullet.id);
                    continue;
                }

                for (ServerPlayer player : players.values()) {
                    if (player.id == bullet.ownerId || player.respawnTimer > 0) {
                        continue;
                    }
                    if (bullet.x + 8 >= player.x && bullet.x <= player.x + PLAYER_SIZE
                            && bullet.y + 4 >= player.y && bullet.y <= player.y + PLAYER_SIZE) {
                        player.damage += bullet.damage;
                        player.hitCombo++;
                        player.hitTextTimer = HIT_TEXT_SECONDS;
                        double direction = bullet.vx < 0 ? -1 : 1;
                        double knockback = 170 + player.damage * 5.2;
                        player.vx += direction * knockback;
                        player.vy -= 180 + player.damage * 1.5;
                        player.grounded = false;
                        player.lastHitBy = bullet.ownerId;
                        player.hitFlashTimer = HIT_FLASH_SECONDS;
                        bullets.remove(bullet.id);
                        break;
                    }
                }
            }
        }

        private void movePlayer(ServerPlayer player, double deltaSeconds) {
            double oldY = player.y;
            player.x += player.vx * deltaSeconds;
            player.y += player.vy * deltaSeconds;
            player.grounded = false;

            if (player.x < -20) {
                player.x = -20;
                player.vx = Math.max(0, player.vx);
            } else if (player.x > WORLD_WIDTH - PLAYER_SIZE + 20) {
                player.x = WORLD_WIDTH - PLAYER_SIZE + 20;
                player.vx = Math.min(0, player.vx);
            }

            for (Rectangle platform : PLATFORMS) {
                boolean horizontallyInside = player.x + PLAYER_SIZE > platform.x && player.x < platform.x + platform.width;
                boolean crossedTop = oldY + PLAYER_SIZE <= platform.y && player.y + PLAYER_SIZE >= platform.y;
                if (horizontallyInside && crossedTop && player.vy >= 0) {
                    player.y = platform.y - PLAYER_SIZE;
                    player.vy = 0;
                    player.grounded = true;
                }
            }
        }

        private void updateChests(double deltaSeconds) {
            chestTimer -= deltaSeconds;
            if (chestTimer > 0 || chests.size() >= 3) {
                return;
            }
            chestTimer = CHEST_RESPAWN_SECONDS;
            int chestId = chests.size() + nextBulletId.getAndIncrement();
            int[][] spots = {
                    {210, 455}, {655, 455}, {170, 330}, {690, 330}, {430, 225}, {240, 140}, {610, 140}
            };
            int[] spot = spots[Math.abs(chestId) % spots.length];
            chests.put(chestId, new ServerChest(chestId, spot[0], spot[1]));
        }

        private void collectChests(ServerPlayer player) {
            if (player.respawnTimer > 0 || player.lives <= 0) {
                return;
            }
            for (ServerChest chest : new ArrayList<>(chests.values())) {
                boolean overlaps = player.x + PLAYER_SIZE >= chest.x && player.x <= chest.x + CHEST_SIZE
                        && player.y + PLAYER_SIZE >= chest.y && player.y <= chest.y + CHEST_SIZE;
                if (overlaps) {
                    Weapon weapon = (player.id + chest.id) % 2 == 0 ? Weapon.MACHINE_GUN : Weapon.SNIPER;
                    player.weapon = weapon;
                    player.ammo = statsFor(weapon).ammo;
                    chests.remove(chest.id);
                    chestTimer = Math.min(chestTimer, 2.0);
                    return;
                }
            }
        }

        private void ringOut(ServerPlayer player, Integer scorerId) {
            player.deaths++;
            player.lives = Math.max(0, player.lives - 1);
            int scorer = scorerId == null ? player.lastHitBy : scorerId;
            ServerPlayer scorerPlayer = players.get(scorer);
            if (scorerPlayer != null && scorerPlayer.id != player.id) {
                scorerPlayer.score++;
            }
            player.respawnTimer = RESPAWN_SECONDS;
            player.vx = 0;
            player.vy = 0;
            player.damage = 0;
            player.hitFlashTimer = 0;
            player.hitTextTimer = 0;
            player.hitCombo = 0;
            if (player.lives == 0) {
                player.x = -300;
                player.y = -300;
                player.respawnTimer = 0;
            }
        }

        private void respawn(ServerPlayer player) {
            player.damage = 0;
            player.x = spawnX(player.id);
            player.y = -180 - (player.id % 3) * 80;
            player.facingX = 1;
            player.facingY = 0;
            player.vx = 0;
            player.vy = 90;
            player.grounded = false;
            player.shootCooldown = 0;
            player.hitFlashTimer = 0;
            player.hitTextTimer = 0;
            player.hitCombo = 0;
            player.attackQueued = false;
            player.lastHitBy = -1;
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
                        .append((int) Math.round(player.x)).append(',')
                        .append((int) Math.round(player.y)).append(',')
                        .append(player.color.getRGB()).append(',')
                        .append(player.damage).append(',')
                        .append(player.score).append(',')
                        .append(player.deaths).append(',')
                        .append(player.lives).append(',')
                        .append(player.name).append(',')
                        .append(player.facingX).append(',')
                        .append(player.facingY).append(',')
                        .append(String.format(java.util.Locale.US, "%.2f", player.hitFlashTimer)).append(',')
                        .append(String.format(java.util.Locale.US, "%.2f", player.hitTextTimer)).append(',')
                        .append(player.hitCombo).append(',')
                        .append(player.weapon.displayName).append(',')
                        .append(player.ammo).append(',')
                        .append(String.format(java.util.Locale.US, "%.1f", player.respawnTimer));
            }
            builder.append('|');
            List<Integer> bulletIds = new ArrayList<>(bullets.keySet());
            Collections.sort(bulletIds);
            for (int i = 0; i < bulletIds.size(); i++) {
                ServerBullet bullet = bullets.get(bulletIds.get(i));
                if (bullet == null) {
                    continue;
                }
                if (i > 0) {
                    builder.append(';');
                }
                builder.append((int) Math.round(bullet.lastX)).append(',')
                        .append((int) Math.round(bullet.lastY)).append(',')
                        .append((int) Math.round(bullet.x)).append(',')
                        .append((int) Math.round(bullet.y)).append(',')
                        .append(bullet.color.getRGB());
            }
            builder.append('|');
            List<Integer> chestIds = new ArrayList<>(chests.keySet());
            Collections.sort(chestIds);
            for (int i = 0; i < chestIds.size(); i++) {
                ServerChest chest = chests.get(chestIds.get(i));
                if (chest == null) {
                    continue;
                }
                if (i > 0) {
                    builder.append(';');
                }
                builder.append(chest.x).append(',').append(chest.y);
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

        private static int spawnX(int id) {
            int[] xs = {220, 640, 420, 130, 720, 300, 560};
            return xs[(id - 1) % xs.length];
        }

        private static int spawnY(int id) {
            int[] ys = {450, 450, 220, 325, 325, 135, 135};
            return ys[(id - 1) % ys.length];
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
        private final boolean bot;
        private volatile String name;
        private volatile double x;
        private volatile double y;
        private volatile double vx;
        private volatile double vy;
        private volatile int damage;
        private volatile int score;
        private volatile int deaths;
        private volatile int lives = STARTING_LIVES;
        private volatile Weapon weapon = Weapon.PISTOL;
        private volatile int ammo = -1;
        private volatile int facingX = 1;
        private volatile int facingY;
        private volatile boolean grounded;
        private volatile boolean up;
        private volatile boolean down;
        private volatile boolean left;
        private volatile boolean right;
        private volatile boolean attackHeld;
        private volatile boolean attackQueued;
        private volatile double shootCooldown;
        private volatile double hitFlashTimer;
        private volatile double hitTextTimer;
        private volatile int hitCombo;
        private volatile double respawnTimer;
        private volatile double botThinkTimer;
        private volatile int lastHitBy = -1;

        private ServerPlayer(int id, int x, int y, Color color, boolean bot) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.color = color;
            this.bot = bot;
            this.name = "Player " + id;
        }
    }

    private enum Weapon {
        PISTOL("Pistol"),
        MACHINE_GUN("Machine Gun"),
        SNIPER("Sniper");

        private final String displayName;

        Weapon(String displayName) {
            this.displayName = displayName;
        }
    }

    private record WeaponStats(double cooldown, double bulletSpeed, double life, int damage, double recoil, int ammo, boolean ammoLimited) {
    }

    private static WeaponStats statsFor(Weapon weapon) {
        return switch (weapon) {
            case MACHINE_GUN -> new WeaponStats(0.08, 760.0, 1.4, 7, 35, 45, true);
            case SNIPER -> new WeaponStats(0.82, 1040.0, 1.2, 38, 145, 8, true);
            case PISTOL -> new WeaponStats(SHOOT_COOLDOWN_SECONDS, BULLET_SPEED, BULLET_LIFETIME_SECONDS,
                    DEFAULT_BULLET_DAMAGE, 75, -1, false);
        };
    }

    private static final class ServerBullet {
        private final int id;
        private final int ownerId;
        private final Color color;
        private double x;
        private double y;
        private double vx;
        private double vy;
        private double lastX;
        private double lastY;
        private double life;
        private int damage;

        private ServerBullet(int id, int ownerId, double x, double y, double vx, double vy, double life, Color color, int damage) {
            this.id = id;
            this.ownerId = ownerId;
            this.x = x;
            this.y = y;
            this.lastX = x;
            this.lastY = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.color = color;
            this.damage = damage;
        }
    }

    private static final class ServerChest {
        private final int id;
        private final int x;
        private final int y;

        private ServerChest(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
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
            if ("ADD_BOT".equals(line)) {
                server.addBot();
                return;
            }

            if (line.startsWith("NAME ")) {
                ServerPlayer player = server.players.get(playerId);
                if (player != null) {
                    player.name = sanitizeName(line.substring("NAME ".length()));
                }
                return;
            }

            if (!line.startsWith("INPUT ")) {
                return;
            }

            String[] parts = line.split(" ");
            if (parts.length != 6) {
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
            boolean attack = Boolean.parseBoolean(parts[5]);
            if (attack && !player.attackHeld) {
                player.attackQueued = true;
            }
            player.attackHeld = attack;
        }

        private void send(String message) {
            out.println(message);
        }
    }
}
