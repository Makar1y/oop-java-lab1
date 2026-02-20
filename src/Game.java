import processing.core.PApplet;
import processing.core.PImage;

import javax.swing.*;

public class Game extends PApplet {

    enum Screen {
        MAIN_MENU,
        PLAY,
        MAP_EDITOR
    }

    Screen screen = Screen.MAIN_MENU;

    float playX, playY, playW, playH;
    float editorX, editorY, editorW, editorH;

    int tileSize = 32;
    int tRows;
    int tCollumns;
    PImage[] tilesArray;
    PImage tilesImage;
    PImage playerImage;
    PImage playerIdle;

    // Map editor
    public int mapCols = 50;
    public int mapRows = 50;
    int emptyTile = -1;
    int[][] map = new int[mapRows][mapCols];
    int selectedTile = 0;
    int sidebarW = 300;
    int mapViewX = 0;
    int mapViewY = 0;
    int paletteX;
    int paletteY;

    // Palette
    int paletteInnerX;
    int paletteInnerY;
    int paletteInnerW;
    int pad = 6;
    int preview = 32;

    // Map editor grid rendering params
    int viewTileSize = 32;
    int mapDrawX = 0;
    int mapDrawY = 0;

    //
    int spawnTile = 3;
    int GroundTile = 19;
    int ExitTile = 11;
    int viewDistance = 7; // Tiles visible from the player
    int[] spawnCoordinates = new int[] {-1,-1};
    int playerX, PlayerY;

    enum PlayState {
        RUNNING,
        LOST,
        WON
    }
    PlayState playState = PlayState.RUNNING;
    final int DIR_DOWN = 0;
    final int DIR_UP = 1;     // "forward"
    final int DIR_LEFT = 2;
    final int DIR_RIGHT = 3;

    int animFrame = 0;
    int playerDir = DIR_DOWN;
    int lastMoveFrameCount = 0;
    final int moveEveryNFrames = 7;

    PImage[][] playerFrames;
    final int PLAYER_FRAMES_X = 8;
    final int PLAYER_FRAMES_Y = 4;

    PImage[][] idleFrames;
    final int IDLE_FRAMES_X = 6;
    final int IDLE_FRAMES_Y = 4;

    int walkAnimFrame = 0;
    int idleAnimFrame = 0;

    float playerRenderX, playerRenderY;

    boolean isMoving = false;
    int moveFromX, moveFromY, moveToX, moveToY;
    int moveStartMs = 0;
    final int moveDurationMs = 120;

    int lastAnimMs = 0;
    final int walkFrameMs = 90; // ms
    final int idleFrameMs = 140; // ms

    float camRenderX, camRenderY;
    final float camFollow = 0.35f;

    void initEditor() {
        map = new int[mapRows][mapCols];
        for (int r = 0; r < mapRows; r++) {
            for (int c = 0; c < mapCols; c++) {
                map[r][c] = emptyTile;
            }
        }
    }

    void fillTiles() {
        for (int i = 0; i < tRows; i++) {
            for (int j = 0; j < tCollumns; j++) {
                tilesArray[tCollumns * i + j] = tilesImage.get(tileSize * j, tileSize * i, tileSize, tileSize);
            }
        }
    }

    void slicePlayerFrames() {
        int frameW = playerImage.width / PLAYER_FRAMES_X;
        int frameH = playerImage.height / PLAYER_FRAMES_Y;

        playerFrames = new PImage[PLAYER_FRAMES_Y][PLAYER_FRAMES_X];
        for (int dir = 0; dir < PLAYER_FRAMES_Y; dir++) {
            for (int f = 0; f < PLAYER_FRAMES_X; f++) {
                playerFrames[dir][f] = playerImage.get(f * frameW, dir * frameH, frameW, frameH);
            }
        }
    }

    void sliceIdleFrames() {
        int frameW = playerIdle.width / IDLE_FRAMES_X;
        int frameH = playerIdle.height / IDLE_FRAMES_Y;

        idleFrames = new PImage[IDLE_FRAMES_Y][IDLE_FRAMES_X];
        for (int dir = 0; dir < IDLE_FRAMES_Y; dir++) {
            for (int f = 0; f < IDLE_FRAMES_X; f++) {
                idleFrames[dir][f] = playerIdle.get(f * frameW, dir * frameH, frameW, frameH);
            }
        }
    }

    public void settings() {
        //        size(displayWidth / 3 * 2, displayHeight / 3 * 2);
        fullScreen();
    }

    public void setup() {
        tilesImage = loadImage("imgs/tiles.png");
        playerImage = loadImage("imgs/player.png");
        playerIdle = loadImage("imgs/player_idle.png");

        tRows = tilesImage.height / tileSize;
        tCollumns = tilesImage.width / tileSize;
        tilesArray = new PImage[tRows * tCollumns];
        fillTiles();

        slicePlayerFrames();
        sliceIdleFrames(); // NEW

        initEditor();

        textAlign(CENTER, CENTER);
        rectMode(CORNER);
    }

    public void draw() {
        switch (screen) {
            case MAIN_MENU -> drawMainMenu();
            case PLAY -> Play();
            case MAP_EDITOR -> drawMapEditor();
        }
    }

    void drawMainMenu() {
        background(200);

        textAlign(CENTER, CENTER);
        fill(25);
        textSize(48);
        text("Main menu", width / 2f, height * 0.22f);

        // Button layout
        playW = width * 0.2f;
        playH = playW / 5f;
        playX = width / 2f - playW / 2f;
        playY = height * 0.40f;

        editorW = playW;
        editorH = playH;
        editorX = playX;
        editorY = playY + playH + 25;

        drawButton(playX, playY, playW, playH, "Play");
        drawButton(editorX, editorY, editorW, editorH, "Map editor");
    }

    void resetPlay() {
        spawnCoordinates[0] = -1;
        spawnCoordinates[1] = -1;
        playerX = 0;
        PlayerY = 0;
        playerDir = DIR_DOWN;
        animFrame = 0;
        lastMoveFrameCount = 0;

        isMoving = false;
        playerRenderX = playerX;
        playerRenderY = PlayerY;

        camRenderX = playerRenderX;
        camRenderY = playerRenderY;

        lastAnimMs = millis();
        walkAnimFrame = 0;
        idleAnimFrame = 0;

        Play();
    }

    void voidStartPlay() {
        for (int r = 0; r < mapRows; r++) {
            for (int c = 0; c < mapCols; c++) {
                int id = map[r][c];
                if (id == spawnTile) {
                    playerX = c;
                    PlayerY = r;
                    spawnCoordinates[0] = c;
                    spawnCoordinates[1] = r;
                    playState = PlayState.RUNNING;

                    isMoving = false;
                    playerRenderX = playerX;
                    playerRenderY = PlayerY;

                    camRenderX = playerRenderX;
                    camRenderY = playerRenderY;

                    lastAnimMs = millis();
                    walkAnimFrame = 0;
                    idleAnimFrame = 0;
                    return;
                }
            }
        }

        spawnCoordinates[0] = playerX;
        spawnCoordinates[1] = PlayerY;

        isMoving = false;
        playerRenderX = playerX;
        playerRenderY = PlayerY;

        camRenderX = playerRenderX;
        camRenderY = playerRenderY;

        lastAnimMs = millis();
        walkAnimFrame = 0;
        idleAnimFrame = 0;
    }

    void Play() {
        if (spawnCoordinates[0] == -1) voidStartPlay();

        if (playState == PlayState.RUNNING) {
            handleMovement();
            updateSmoothMovementAndAnimation();
            updateCamera();
            checkWinLose();
        }

        drawPlayWorld();

        if (playState != PlayState.RUNNING) {
            drawEndOverlay();
        }
    }

    void updateCamera() {
        camRenderX = lerp(camRenderX, playerRenderX, camFollow);
        camRenderY = lerp(camRenderY, playerRenderY, camFollow);
    }

    void handleMovement() {
        int dx = 0, dy = 0;

        if (keyPressed) {
            if (keyCode == UP || key == 'w' || key == 'W') {
                dy = -1;
                playerDir = DIR_UP;
            } else if (keyCode == DOWN || key == 's' || key == 'S') {
                dy = 1;
                playerDir = DIR_DOWN;
            } else if (keyCode == LEFT || key == 'a' || key == 'A') {
                dx = -1;
                playerDir = DIR_LEFT;
            } else if (keyCode == RIGHT || key == 'd' || key == 'D') {
                dx = 1;
                playerDir = DIR_RIGHT;
            }
        }

        if (isMoving) return;

        if (dx == 0 && dy == 0) return;

        int nx = playerX + dx;
        int ny = PlayerY + dy;

        if (nx < 0 || nx >= mapCols || ny < 0 || ny >= mapRows) return;

        moveFromX = playerX;
        moveFromY = PlayerY;
        moveToX = nx;
        moveToY = ny;
        moveStartMs = millis();
        isMoving = true;

        playerX = nx;
        PlayerY = ny;
    }

    void updateSmoothMovementAndAnimation() {
        int now = millis();

        if (isMoving) {
            float t = (now - moveStartMs) / (float) moveDurationMs;
            if (t >= 1f) {
                t = 1f;
                isMoving = false;
            }

            // Smoothstep easing (less robotic than linear)
            float eased = t * t * (3f - 2f * t);

            playerRenderX = lerp(moveFromX, moveToX, eased);
            playerRenderY = lerp(moveFromY, moveToY, eased);

            if (now - lastAnimMs >= walkFrameMs) {
                walkAnimFrame = (walkAnimFrame + 1) % PLAYER_FRAMES_X;
                lastAnimMs = now;
            }
        } else {
            playerRenderX = playerX;
            playerRenderY = PlayerY;

            if (now - lastAnimMs >= idleFrameMs) {
                idleAnimFrame = (idleAnimFrame + 1) % IDLE_FRAMES_X;
                lastAnimMs = now;
            }
        }
    }

    void checkWinLose() {
        int id = map[PlayerY][playerX];
        if (id == emptyTile) {
            playState = PlayState.LOST;
        } else if (id == ExitTile) {
            playState = PlayState.WON;
        }
    }

    void drawPlayWorld() {
        background(0);

        int radius = viewDistance;
        int diameterTiles = radius * 2 + 1;
        int gameTileSize = min(width, height) / max(1, diameterTiles);

        float originX = width / 2f - gameTileSize / 2f - radius * gameTileSize;
        float originY = height / 2f - gameTileSize / 2f - radius * gameTileSize;

        float camX = camRenderX;
        float camY = camRenderY;

        int centerTileX = (int) floor(camX);
        int centerTileY = (int) floor(camY);

        float fracX = camX - centerTileX;
        float fracY = camY - centerTileY;

        for (int dy = -radius - 1; dy <= radius + 1; dy++) {
            for (int dx = -radius - 1; dx <= radius + 1; dx++) {

                int tx = centerTileX + dx;
                int ty = centerTileY + dy;

                float sx = originX + (dx + radius - fracX) * gameTileSize;
                float sy = originY + (dy + radius - fracY) * gameTileSize;

                int id = emptyTile;
                if (tx >= 0 && tx < mapCols && ty >= 0 && ty < mapRows) {
                    id = map[ty][tx];
                }

                if (id >= 0) {
                    image(tilesArray[id], sx, sy, gameTileSize, gameTileSize);
                }

                int dist = abs(dx) + abs(dy);
                float vis = constrain(1f - (dist / (float) radius), 0f, 1f);

                float alpha = (1f - vis) * 255f;
                noStroke();
                fill(0, alpha);
                rect(sx, sy, gameTileSize, gameTileSize);
            }
        }

        drawPlayer(width / 2 - gameTileSize / 2, height / 2 - gameTileSize / 2, gameTileSize);
    }

    void drawPlayer(int x, int y, int sizePx) {
        int fx;
        int fy = constrain(playerDir, 0, 3);

        if (isMoving) {
            fx = constrain(walkAnimFrame, 0, PLAYER_FRAMES_X - 1);
            image(playerFrames[fy][fx], x, y, sizePx, sizePx);
        } else {
            fx = constrain(idleAnimFrame, 0, IDLE_FRAMES_X - 1);
            image(idleFrames[fy][fx], x, y, sizePx, sizePx);
        }
    }

    void drawEndOverlay() {
        fill(0, 170);
        noStroke();
        rect(0, 0, width, height);

        fill(255);
        textAlign(CENTER, CENTER);
        textSize(52);

        if (playState == PlayState.WON) text("YOU WIN", width / 2f, height / 2f - 30);
        if (playState == PlayState.LOST) text("YOU LOSE", width / 2f, height / 2f - 30);

        textSize(18);
        text("Press R to restart, ESC for menu", width / 2f, height / 2f + 30);
    }

    void drawMapEditor() {
        int mapViewW = width - sidebarW;
        int mapViewH = height;

        //
        int fitTileW = Math.max(1, mapViewW / mapCols);
        int fitTileH = Math.max(1, mapViewH / mapRows);
        viewTileSize = Math.min(fitTileW, fitTileH);

        int mapPixelW = mapCols * viewTileSize;
        int mapPixelH = mapRows * viewTileSize;

        // Center map inside the view
        mapDrawX = mapViewX + (mapViewW - mapPixelW) / 2;
        mapDrawY = mapViewY + (mapViewH - mapPixelH) / 2;


        // Background
        background(200);
        noStroke();
        fill(235);
        rect(mapViewX, mapViewY, mapViewW, mapViewH);

        // Draw tiles
        for (int r = 0; r < mapRows; r++) {
            for (int c = 0; c < mapCols; c++) {
                int id = map[r][c];
                if (id != emptyTile) {
                    int sx = mapDrawX + c * viewTileSize;
                    int sy = mapDrawY + r * viewTileSize;
                    image(tilesArray[id], sx, sy, viewTileSize, viewTileSize);
                }
            }
        }

        // Grid
        stroke(0, 40);
        for (int c = 0; c <= mapCols; c++) {
            int x = mapDrawX + c * viewTileSize;
            line(x, mapDrawY, x, mapDrawY + mapPixelH);
        }
        for (int r = 0; r <= mapRows; r++) {
            int y = mapDrawY + r * viewTileSize;
            line(mapDrawX, y, mapDrawX + mapPixelW, y);
        }

        // Sidebar / palette
        paletteX = width - sidebarW;
        paletteY = 0;

        noStroke();
        fill(220);
        rect(paletteX, paletteY, sidebarW, height);

        fill(20);
        textSize(18);
        textAlign(LEFT, TOP);
        text("Palette", paletteX + 12, 12);
        textSize(12);
        text("LMB paint / RMB erase\nS save / L load\nEsc back", paletteX + 12, 40);

        paletteInnerX = paletteX + 12;
        paletteInnerY = 140;
        paletteInnerW = sidebarW - 12*2;

        drawPalette(paletteInnerX, paletteInnerY, paletteInnerW);
    }

    void drawPalette(int x, int y, int w) {
        int cols = Math.max(1, w / (preview + pad));

        for (int i = 0; i < tilesArray.length; i++) {
            int cx = i % cols;
            int cy = i / cols;

            int px = x + cx * (preview + pad);
            int py = y + cy * (preview + pad);

            // Highlight
            stroke(i == selectedTile ? color(255, 140, 0) : color(0, 60));
            strokeWeight(i == selectedTile ? 3 : 1);
            fill(255);
            rect(px - 2, py - 2, preview + 4, preview + 4);

            // Draw tile
            image(tilesArray[i], px, py, preview, preview);
        }
    }

    void paintAtMouse(boolean erase) {
        int mapPixelW = mapCols * viewTileSize;
        int mapPixelH = mapRows * viewTileSize;

        // Click not inside the map rectangle
        if (mouseX < mapDrawX || mouseX >= mapDrawX + mapPixelW) return;
        if (mouseY < mapDrawY || mouseY >= mapDrawY + mapPixelH) return;

        int localX = mouseX - mapDrawX;
        int localY = mouseY - mapDrawY;

        int col = localX / viewTileSize;
        int row = localY / viewTileSize;

        if (row < 0 || row >= mapRows || col < 0 || col >= mapCols) return;

        map[row][col] = erase ? emptyTile : selectedTile;
    }

    void pickTileAtMouse() {
        int x = paletteInnerX;
        int y = paletteInnerY;
        int w = paletteInnerW;
        int cols = Math.max(1, w / (preview + pad));

        int localX = mouseX - x;
        int localY = mouseY - y;
        if (localX < 0 || localY < 0) return;

        int cx = localX / (preview + pad);
        int cy = localY / (preview + pad);
        int idx = cy * cols + cx;

        if (idx >= 0 && idx < tilesArray.length) {
            selectedTile = idx;
        }
    }

    void drawButton(float x, float y, float w, float h, String label) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

        stroke(30);
        strokeWeight(2);
        fill(hover ? color(230) : color(245));
        rect(x, y, w, h, 14);

        fill(20);
        textSize(22);
        text(label, x + w / 2f, y + h / 2f);
    }

    public void mousePressed() {
        if (screen == Screen.MAIN_MENU) {
            if (isInside(mouseX, mouseY, playX, playY, playW, playH)) {
                screen = Screen.PLAY;
            } else if (isInside(mouseX, mouseY, editorX, editorY, editorW, editorH)) {
                screen = Screen.MAP_EDITOR;
            }
        } else if (screen == Screen.MAP_EDITOR) {
            boolean inSidebar = isInside(mouseX, mouseY, paletteX, paletteY, sidebarW, height);
            if (inSidebar) {
                pickTileAtMouse();
            } else {
                paintAtMouse(mouseButton == RIGHT);
            }
        }
    }

    public void mouseDragged() {
        if (screen == Screen.MAP_EDITOR) {
            boolean inSidebar = isInside(mouseX, mouseY, paletteX, paletteY, sidebarW, height);
            if (!inSidebar) {
                paintAtMouse(mouseButton == RIGHT);
            }
        }
    }

    boolean isInside(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public void keyPressed() {
        if (key == ESC) {
            key = 0; // prevent Processing from quitting
            if (screen != Screen.MAIN_MENU) {
                screen = Screen.MAIN_MENU;
            } else {
                System.exit(0);
            }
        } else if (screen == Screen.PLAY) {
            if (key == 'r' || key == 'R') {
                resetPlay();
            }
        }else if (screen == Screen.MAP_EDITOR) {
            if (key == 's' || key == 'S') {
                String fileName = JOptionPane.showInputDialog("Enter map(file) name(without extension):") + ".csv";
                saveMapCSV(fileName);
                JOptionPane.showMessageDialog(null, "Saved successfully");
            } else if (key == 'l' || key == 'L') {
                String fileName = JOptionPane.showInputDialog("Enter map(file) name(without extension):") + ".csv";
                loadMapCSV(fileName);
            }
        }
    }

    void saveMapCSV(String filename) {
        String[] lines = new String[mapRows];
        for (int r = 0; r < mapRows; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < mapCols; c++) {
                if (c > 0) sb.append(',');
                sb.append(map[r][c]);
            }
            lines[r] = sb.toString();
        }
        saveStrings(filename, lines);
    }

    void loadMapCSV(String filename) {
        String[] lines = loadStrings(filename);
        if (lines == null) {
            JOptionPane.showMessageDialog(null, "Not found");
            return;
        }

        int rows = Math.min(lines.length, mapRows);
        for (int r = 0; r < rows; r++) {
            String[] parts = split(lines[r], ',');
            int cols = Math.min(parts.length, mapCols);
            for (int c = 0; c < cols; c++) {
                map[r][c] = parseInt(parts[c]);
            }
        }
        JOptionPane.showMessageDialog(null, "Loaded successfully");
    }

    public static void main(String[] args) {
        PApplet.main("Game");
    }
}