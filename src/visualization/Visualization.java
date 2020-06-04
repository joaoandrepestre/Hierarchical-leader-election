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

    // Possible states for the setup menu
    public final int SIZE = 0; /* The menu is waiting for the input for the size of the network */
    public final int HOPS = 1; /* The menu is waiting for the input for MAX_HOPS */
    public final int TOPOLOGY = 2; /* The menu is waiting for the user to draw the topology */
    public final int LLEADER = 3; /* The menu is waiting for the input for the local leaders */
    public final int LDELTA = 4; /* The menu is waiting for the input for the local deltas */
    public final int GLEADER = 5; /* The menu is waiting for the input for the global leader */
    public final int GDELTA = 6; /* The menu is waiting for the input for the global deltas */

    public final ActorSystem system = ActorSystem.create("system"); /* Akka actor system */

    public Network net; /* The network to draw */
    public PVector[] points; /* Geometrical positions of the nodes */
    public boolean recording = false; /* Flag for recording a video of the simulation. 
                                         While true, will save a sequence of frames of the simulation
                                         in the directory ./recordings */
    public boolean screenshooting = false;
    public int screenshotCounter = 0; /* Counts how many screenshots were taken */
    public int recordingCounter = 0; /* Counts how many recordings of the simulation were done */
    
    public int menuState = SIZE; /* State of the menu */
    public int highlightedNode = 0; /* Node being edited at the moment in the menu */
    public String input = ""; /* Textual input used in the menu */
    public int networkSize; /* Number of nodes in the network */
    public int[][] topologyGraph; /* Graph representing the topology of the network.
                                     If topologyGraph[i][j] is 1, the channel between 
                                     nodes i and j is up */
    public int[] globalDeltas; /* Initial array of distances to the global leader */
    public int globalLeader = -1; /* Global leader of the network */
    public int[] localDeltas; /* Array of initial distances to the local leaders of the network */
    public int[] localLeaders; /* Array of local leaders of the network */
    public int maxHops; /* Maximum number of hops between a node and its local leader */

    /*
     * Initial settings. Creates the window.
     */
    public void settings() {
        size(640, 480);
    }

    /*
     * Checks if the points p3 is between p1 and p2
     * 
     * @param p1 The first point
     * 
     * @param p2 The second point
     * 
     * @param p3 The point that might be in between
     * 
     * @return true if p3 is between p1 and p2
     * 
     * @return false otherwise
     */
    public boolean pointBetweenPoints(PVector p1, PVector p2, PVector p3) {
        float d = p1.dist(p3) + p3.dist(p2);
        float d2 = p1.dist(p2);

        return abs(d - d2) < 0.5;
    }

    /*
     * Draws an arrow from  p1 to p2
     * 
     * @param p1 The first point
     * 
     * @param p2 The second point
     */
    public void drawArrow(PVector p1, PVector p2) {
        if(pointBetweenPoints(p1, p2, new PVector(mouseX, mouseY))){
            stroke(255,0,0);
        }else{
            stroke(0);
        }
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
     * Draws an edge between the nodes at p1 and p2
     * 
     * @param p1 Geometrical position of the highest node 
     * 
     * @param p2 Geometrical position of the lowest node
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
     * 
     * @param n The node to be drawn
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
     * Draws every node in the network
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
            if(key == ENTER && !input.equals("")){
                menuTransition();
            }
            else if(key == BACKSPACE){
                input = input.substring(0, max(0, input.length()-1));
            }
            else if (Character.isDigit(key)){
                input += key;
            }
        }
        else if(menuState==HOPS){
            if(key == ENTER && !input.equals("")){
                menuTransition();
            }
            else if(key == BACKSPACE){
                input = input.substring(0, max(0, input.length()-1));
            }
            else if (Character.isDigit(key)){
                input += key;
            }
        }
        else if(menuState==TOPOLOGY && key == ENTER){
            menuTransition();
        }
        else if(menuState==LLEADER && key == ENTER && isFull(localLeaders)){
            menuTransition();
        }
        else if(menuState == LDELTA){
            if(key == ENTER){
                if(!isFull(localDeltas) || !input.equals("")){
                    if(!input.equals("")){
                        localDeltas[highlightedNode] = Integer.parseInt(input);
                    }
                    input = "";
                    highlightedNode = (highlightedNode+1)%networkSize;
                }else{
                    menuTransition();
                }
            }
            else if(key == BACKSPACE){
                input = input.substring(0, max(0, input.length()-1));
            }
            else if(Character.isDigit(key)){
                input += key;
            }
        }
        else if(menuState == GDELTA){
            if(key == ENTER){
                if(!isFull(globalDeltas) || !input.equals("")){
                    if(!input.equals("")){
                        globalDeltas[highlightedNode] = Integer.parseInt(input);
                    }
                    input = "";
                    highlightedNode = (highlightedNode+1)%networkSize;
                }else{
                    menuTransition();
                }            
            }
            else if(key == BACKSPACE){
                input = input.substring(0, max(0, input.length()-1));
            }
            else if(Character.isDigit(key)){
                input += key;
            }
        }

        if (key == 'p' || key == 'P') {
            screenshooting = true;
        }
        if (key == 'r' || key == 'R') {
            recording = !recording;
            if (!recording)
                recordingCounter++;
        }
    }

    /* 
     * Checks if the mouse is over a possible edge of the network.
     * 
     * @return The pair of id's of the nodes that form the edge qhere the mouse is
     * 
     * @return null if the mouse is not over an edge
     */
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

    /*
     * Checks if the mouse is over a node
     * 
     * @return The id of the node the mouse is over
     * 
     * @return -1 if the mouse is not over any node 
     */
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

    /* 
     * Mouse pressed handler
     */
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
                    if(topologyGraph[i][j] == 1){
                        net.dropChannel(i, j);
                    } else{
                        net.remakeChannel(i,j);
                    }
                    topologyGraph[i][j] = 1 - topologyGraph[i][j]; 
                    topologyGraph[j][i] = 1 - topologyGraph[j][i];
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

    /* 
     * Checks if the array's positions are not negative
     * 
     * @param array The array to be checked
     * 
     * @return true if the array is full
     * 
     * @return false otherwise
     */
    public boolean isFull(int[] array){
        for(int i: array){
            if(i<0) return false;
        }
        return true;
    }

    /* 
     * Changes the state of the menu depending on the current state
     */
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

    /* 
     * Checks if the node is a local leader
     * 
     * @param i The id of the node to be checked
     * 
     * @return true if the node is in the local leaders array
     * 
     * @return false otherwise
     */
    public boolean nodeIsLocalLeader(int i){
        for(int leader: localLeaders){
            if(leader == i){
                return true;
            }
        }
        return false;
    }

    /* 
     * Draws the current state of the start menu
     */
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
                    textColor = 0;
                }
                ellipse(p1.x, p1.y, 20, 20);
                fill(textColor);
                textAlign(CENTER, CENTER);
                textSize(12);
                text(i, p1.x, p1.y);
                if(menuState == LLEADER || menuState == LDELTA || menuState == GDELTA){
                    String s = "";
                    if(i==highlightedNode && !input.equals("")){
                        s = input;
                    } else if(menuState == LDELTA && localDeltas[i]>=0){
                        s += localDeltas[i];
                    } else if(menuState == GDELTA && globalDeltas[i]>=0){
                        s += globalDeltas[i];
                    } else if(menuState == LLEADER && localLeaders[i]>=0){
                        s += localLeaders[i];
                    }
                    if (p1.x >= width / 2) {
                        textAlign(LEFT, CENTER);
                        fill(0);
                        text(s, p1.x + 15, p1.y);
                    } else {
                        textAlign(RIGHT, CENTER);
                        fill(0);
                        text(s, p1.x - 15, p1.y);
                    }
                }
            }
            if(menuState==TOPOLOGY){
                fill(0);
                textSize(20);
                textAlign(CENTER, CENTER);
                text("Use your mouse to draw the topology.\nPress ENTER when done", width/2, height/10);
            }
            else if(menuState==LLEADER){
                fill(0);
                textSize(20);
                textAlign(CENTER, CENTER);
                if(!isFull(localLeaders)){
                    text("Click on the local leader of the highlighted node.", width/2, height/10);
                }else{
                    text("Edit the local leader of the highlighted node.\nPress ENTER if done", width/2, height/10);
                }
            }
            else if(menuState==LDELTA){
                fill(0);
                textSize(20);
                textAlign(CENTER, CENTER);
                if(!isFull(localDeltas)){
                    text("Enter the local delta of the highlighted node.\nPress ENTER to go to next node", width/2, height/10);
                }else if (!input.equals("")){
                    text("Edit the local delta of the highlighted node.\nPress ENTER to go to next node", width/2, height/10);
                }else{
                    text("Edit the local delta of the highlighted node.\nPress ENTER if done", width/2, height/10);
                }
            }
            else if(menuState == GLEADER){
                fill(0);
                textSize(20);
                textAlign(CENTER, CENTER);
                text("Click on the global leader.", width/2, height/10);
            }
            else if(menuState == GDELTA){
                fill(0);
                textSize(20);
                textAlign(CENTER, CENTER);
                if(!isFull(globalDeltas)){
                    text("Enter the global delta of the highlighted node.\nPress ENTER to go to next node", width/2, height/10);
                }else if (!input.equals("")){
                    text("Edit the global delta of the highlighted node.\nPress ENTER to go to next node", width/2, height/10);
                }else{
                    text("Edit the global delta of the highlighted node.\nPress ENTER if done", width/2, height/10);
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
        }

        if(screenshooting){
            screenshooting = false;
            save("../screenshots/simulation" + screenshotCounter + ".png");
            fill(255);
            rect(0,0,width,height);
            screenshotCounter++;
        }
        
        if (recording) {
            saveFrame("../recordings/simulation" + recordingCounter + "-######.png");
            fill(255, 0, 0);
            ellipse(width - 20, 20, 15, 15);
        }
    }

}