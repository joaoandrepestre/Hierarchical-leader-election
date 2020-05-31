package visualization;

import processing.core.PApplet;

public class Visualization extends PApplet {

    public void settings() {
        size(640, 480);
    }

    public void drawNetwork(int n) {
        int r = 200;
        float x = width / 2;
        float y = height / 2 - r;
        float ang = PI / 2;
        float dang = 2 * PI / n;
        fill(255);
        for (int i = 0; i < n; i++) {
            ellipse(x, y, 10, 10);
            ang += dang;
            x = width/2 + r*cos(ang);
            y = height/2 - r*sin(ang);
        }
    }

    public void draw() {
        background(51);
        drawNetwork(10);
    }

}