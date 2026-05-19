# Swing LAN Multiplayer

A tiny Java Swing local multiplayer prototype. One computer hosts, other computers on the same Wi-Fi join using the host's local IP address.

## Run in IntelliJ

1. Open this folder in IntelliJ.
2. Open `src/main/java/com/example/lan/LanMultiplayerGame.java`.
3. Click the green run button next to `main`.

## Play

- On the host computer, click `Host Game`.
- On another computer, run the same app, click `Refresh List`, select the hosted game, and click `Join Selected`.
- If the host does not appear, enter the host computer's local IP and click `Join IP`.
- Move with `WASD` or arrow keys.
- The game uses TCP port `5050` for gameplay and UDP port `5051` for LAN discovery.

If joining fails, allow Java through the host computer's firewall and make sure both devices are on the same Wi-Fi network. Some networks block UDP broadcast, so manual IP join is still available as a fallback.
