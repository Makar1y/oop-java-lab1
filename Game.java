import processing.core.PApplet;
import processing.core.PImage;

public class Game extends PApplet {

    // --- App "screens" ---
    private enum Screen {
        MAIN_MENU,
        PLAY,
        MAP_EDITOR
    }

    private Screen screen = Screen.MAIN_MENU;

    // --- Menu buttons (computed each frame from window size) ---
    private float playX, playY, playW, playH;
    private float editorX, editorY, editorW, editorH;

    public int tileSize = 32;
    public int tRows;
    public int tCollumns;
    public PImage[] tilesArray;
    public PImage tilesImage;

    public void fillTiles() {
        for (int i = 0; i < tRows; i++) {
            for (int j = 0; j < tCollumns; j++) {
                tilesArray[tCollumns * i + j] = tilesImage.get(tileSize * j, tileSize * i, tileSize, tileSize);
            }
        }
    }

    public void settings() {
        size(displayWidth / 3 * 2, displayHeight / 3 * 2);
    }

    public void setup() {
        tilesImage = loadImage("tiles.png");
        tRows = tilesImage.height / tileSize;
        tCollumns = tilesImage.width / tileSize;
        tilesArray = new PImage[tRows * tCollumns];
        fillTiles();

        textAlign(CENTER, CENTER);
        rectMode(CORNER);
    }

    public void draw() {
        switch (screen) {
            case MAIN_MENU -> drawMainMenu();
            case PLAY -> drawPlay();
            case MAP_EDITOR -> drawMapEditor();
        }
    }

    private void drawMainMenu() {
        background(200);

        fill(25);
        textSize(48);
        text("Main menu", width / 2f, height * 0.22f);

        // Button layout
        playW = width * 0.3f;
        playH = width / 15f;
        playX = width / 2f - playW / 2f;
        playY = height * 0.40f;

        editorW = playW;
        editorH = playH;
        editorX = playX;
        editorY = playY + playH + 25;

        drawButton(playX, playY, playW, playH, "Play");
        drawButton(editorX, editorY, editorW, editorH, "Map editor");
    }

    private void drawPlay() {
    }

    private void drawMapEditor() {
        background(200);

        for (int i = 0; i < tRows * tCollumns; i++) {
            int spaceAround = 10;
            float x = (width - tileSize * tRows * tCollumns - tRows * tCollumns * spaceAround) / 2f + i * tileSize + i * spaceAround;
            float y = 10;
            int rectCorrection = spaceAround / 2;

            image(tilesArray[i], x, y);
            fill(0, 0, 0, 1);
            rect(x-rectCorrection, y-rectCorrection, tileSize + 2 * rectCorrection, tileSize + 2 * rectCorrection);
        }
    }

    private void drawButton(float x, float y, float w, float h, String label) {
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
        }
    }

    private boolean isInside(float px, float py, float x, float y, float w, float h) {
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

        }
    }

    public static void main(String[] args) {
        PApplet.main("Game");
    }
}