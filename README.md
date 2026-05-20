# LAN Platform Mayhem

A tiny Java Swing LAN platform shooter prototype inspired by arena games where players use guns to knock each other off the map. One computer hosts, other computers on the same Wi-Fi can discover and join the hosted game.

## Run in IntelliJ

1. Open this folder in IntelliJ.
2. Open `src/main/java/com/example/lan/LanMultiplayerGame.java`.
3. Click the green run button next to `main`.

## Play

- On the host computer, click `Host Game`.
- The host can pick the starting map before hosting.
- On another computer, run the same app, click `Refresh List`, select the hosted game, and click `Join Selected`.
- If the host does not appear, enter the host computer's local IP and click `Join IP`.
- Enter a player name before hosting or joining.
- Move with `A/D` or left/right arrows.
- Jump with `W` or up arrow.
- Press jump again in the air to double jump.
- Fast-fall with `S` or down arrow.
- Shoot with `Space` or `J`.
- Hold shoot with the Machine Gun for automatic fire.
- Click `Leave` to return to the menu.
- Click `Add Bot` while in-game to spawn a simple AI opponent for testing.
- Click `Add Dummy` to spawn a movementless target for weapon testing.
- Pick up chests to get a Machine Gun or Sniper with limited ammo.
- Pistol is slower with medium knockback, Machine Gun is automatic with medium knockback, and Sniper is slow but wildly powerful.
- Knock other players off the platforms to score. Each player has 5 lives and respawns after a short delay until they run out.
- Open `Settings` to change movement and shooting keybinds.
- The host can change the map during the match with the map dropdown.
- The game uses TCP port `5050` for gameplay and UDP port `5051` for LAN discovery.

If joining fails, allow Java through the host computer's firewall and make sure both devices are on the same Wi-Fi network. Some networks block UDP broadcast, so manual IP join is still available as a fallback.

## Quality of Life

- The menu shows your LAN IP to make manual joining easier.
- Hosted games auto-refresh every few seconds while the menu is open.
- You can double-click a hosted game to join it.
- The in-game display stays minimal: player list plus KO count, chests, tracer bullets, and simple character weapon size.
- Respawns drop players from above the screen, with a small arrow until they fall into view.
