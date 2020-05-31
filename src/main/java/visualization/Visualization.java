package visualization;

import akka.actor.ActorSystem;
import height.Height;
import network.Network;
import network.Node;
import processing.core.PApplet;

/* 
* Visualization class. Extension of processing's PApplet. 
* Creates a visualization window and draws the current state of the network
 */
public class Visualization extends PApplet {

    public Network net; /* The network to draw */
    public int clickCounter = 0;

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

        int[][] topologyGraph = { { 0, 1, 1, 0, 0, 0 }, 
                                  { 1, 0, 1, 1, 0, 0 }, 
                                  { 1, 1, 0, 0, 1, 0 },
                                  { 0, 1, 0, 0, 1, 0 }, 
                                  { 0, 0, 1, 1, 0, 1 }, 
                                  { 0, 0, 0, 0, 1, 0 } };
        int[] globalDeltas = { 3, 3, 2, 2, 1, 0 };
        int globalLeader = 5;
        int[] localDeltas = { 1, 1, 0, 2, 1, 0 };
        int[] localLeaders = { 2, 2, 2, 5, 5, 5 };

        // Exemple 2: Simpler 3 node clique, 0 is the only leader
        /*int[][] topologyGraph = { { 0, 1, 1 }, 
                                   { 1, 0, 1 }, 
                                   { 1, 1, 0 } }; 
         int[] globalDeltas = { 0, 1, 1 }; int globalLeader = 0; int[] localDeltas = { 0, 1, 1 }; 
         int[] localLeaders = { 0, 0, 0 };*/

        net = new Network(system, topologyGraph, globalDeltas, globalLeader, localDeltas, localLeaders);
    }

    /*
     * Draws an arrow from (x1,y1) to (x2,y2)
     */
    public void drawArrow(float x1, float y1, float x2, float y2) {
        line(x1, y1, x2, y2);
        pushMatrix();
        translate(x2, y2);
        float a = atan2(x1 - x2, y2 - y1);
        rotate(a);
        line(0, 0, -10, -10);
        line(0, 0, 10, -10);
        popMatrix();
    }

    /*
     * Draws an edge between the nodes at (x1, y1) and (x2, y2)
     */
    public void drawEdge(float x1, float y1, float x2, float y2) {
        float deltaX = x2 - x1;
        float deltaY = y2 - y1;
        float L = sqrt(deltaX * deltaX + deltaY * deltaY);
        x1 = x1 + deltaX * 10 / L;
        y1 = y1 + deltaY * 10 / L;
        x2 = x2 - deltaX * 10 / L;
        y2 = y2 - deltaY * 10 / L;

        drawArrow(x1, y1, x2, y2);
    }

    /*
     * Draws the node in a position of the circumference defined by the nodeID.
     * Writes on the screen the nodes height and clock. Draws the edges from this
     * node to its neighbors of smaller height.
     */
    public void drawNode(Node n) {
        int nodeColor = 255;
        int textColor = 0;
        float r = width / 5;
        float ang = PI / 2 + n.nodeId * (2 * PI / net.nodes.length);
        float x = width / 2 + r * cos(ang);
        float y = height / 2 - r * sin(ang);
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
                ang = PI / 2 + h.nodeId * (2 * PI / net.nodes.length);
                float x2 = width / 2 + r * cos(ang);
                float y2 = height / 2 - r * sin(ang);
                drawEdge(x, y, x2, y2);
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
        if (key == ENTER) {
            switch (clickCounter) {
                case 0:
                    net.dropChannel(0, 2);
                    break;
                case 1:
                    net.dropChannel(4, 5);
                    break;
                case 2:
                    net.remakeChannel(0, 2);
                    break;
                default:
                    exit();
            }
            clickCounter++;
        }
    }

    /*
     * Main processing function. Erases the screen and redraws the network every
     * frame.
     */
    public void draw() {
        background(255);
        drawNetwork();
    }

}