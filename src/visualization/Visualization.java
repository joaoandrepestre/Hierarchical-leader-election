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

    public final int SIZE = 0;
    public final int HOPS = 1;
    public final int TOPOLOGY = 2;
    public final int LLEADER = 3;
    public final int LDELTA = 4;
    public final int GLEADER = 5;
    public final int GDELTA = 6;

    public final ActorSystem system = ActorSystem.create("system"); /* Akka actor system */

    public Network net; /* The network to draw */
    public PVector[] points;
    public boolean recording = false;
    public int screenshotCounter = 0;
    public int recordingCounter = 0;
    
    public int menuState = SIZE;
    public int highlightedNode = 0;
    public String input = "";
    public int networkSize;
    public int[][] topologyGraph;
    public int[] globalDeltas;
    public int globalLeader = -1;
    public int[] localDeltas;
    public int[] localLeaders;
    public int maxHops; 

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
        /* final ActorSystem system = ActorSystem.create("system"); /* Akka actor system

        // Example 1: The graph we discussed in our meetings

        int[][] topologyGraph = { { 0, 1, 1, 0, 0, 0 }, { 1, 0, 1, 1, 0, 0 }, { 1, 1, 0, 0, 1, 0 },
                { 0, 1, 0, 0, 1, 0 }, { 0, 0, 1, 1, 0, 1 }, { 0, 0, 0, 0, 1, 0 } };
        int[] globalDeltas = { 3, 3, 2, 2, 1, 0 };
        int globalLeader = 5;
        int[] localDeltas = { 1, 1, 0, 2, 1, 0 };
        int[] localLeaders = { 2, 2, 2, 5, 5, 5 };

        // Exemple 2: Simpler 3 node clique, 0 is the only leader
        
         int[][] topologyGraph = { { 0, 1, 1 }, { 1, 0, 1 }, { 1, 1, 0 } }; int[]
         globalDeltas = { 0, 1, 1 }; int globalLeader = 0; int[] localDeltas = { 0, 1,
         1 }; int[] localLeaders = { 0, 0, 0 };
        

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
        } */
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
        stroke(0);
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
        textSize(12);
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
        textSize(12);
        textAlign(LEFT, CENTER);
        fill(0);
        text("MAX HOPS: " + Network.MAX_HOPS, 20, 20);
        for (Node n : net.nodes) {
            drawNode(n);
        }
    }

    /*
     * Key pressing handler.
     */
    public void keyPressed() {
        if(menuState==SIZE){
            if(key == ENTER){
                menuTransition();
            }
            else if (Character.isDigit(key)){
                input += key;
            }
        }
        else if(menuState==HOPS){
            if(key == ENTER){
                menuTransition();
            }
            else if (Character.isDigit(key)){
                input += key;
            }
        }
        else if(menuState==TOPOLOGY && key == ENTER){
            menuTransition();
        }
        else if(menuState==LLEADER && key == ENTER){
            menuTransition();
        }
        else if(menuState == LDELTA){
            if(key == ENTER){
                menuTransition();
            }
            else if(key == CODED){
                if(keyCode == LEFT){
                    localDeltas[highlightedNode] = Integer.parseInt(input);
                    input = "";
                    highlightedNode = (highlightedNode+1)%networkSize;
                }
                else if(keyCode == RIGHT){
                    localDeltas[highlightedNode] = Integer.parseInt(input);
                    input = "";
                    highlightedNode--;
                    highlightedNode = highlightedNode<0?networkSize+highlightedNode:highlightedNode;
                }
            }
            else if(Character.isDigit(key)){
                input += key;
            }
        }
        else if(menuState == GDELTA){
            if(key == ENTER){
                menuTransition();
            }
            else if(key == CODED){
                if(keyCode == LEFT){
                    globalDeltas[highlightedNode] = Integer.parseInt(input);
                    input = "";
                    highlightedNode = (highlightedNode+1)%networkSize;
                }
                else if(keyCode == RIGHT){
                    globalDeltas[highlightedNode] = Integer.parseInt(input);
                    input = "";
                    highlightedNode--;
                    highlightedNode = highlightedNode<0?networkSize+highlightedNode:highlightedNode;
                }
            }
            else if(Character.isDigit(key)){
                input += key;
            }
        }

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

    public PVector mouseOverEdge(){
        PVector mouse = new PVector(mouseX, mouseY);
        PVector p1;
        PVector p2;
        for (int i = 0; i < networkSize; i++) {
            p1 = points[i];
            for (int j = 0; j < networkSize; j++) {
                if (i != j) {
                    p2 = points[j];
                    if (pointBetweenPoints(p1, p2, mouse)) {
                        return new PVector(i,j);
                    }
                }
            }
        }
        return null;
    }

    public int mouseOverNode(){
        PVector mouse = new PVector(mouseX, mouseY);
        PVector p;
        for(int i=0;i<networkSize;i++){
            p = points[i];
            if(mouse.dist(p) <= 10){
                return i;
            }
        }
        return -1;
    }

    public void mousePressed() {
        PVector ij;
        int n;
        if((ij = mouseOverEdge())!=null){
            int i = (int) ij.x;
            int j = (int) ij.y;
            if(menuState == TOPOLOGY && mouseButton == LEFT){
                topologyGraph[i][j] = 1 - topologyGraph[i][j]; 
                topologyGraph[j][i] = 1 - topologyGraph[j][i]; 
            }
            else if(menuState > GDELTA){
                if (mouseButton == LEFT){
                    topologyGraph[i][j] = 1 - topologyGraph[i][j]; 
                    topologyGraph[j][i] = 1 - topologyGraph[j][i];
                    if(topologyGraph[i][j] == 1){
                        net.dropChannel(i, j);
                    } else{
                        net.remakeChannel(i,j);
                    }
                }
            }
        }
        if((n = mouseOverNode())>=0){
            if(menuState == LLEADER && mouseButton == LEFT){
                localLeaders[highlightedNode] = n;
                highlightedNode = (highlightedNode+1)%networkSize;  
            }
            if(menuState == GLEADER && mouseButton == LEFT){
                globalLeader = n;
                menuTransition();
            }
        }
    }

    public void menuTransition(){
        switch (menuState) {
            case SIZE:
                menuState++;
                networkSize = Integer.parseInt(input);
                input = "";
                points = new PVector[networkSize];
                topologyGraph = new int[networkSize][networkSize];
                globalDeltas = new int[networkSize];
                localDeltas = new int[networkSize];
                localLeaders = new int[networkSize];
                float ang, x, y;
                for (int i = 0; i < networkSize; i++) {
                    ang = PI / 2 + i * (2 * PI / networkSize);
                    x = width / 2 + width/5 * cos(ang);
                    y = height / 2 - width/5 * sin(ang);
                    points[i] = new PVector(x, y);
                    globalDeltas[i] = -1;
                    localDeltas[i] = -1;
                    localLeaders[i] = -1;
                    for(int j=0;j<networkSize;j++){
                        topologyGraph[i][j] = 0;
                    }
                }
                break;
            case HOPS:
                menuState++;
                maxHops = Integer.parseInt(input);
                input = "";
                break;
            case TOPOLOGY:
                menuState++;
                break;
            case LLEADER:
                menuState++;
                highlightedNode = 0;
                input = "";
                break;
            case LDELTA:
                menuState++;
                break;
            case GLEADER:
                menuState++;
                highlightedNode = 0;
                input = "";
                break;
            case GDELTA:
                menuState++;
                net = new Network(system, topologyGraph, globalDeltas, globalLeader, localDeltas, localLeaders, maxHops);
                break;
            default:
                break;
        }
    }

    public boolean nodeIsLocalLeader(int i){
        for(int leader: localLeaders){
            if(leader == i){
                return true;
            }
        }
        return false;
    }

    public void startMenu(){
        PVector p1, p2;
        if(menuState==SIZE){
            fill(0);
            textAlign(CENTER, CENTER);
            textSize(20);
            text("Enter desired number of nodes and press ENTER:", width/2,height/10);
            textSize(50);
            text(input, width/2, height/2);
        }
        else if(menuState == HOPS){
            fill(0);
            textAlign(CENTER, CENTER);
            textSize(20);
            text("Enter desires number for MAX_HOPS and press ENTER:", width/2,height/10);
            textSize(50);
            text(input, width/2, height/2);
        }
        else{
            textSize(12);
            textAlign(LEFT, CENTER);
            fill(0);
            text("MAX HOPS: " + maxHops, 20, 20);
            for(int i=0;i<networkSize;i++){
                p1 = points[i];
                for(int j=0;j<networkSize;j++){
                    if(topologyGraph[i][j]==1){
                        p2 = points[j];
                        stroke(0);
                        line(p1.x, p1.y, p2.x, p2.y);
                    }
                }
            }
            for(int i=0;i<networkSize;i++){
                int nodeColor = 255;
                int textColor = 0;
                p1 = points[i];
                fill(nodeColor);
                if(nodeIsLocalLeader(i)){
                    ellipse(p1.x, p1.y, 25, 25);
                }
                if(i==globalLeader){
                    nodeColor = 0;
                    textColor = 255;
                }
                fill(nodeColor);
                if((menuState == LLEADER || menuState == LDELTA || menuState == GDELTA) && i==highlightedNode){
                    fill(0,255,0);
                }
                ellipse(p1.x, p1.y, 20, 20);
                fill(textColor);
                textAlign(CENTER, CENTER);
                textSize(12);
                text(i, p1.x, p1.y);
                if((menuState == LDELTA || menuState == GDELTA) && i==highlightedNode){
                    if (p1.x >= width / 2) {
                        textAlign(LEFT, CENTER);
                        fill(0);
                        text(input, p1.x + 15, p1.y);
                    } else {
                        textAlign(RIGHT, CENTER);
                        fill(0);
                        text(input, p1.x - 15, p1.y);
                    }
                }
            }
            if(menuState==TOPOLOGY){
                textSize(20);
                textAlign(CENTER, CENTER);
                text("Use your mouse to draw the topology.\nPress ENTER when done", width/2, height/10);
            }
            else if(menuState==LLEADER){
                textSize(20);
                textAlign(CENTER, CENTER);
                text("Click on the local leader of the highlighted node.\nPress ENTER when done", width/2, height/10);
            }
            else if(menuState==LDELTA){
                textSize(20);
                textAlign(CENTER, CENTER);
                text("Enter the local delta of the highlighted node.\nNavigate using <- and ->. Press ENTER when done", width/2, height/10);
            }
            else if(menuState == GLEADER){
                textSize(20);
                textAlign(CENTER, CENTER);
                text("Click on the global leader.", width/2, height/10);
            }
            else if(menuState == GDELTA){
                textSize(20);
                textAlign(CENTER, CENTER);
                text("Enter the global delta of the highlighted node.\nNavigate using <- and ->. Press ENTER when done", width/2, height/10);
            }
        } 
    }

    /*
     * Main processing function. Erases the screen and redraws the network every
     * frame.
     */
    public void draw() {
        background(255);
        PVector edge = mouseOverEdge();
        int node = mouseOverNode();

        if(menuState <= GDELTA){
            if(menuState == TOPOLOGY){
                if(edge != null){
                    PVector p1 = points[(int) edge.x];
                    PVector p2 = points[(int) edge.y];
                    stroke(51); 
                    line(p1.x,p1.y,p2.x,p2.y);
                }
            }
            startMenu();
            if(menuState == LLEADER || menuState == GLEADER){
                if(node >= 0){
                    PVector p = points[node];
                    fill(255,255,0,85);
                    ellipse(p.x, p.y, 20, 20);
                }
            }
        }
        else{
            if(edge != null && topologyGraph[(int) edge.x][(int) edge.y]==0){
                PVector p1 = points[(int) edge.x];
                PVector p2 = points[(int) edge.y];
                stroke(51); 
                line(p1.x,p1.y,p2.x,p2.y);
            }
            drawNetwork();
            if(edge != null && topologyGraph[(int) edge.x][(int) edge.y]==1){
                PVector p1 = points[(int) edge.x];
                PVector p2 = points[(int) edge.y];
                stroke(255,0,0,85); 
                line(p1.x,p1.y,p2.x,p2.y);
            }
        }
        
        if (recording) {
            saveFrame("recordings/simulation" + recordingCounter + "-######.png");
            fill(255, 0, 0);
            ellipse(width - 20, 20, 15, 15);
        }
    }

}