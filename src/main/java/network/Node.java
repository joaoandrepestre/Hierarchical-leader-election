package network;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Arrays;
import java.lang.Math;

import height.Height;
import height.ReferenceLevel;
import events.*;

class NodeActor extends UntypedAbstractActor {

    private Node n;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this); /* Akka logger */

    public NodeActor(Node n){
        this.n = n;
        log.info("Created node {} with height {}", n.nodeId, n.heights[n.nodeId]);
    }
    
    public static Props createActor(Node n) {
        return Props.create(NodeActor.class, () -> {
            return new NodeActor(n);
        });
    }

    /*
     * Logs the full state of the node. That is, its height, its forming and
     * neighbor sets and its causal clock.
     */
    public void logState() {
        String s = "Height: " + n.heights[n.nodeId] + "\n";
        s += "forming: ";
        for (ActorRef channel : n.forming) {
            if (channel != null) {
                s += channel.path().name() + "; ";
            }
        }
        s += "\nneighbors: ";
        for (ActorRef channel : n.neighbors) {
            if (channel != null) {
                s += channel.path().name() + "; ";
            }
        }
        s += "\nClock: " + n.causalClock;
        log.info("\n[{}]: {}", getSelf().path().name(), s);
    }

    /*
     * Called when a message is received by the actor. Event handlers defined bellow.
     * 
     * @param message The received message
     */
    @Override
    public void onReceive(Object message) throws Throwable {
        Event e = (Event) message;
        n.causalClock = Math.max(n.causalClock, e.timestamp) + 1;
        if (e instanceof ChannelDown) {
            ChannelDown chdown = (ChannelDown) e;
            log.info("\n[{}]: Received {}", getSelf().path().name(), chdown);
            n.handleChannelDown(chdown);
            logState();
        } else if (e instanceof ChannelUp) {
            ChannelUp chup = (ChannelUp) e;
            log.info("\n[{}]: Received {}", getSelf().path().name(), chup);
            n.handleChannelUp(chup);
            logState();
        } else if (e instanceof Update) {
            Update u = (Update) e;
            log.info("\n[{}]: Received {}", getSelf().path().name(), u);
            n.handleUpdate(u);
            logState();
        } else if (e instanceof SetUp) {
            SetUp sup = (SetUp) e;
            log.info("\n[{}]: Received {}", getSelf().path().name(), sup);
            n.handleSetUp(sup);
            logState();
        }
    }

}

/*
 * Node class. Implements the computing node part of the network model. Receives
 * messages from channels and runs the algorithm by handling this events
 */
public class Node {
    private final int MAX_HOPS = 2; /* Constant maximum number of hops between any node and its local leader */

    public int nodeId; /* Id of the node */
    public ActorRef[] forming; /* Set of channels that are up but that haven't sent any messages yet */
    public ActorRef[] neighbors; /* Set of channels that are up and have sent messages */
    public int globalLeaderId; /* Id of the global leader */
    public int localLeaderId; /* Id of the local leader of this node */
    public int causalClock; /* Causal clock used to time events. Lamport's logical clock algorithm used */
    public Height[] heights; /* Set of heights of neighbor nodes */

    public ActorRef nodeActor;

    /*
     * Constructor. Initializes the variables.
     * 
     * @param id Id of this node
     * 
     * @param gd Number of hops from this node to the global leader in initial
     * configuration
     * 
     * @param glid Initial global leader id
     * 
     * @param ld Number os hops from this node to its local leader in initial
     * configuration
     * 
     * @param llid Initial local leader id
     * 
     * @param networkSize Number of nodes in the network
     */
    public Node(ActorSystem system, int id, int gd, int glid, int ld, int llid, int networkSize) {
        nodeId = id;
        forming = new ActorRef[networkSize];
        neighbors = new ActorRef[networkSize];
        globalLeaderId = glid;
        localLeaderId = llid;
        causalClock = 0;
        heights = new Height[networkSize];
        heights[nodeId] = new Height(gd, -1, globalLeaderId, ld, -1, localLeaderId, nodeId);

        nodeActor = system.actorOf(NodeActor.createActor(this), "n"+nodeId);
    }

    public Height getHeight() {
        return heights[nodeId];
    }

    /*
     * Adds the channel to the forming set.
     * 
     * @param channel Channel to the discovered neighbor node
     * 
     * @param neighborId Id of the discovered neighbor node
     */
    private void addForming(ActorRef channel, int neighborId) {
        if (forming[neighborId] == null)
            forming[neighborId] = channel;
    }

    /*
     * Moves a channel from the forming set to the neighbor set.
     * 
     * @param neighborId Id of the neighbor
     */
    private void addNeighbor(int neighborId) {
        if (forming[neighborId] != null && neighbors[neighborId] == null) {
            neighbors[neighborId] = forming[neighborId];
            forming[neighborId] = null;
        }
    }

    /*
     * Removes the channel from forming and neighbor sets.
     * 
     * @param neighborId Id of the neighbor removed
     */
    private void removeNeighbor(int neighborId) {
        if (forming[neighborId] != null)
            forming[neighborId] = null;
        if (neighbors[neighborId] != null)
            neighbors[neighborId] = null;
        heights[neighborId] = null;
    }

    /*
     * ChannelDown handler. Removes the neighbor and checks if there is a path to
     * the leader. If there isn't, starts a search.
     * 
     * @param chdown The ChannelDown event
     */
    public void handleChannelDown(ChannelDown chdown) {
        removeNeighbor(chdown.neighborId);
        if (!hasNeighbors() && (nodeId != globalLeaderId)) {
            //log.info("\n[{}]: No neighbors, electing self...", getSelf().path().name());
            electSelfGlobal();
            sendToForming(heights[nodeId]);
        } else if (isSink()) {
            if (nodeId == localLeaderId) {
                //log.info("\n[{}]: Is a sink and local leader, searching global", getSelf().path().name());
                heights[nodeId].startNewReferenceLevelGlobal(causalClock, nodeId);
            } else {
                //log.info("\n[{}]: Is a sink, searching local", getSelf().path().name());
                heights[nodeId].startNewReferenceLevelLocal(causalClock, nodeId);
            }
            sendToAll(heights[nodeId]);
        }
    }

    /*
     * ChannelUp handler. Adds the neighbor to the forming set and sends an update
     * of its height
     * 
     * @param chup The ChannelUp event
     */
    public void handleChannelUp(ChannelUp chup) {
        addForming(chup.channel, chup.neighborId);
        sendMessage(chup.channel, heights[nodeId]);
    }

    /*
     * SetUp handler. Adds the neighbor to the forming set, moves it to the neighbor
     * set and saves the neighbors height in the height set.
     * 
     * @param sup The SetUp event
     */
    public void handleSetUp(SetUp sup) {
        addForming(sup.channel, sup.neighborId);
        addNeighbor(sup.neighborId);
        heights[sup.neighborId] = sup.height.copy();
    }

    /*
     * Update handler. Moves the neighbor to the neighbor set, saves its height in
     * height set and decides how to change its own height based on the algortihm.
     * If the height was changed, sends an update to neighbors.
     */
    public void handleUpdate(Update u) {
        Height h = u.height;
        heights[h.nodeId] = h.copy();
        addNeighbor(h.nodeId);
        Height myOldHeight = heights[nodeId].copy();
        ReferenceLevel neighborsRL = h.rl;
        if (myOldHeight.globalLeaderPair.compareTo(h.globalLeaderPair) == 0) { // same global leaders
            if (myOldHeight.localLeaderPair.compareTo(h.localLeaderPair) == 0) { // same local leaders
                if (isSink()) {
                    //log.info("\n[{}]: Is sink...", getSelf().path().name());
                    if (h.rl.reflected == 0 && h.rl.localHops > MAX_HOPS) { // local search has gone too far
                        //log.info("\n[{}]: Local search gone too far, reflecting...", getSelf().path().name());
                        heights[nodeId].reflectReferenceLevel(h.rl);
                    } else if (nodeId == localLeaderId && h.rl.localHops > 0) { // local search found global leader
                        //log.info("\n[{}]: Local search found a local leader, searching global...",
                        //        getSelf().path().name());
                        heights[nodeId].startNewReferenceLevelGlobal(causalClock, nodeId);
                    } else if (neighborsHaveSameRL(neighborsRL)) { // neighbors have the same RL
                        //log.info("\n[{}]: All neighbors have the same RL (dead end)...", getSelf().path().name());
                        if (neighborsRL.timestamp > 0 && neighborsRL.reflected == 0) { // search hasn't been reflected
                                                                                       // yet
                            //log.info("\n[{}]: The search has not been reflected, reflecting it...",
                            //        getSelf().path().name());
                            heights[nodeId].reflectReferenceLevel(h.rl);
                        } else if (neighborsRL.timestamp > 0 && neighborsRL.reflected == 1
                                && neighborsRL.originId == nodeId) { // search has been reflected and it was started by
                                                                     // this node
                            //log.info("\n[{}]: The reflected search has reached the origin, electing self...",
                            //        getSelf().path().name());

                            if (neighborsRL.localHops == 0) {
                                electSelfGlobal();
                            } else {
                                electSelfLocal();
                            }
                        } else { // the search has been relfected and this node didn't start it
                            //log.info(
                            //        "\n[{}]: There is no search happening, or a reflected search reached a second dead end, starting new search...",
                            //        getSelf().path().name());

                            if (nodeId == localLeaderId) {
                                heights[nodeId].startNewReferenceLevelGlobal(causalClock, nodeId);
                            } else {
                                heights[nodeId].startNewReferenceLevelLocal(causalClock, nodeId);
                            }
                        }
                    } else { // neighbors have different RL
                        //log.info("\n[{}]: Neighbors have different RL, propagating largest...",
                        //        getSelf().path().name());
                        propagateLargestRL();
                    }
                }
            } else { // different local leaders
                if (nodeId != localLeaderId && !localLeadersInNeighborhood()) {
                    //log.info("\n[{}]: Different local leaders, leaders far away, electing self...",
                    //        getSelf().path().name());
                    electSelfLocal();
                } else {
                    //log.info("\n[{}]: Different local leaders, checking priority...", getSelf().path().name());
                    adoptLLPIfPriority(h.nodeId);
                }
            }
        } else { // different global leaders
            //log.info("\n[{}]: Different global leaders, checking priority...", getSelf().path().name());
            adoptGLPIfPriority(h.nodeId);
        }

        if (myOldHeight.compareTo(heights[nodeId]) != 0) {
            sendToAll(heights[nodeId]);
        }
    }

    /*
     * Checks if the neighbors set is not empty.
     * 
     * @return true if at least one element of neighbors is not null
     * 
     * @return false otherwise
     */
    private boolean hasNeighbors() {
        for (ActorRef channel : neighbors) {
            if (channel != null)
                return true;
        }
        return false;
    }

    /*
     * Checks if neighbors know of a local leader less than MAX_HOPS away.
     * 
     * @return true if there is at least one local leader within MAX_HOPS
     * 
     * @return false otherwise
     */
    private boolean localLeadersInNeighborhood() {
        for (Height h : heights) {
            if (h != null && h != heights[nodeId] && h.localDelta >= 0 && h.localDelta + 1 <= MAX_HOPS) {
                return true;
            }
        }
        return false;
    }

    /*
     * Checks if the node has no outgoing links.
     * 
     * @return true if the node is not a global leader and is the lowest among its
     * neighbors
     * 
     * @return false otherwise
     */
    private boolean isSink() {
        boolean isSink = (nodeId != globalLeaderId);
        for (Height h : heights) {
            if (h != null && h != heights[nodeId]) {
                isSink = isSink && (h.globalLeaderPair.compareTo(heights[nodeId].globalLeaderPair) == 0)
                        && (heights[nodeId].compareTo(h) < 0);
            }
            if (!isSink) {
                return false;
            }
        }
        return true;
    }

    /*
     * Check if all neighbors have the same ReferenceLevel.
     * 
     * @return true if all neighbors have the same RL
     * 
     * @return false otherwise
     */
    private boolean neighborsHaveSameRL(ReferenceLevel rl) {
        for (Height h : heights) {
            if (h != null && h != heights[nodeId] && h.rl.compareTo(rl) != 0) {
                return false;
            }
        }
        return true;
    }

    /*
     * Adopts the largest ReferenceLevel among its neighbors as its own.
     */
    private void propagateLargestRL() {
        ReferenceLevel rl = new ReferenceLevel();
        for (Height h : heights) {
            if (h != null && h != heights[nodeId]) {
                if (nodeId == localLeaderId && h.rl.localHops == 0 && h.rl.compareTo(rl) > 0) {
                    rl = h.rl;
                } else if (h.rl.compareTo(rl) > 0) {
                    rl = h.rl;
                }
            }
        }
        int gdelta = 0;
        int ldelta = 0;
        for (Height h : heights) {
            if (h != null && h != heights[nodeId]) {
                if (h.rl.compareTo(rl) == 0) {
                    if (h.globalDelta < gdelta)
                        gdelta = h.globalDelta;
                    if (h.localDelta < ldelta)
                        ldelta = h.localDelta;
                }
            }
        }
        heights[nodeId].rl = rl.copy();
        if (rl.localHops > 0) {
            heights[nodeId].rl.localHops++;
            heights[nodeId].localDelta = ldelta - 1;
        } else {
            heights[nodeId].globalDelta = gdelta - 1;
        }
    }

    /*
     * Redefine its global leader pair to elect itself as leader with causalClock as
     * timestamp.
     */
    private void electSelfGlobal() {
        heights[nodeId].electGlobal(causalClock, nodeId);
        globalLeaderId = nodeId;
        localLeaderId = nodeId;
    }

    /*
     * Redefine its local leader pair to elect itself as leader with causalClock as
     * timestamp.
     */
    private void electSelfLocal() {
        heights[nodeId].electLocal(causalClock, nodeId);
        localLeaderId = nodeId;
    }

    /*
     * Adopts the neighbor's global leader if its been elected more recently.
     * 
     * @param neighborId Id of the neighbor whose leader it may adopt
     */
    private void adoptGLPIfPriority(int neighborId) {
        Height h = heights[neighborId];
        if (h.globalLeaderPair.compareTo(heights[nodeId].globalLeaderPair) < 0) {
            heights[nodeId] = h.copy();
            heights[nodeId].globalDelta++;
            heights[nodeId].localDelta++;
            heights[nodeId].nodeId = nodeId;
            globalLeaderId = h.globalLeaderPair.leaderId;
        } else {
            sendMessage(neighbors[neighborId], heights[nodeId]);
        }
    }

    /*
     * Adopts the neighbor's local leader if it's closer than its own leader, of if
     * it's closer to the global leader
     * 
     * @param neighborId Id of the neighbor whose leader it may adopt
     */
    private void adoptLLPIfPriority(int neighborId) {
        Height h = heights[neighborId];
        if (h.localDelta >= 0 && h.globalDelta >= 0) {
            if ((heights[nodeId].localDelta < 0) || (h.localDelta + 1 < heights[nodeId].localDelta)
                    || ((h.localDelta + 1 == heights[nodeId].localDelta)
                            && (h.globalDelta + 1 < heights[nodeId].globalDelta))
                    || ((h.localDelta + 1 == heights[nodeId].localDelta)
                            && (h.globalDelta + 1 == heights[nodeId].globalDelta)
                            && h.localLeaderPair.compareTo(heights[nodeId].localLeaderPair) < 0)) {
                heights[nodeId] = h.copy();
                heights[nodeId].globalDelta++;
                heights[nodeId].localDelta++;
                heights[nodeId].nodeId = nodeId;
            }
        } else {
            sendMessage(neighbors[neighborId], heights[nodeId]);
        }
    }

    /*
     * Sends an Update message to the target with the given height
     * 
     * @param target Channel to send the message to
     * 
     * @param height Height to send to target
     */
    private void sendMessage(ActorRef target, Height height) {
        causalClock++;
        target.tell(new Update(causalClock, height), nodeActor);
    }

    /*
     * Sends an Update message to all channels in the neighbor set
     * 
     * @height Height to send to neighbors
     */
    private void sendToNeihgbors(Height height) {
        for (ActorRef target : neighbors) {
            if (target != null)
                sendMessage(target, height);
        }
    }

    /*
     * Sends an Update message to all channels in the forming set
     * 
     * @height Height to send to neighbors
     */
    private void sendToForming(Height height) {
        for (ActorRef target : forming) {
            if (target != null)
                sendMessage(target, height);
        }
    }

    /*
     * Sends an Update message to all channels in the forming and neighbor sets
     * 
     * @height Height to send to neighbors
     */
    private void sendToAll(Height height) {
        sendToNeihgbors(height);
        for (ActorRef target : forming) {
            if (target != null && !Arrays.asList(neighbors).contains(target)) {
                sendMessage(target, height);
            }
        }
    }
}