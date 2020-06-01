package visualization;

import akka.actor.ActorSystem;
import height.Height;
import network.Network;
import network.Node;
import processing.core.PApplet;
import processing.core.PVector;

/* 
* Visualization class. Extension of processing's PApplet. 
* Creates a visualization window and draws the current state of the network
 */
public class Visualization extends PApplet {

    public Network net; /* The network to draw */
    public PVector[] points;
    public boolean recording = false;
    public int screenshotCounter = 0;
    public int recordingCounter = 0;

    /*
     * Initial settings. Creates the window.
     */
    public void settings() {
        size(640, 480);
    }

    /*
     * Initial setup. Creates the Akka system and the network.
     */
    public void setup() {
        final ActorSystem system = ActorSystem.create("system"); /* Akka actor system */

        // Example 1: The graph we discussed in our meetings

        int[][] topologyGraph = { { 0, 1, 1, 0, 0, 0 }, { 1, 0, 1, 1, 0, 0 }, { 1, 1, 0, 0, 1, 0 },
                { 0, 1, 0, 0, 1, 0 }, { 0, 0, 1, 1, 0, 1 }, { 0, 0, 0, 0, 1, 0 } };
        int[] globalDeltas = { 3, 3, 2, 2, 1, 0 };
        int globalLeader = 5;
        int[] localDeltas = { 1, 1, 0, 2, 1, 0 };
        int[] localLeaders = { 2, 2, 2, 5, 5, 5 };

        // Exemple 2: Simpler 3 node clique, 0 is the only leader
        /*
         * int[][] topologyGraph = { { 0, 1, 1 }, { 1, 0, 1 }, { 1, 1, 0 } }; int[]
         * globalDeltas = { 0, 1, 1 }; int globalLeader = 0; int[] localDeltas = { 0, 1,
         * 1 }; int[] localLeaders = { 0, 0, 0 };
         */

        net = new Network(system, topologyGraph, globalDeltas, globalLeader, localDeltas, localLeaders);

        float x, y, ang;
        int n = topologyGraph.length;
        float r = width / 5;
        points = new PVector[n];
        for (int i = 0; i < n; i++) {
            ang = PI / 2 + i * (2 * PI / n);
            x = width / 2 + r * cos(ang);
            y = height / 2 - r * sin(ang);
            points[i] = new PVector(x, y);
        }
    }

    /*
     * Checks if the points (x3,y3) is between (x1,y1) and (x2,y2)
     */
    public boolean pointBetweenPoints(PVector p1, PVector p2, PVector p3) {
        float d = p1.dist(p3) + p3.dist(p2);
        float d2 = p1.dist(p2);

        return abs(d - d2) < 0.5;
    }

    /*
     * Draws an arrow from (x1,y1) to (x2,y2)
     */
    public void drawArrow(PVector p1, PVector p2) {
        line(p1.x, p1.y, p2.x, p2.y);
        pushMatrix();
        translate(p2.x, p2.y);
        float a = atan2(p1.x - p2.x, p2.y - p1.y);
        rotate(a);
        line(0, 0, -10, -10);
        line(0, 0, 10, -10);
        popMatrix();
    }

    /*
     * Draws an edge between the nodes at (x1, y1) and (x2, y2)
     */
    public void drawEdge(PVector p1, PVector p2) {
        float x1 = p1.x;
        float y1 = p1.y;
        float x2 = p2.x;
        float y2 = p2.y;

        float deltaX = x2 - x1;
        float deltaY = y2 - y1;
        float L = sqrt(deltaX * deltaX + deltaY * deltaY);
        x1 = x1 + deltaX * 10 / L;
        y1 = y1 + deltaY * 10 / L;
        x2 = x2 - deltaX * 10 / L;
        y2 = y2 - deltaY * 10 / L;

        drawArrow(new PVector(x1, y1), new PVector(x2, y2));
    }

    /*
     * Draws the node in a position of the circumference defined by the nodeID.
     * Writes on the screen the nodes height and clock. Draws the edges from this
     * node to its neighbors of smaller height.
     */
    public void drawNode(Node n) {
        int nodeColor = 255;
        int textColor = 0;
        float x = points[n.nodeId].x;
        float y = points[n.nodeId].y;
        fill(255);
        if (n.nodeId == n.localLeaderId) {
            ellipse(x, y, 25, 25);
        }
        if (n.nodeId == n.globalLeaderId) {
            nodeColor = 0;
            textColor = 255;
        }
        fill(nodeColor);
        ellipse(x, y, 20, 20);
        fill(textColor);
        textAlign(CENTER, CENTER);
        text(n.nodeId, x, y);
        fill(0);
        if (x >= width / 2) {
            textAlign(LEFT, CENTER);
            text(n.getHeight().toString() + "\nClock: " + n.causalClock, x + 15, y);
        } else {
            textAlign(RIGHT, CENTER);
            text(n.getHeight().toString() + "\nClock: " + n.causalClock, x - 15, y);
        }
        for (Height h : n.heights) {
            if (h != null && h.compareTo(n.getHeight()) < 0) {
                drawEdge(points[n.nodeId], points[h.nodeId]);
            }
        }
    }

    /*
     * Draws every node
     */
    public void drawNetwork() {
        for (Node n : net.nodes) {
            drawNode(n);
        }
    }

    /*
     * Key pressing handler.
     */
    public void keyPressed() {
        if (key == 'p' || key == 'P') {
            save("screenshots/simulation" + screenshotCounter + ".png");
            fill(255);
            rect(0,0,width,height);
            screenshotCounter++;
        }
        if (key == 'r' || key == 'R') {
            recording = !recording;
            if (!recording)
                recordingCounter++;
        }
    }

    public void mousePressed() {
        PVector mouse = new PVector(mouseX, mouseY);
        PVector p1;
        PVector p2;
        for (int i = 0; i < net.nodes.length; i++) {
            p1 = points[i];
            for (int j = 0; j < net.nodes.length; j++) {
                if (i != j) {
                    p2 = points[j];
                    if (pointBetweenPoints(p1, p2, mouse)) {
                        if (mouseButton == LEFT)
                            net.dropChannel(i, j);
                        if (mouseButton == RIGHT)
                            net.remakeChannel(i, j);
                    }
                }
            }
        }
    }

    /*
     * Main processing function. Erases the screen and redraws the network every
     * frame.
     */
    public void draw() {
        background(255);
        drawNetwork();
        if (recording) {
            saveFrame("recordings/simulation" + recordingCounter + "-######.png");
            fill(255, 0, 0);
            ellipse(width - 20, 20, 15, 15);
        }
    }

}