package network;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.Arrays;
import java.lang.Math;

import height.Height;
import height.ReferenceLevel;
import events.*;

public class Node extends UntypedAbstractActor {
    private final int MAX_HOPS = 2; 
    private int nodeId;
    private ActorRef[] forming;
    private ActorRef[] neighbors;
    private int globalLeaderId;
    private int localLeaderId;
    private int causalClock;
    public Height[] heights;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public Node(int id, int gd, int glid, int ld, int llid, int networkSize) {
        nodeId = id;
        forming = new ActorRef[networkSize];
        neighbors = new ActorRef[networkSize];
        globalLeaderId = glid;
        localLeaderId = llid;
        causalClock = 0;
        heights = new Height[networkSize];
        heights[nodeId] = new Height(gd, -1, globalLeaderId, ld, -1, localLeaderId, nodeId);
        log.info("Created node {} with height {}", nodeId, heights[nodeId]);
    }

    public static Props createActor(int id, int gd, int glid, int ld, int llid, int size) {
        return Props.create(Node.class, () -> {
            return new Node(id, gd, glid, ld, llid, size);
        });
    }

    private void addForming(ActorRef channel, int neighborId) {
        if (forming[neighborId] == null)
            forming[neighborId] = channel;
    }

    private void addNeighbor(int neighborId) {
        if (forming[neighborId] != null && neighbors[neighborId] == null) {
            neighbors[neighborId] = forming[neighborId];
            forming[neighborId] = null;
        }
    }

    private void removeNeighbor(ActorRef channel, int neighborId) {
        if (forming[neighborId] != null)
            forming[neighborId] = null;
        if (neighbors[neighborId] != null)
            neighbors[neighborId] = null;
        heights[neighborId] = null;
    }

    private void handleChannelDown(ChannelDown chdown) {
        removeNeighbor(chdown.channel, chdown.neighborId);
        if (Arrays.asList(neighbors).isEmpty()) {
            electSelfGlobal();
            sendToForming(heights[nodeId]);
        } else if (isSink()) {
            if (nodeId == localLeaderId)
                startNewRefLevelGlobal();
            else
                startNewRefLevelLocal();
            sendToAll(heights[nodeId]);
        }
    }

    private void handleChannelUp(ChannelUp chup) {
        addForming(chup.channel, chup.neighborId);
        sendMessage(chup.channel, heights[nodeId]);
    }

    private void handleUpdate(Update u) {
        Height h = u.height;
        heights[h.nodeId] = h;
        addNeighbor(h.nodeId);
        Height myOldHeight = heights[nodeId];
        ReferenceLevel neighborsRL;
        if (myOldHeight.glp == h.glp) { // same global leaders
            if (myOldHeight.llp == h.llp) { // same local leaders
                if (isSink()) {
                    if (h.rl.reflected == 0 && h.rl.localHops > MAX_HOPS) { // local search has gone too far
                        reflectReferenceLevel();
                    } else if (nodeId == localLeaderId && h.rl.localHops > 0) { // local search found global leader
                        startNewRefLevelGlobal();
                    } else if ((neighborsRL = getNeighborsRL()) != null) { // neighbors have the same RL
                        if (neighborsRL.timestamp > 0 && neighborsRL.reflected == 0) { // search hasn't been reflected
                                                                                       // yet
                            reflectReferenceLevel();
                        } else if (neighborsRL.timestamp > 0 && neighborsRL.reflected == 1
                                && neighborsRL.originId == nodeId) { // search has been reflected and it was started by
                                                                     // this node
                            if (neighborsRL.localHops == 0) {
                                electSelfGlobal();
                            } else {
                                electSelfLocal();
                            }
                        } else { // the search has been relfected and this node didn't start it
                            if (nodeId == localLeaderId) {
                                startNewRefLevelGlobal();
                            } else {
                                startNewRefLevelLocal();
                            }
                        }
                    } else { // neighbors have different RL
                        propagateLargestRL();
                    }
                }
            } else { // different local leaders
                if (nodeId != localLeaderId && !localLeadersInNeighborhood()) {
                    electSelfLocal();
                } else {
                    adoptLLPIfPriority();
                }
            }
        } else { // different global leaders
            adoptGLPIfPriority();
        }

        if (myOldHeight != heights[nodeId]) {
            sendToAll(heights[nodeId]);
        }
    }

    private boolean localLeadersInNeighborhood() {
        return false;
    }

    private boolean isSink() {
        return false;
    }

    private ReferenceLevel getNeighborsRL() {
        return null;
    }

    private void propagateLargestRL() {
    }

    private void reflectReferenceLevel() {
    }

    private void startNewRefLevelGlobal() {
    }

    private void startNewRefLevelLocal() {
    }

    private void electSelfGlobal() {
    }

    private void electSelfLocal() {
    }

    private void adoptGLPIfPriority() {
    }

    private void adoptLLPIfPriority() {
    }

    private void sendMessage(ActorRef target, Height height) {
        causalClock++;
        target.tell(new Update(causalClock, height), getSelf());
    }

    private void sendToNeihgbors(Height height) {
        for (ActorRef target : neighbors) {
            sendMessage(target, height);
        }
    }

    private void sendToForming(Height height) {
        for (ActorRef target : forming) {
            sendMessage(target, height);
        }
    }

    private void sendToAll(Height height) {
        sendToNeihgbors(height);
        for (ActorRef target : forming) {
            if (!Arrays.asList(neighbors).contains(target)) {
                sendMessage(target, height);
            }
        }
    }

    @Override
    public void onReceive(Object message) {
        Event e = (Event) message;
        log.info("Message received with timestamp {} from {}", e.timestamp, getSender().path().name());
        causalClock = Math.max(causalClock, e.timestamp) + 1;
        if (e instanceof ChannelDown) {
            handleChannelDown((ChannelDown) e);
        } else if (e instanceof ChannelUp) {
            handleChannelUp((ChannelUp) e);
        } else if (e instanceof Update) {
            handleUpdate((Update) e);
        } else {
            // error?
        }
    }
}