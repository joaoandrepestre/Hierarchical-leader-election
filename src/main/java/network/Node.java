package network;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.Arrays;
import java.lang.Math;

import height.Height;
import height.LeaderPair;
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
                heights[nodeId].startNewReferenceLevelGlobal(causalClock, nodeId);
            else
                heights[nodeId].startNewReferenceLevelLocal(causalClock, nodeId);
            sendToAll(heights[nodeId]);
        }
    }

    private void handleChannelUp(ChannelUp chup) {
        addForming(chup.channel, chup.neighborId);
        sendMessage(chup.channel, heights[nodeId]);
    }

    private void handleUpdate(Update u) {
        Height h = u.height;
        heights[h.nodeId] = h.copy();
        addNeighbor(h.nodeId);
        Height myOldHeight = heights[nodeId].copy();
        ReferenceLevel neighborsRL = h.rl;
        if (myOldHeight.globalLeaderPair.compareTo(h.globalLeaderPair) == 0) { // same global leaders
            if (myOldHeight.localLeaderPair.compareTo(h.localLeaderPair) == 0) { // same local leaders
                if (isSink()) {
                    if (h.rl.reflected == 0 && h.rl.localHops > MAX_HOPS) { // local search has gone too far
                        heights[nodeId].reflectReferenceLevel();
                    } else if (nodeId == localLeaderId && h.rl.localHops > 0) { // local search found global leader
                        heights[nodeId].startNewReferenceLevelGlobal(causalClock, nodeId);
                    } else if (neighborsHaveSameRL(neighborsRL)) { // neighbors have the same RL
                        if (neighborsRL.timestamp > 0 && neighborsRL.reflected == 0) { // search hasn't been reflected
                                                                                       // yet
                            heights[nodeId].reflectReferenceLevel();
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
                                heights[nodeId].startNewReferenceLevelGlobal(causalClock, nodeId);
                            } else {
                                heights[nodeId].startNewReferenceLevelLocal(causalClock, nodeId);
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
                    adoptLLPIfPriority(h.nodeId);
                }
            }
        } else { // different global leaders
            adoptGLPIfPriority(h.nodeId);
        }

        if (myOldHeight.compareTo(heights[nodeId]) != 0) {
            sendToAll(heights[nodeId]);
        }
    }

    private boolean localLeadersInNeighborhood() {
        for (Height h : heights) {
            if (h != null && h != heights[nodeId] && h.localDelta + 1 <= MAX_HOPS) {
                return true;
            }
        }
        return false;
    }

    private boolean isSink() {
        boolean isSink = (nodeId == globalLeaderId);
        for (Height h : heights) {
            if (h != null && h != heights[nodeId]) {
                isSink = isSink && (h.globalLeaderPair.compareTo(heights[nodeId].globalLeaderPair) == 0)
                        && (heights[nodeId].compareTo(h) < 0);
                if (!isSink) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean neighborsHaveSameRL(ReferenceLevel rl) {
        for (Height h : heights) {
            if (h != null && h != heights[nodeId] && h.rl.compareTo(rl) != 0) {
                return false;
            }
        }
        return true;
    }

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

    private void electSelfGlobal() {
        heights[nodeId].electGlobal(causalClock, nodeId);
        globalLeaderId = nodeId;
        localLeaderId = nodeId;
    }

    private void electSelfLocal() {
        heights[nodeId].electLocal(causalClock, nodeId);
        localLeaderId = nodeId;
    }

    private void adoptGLPIfPriority(int neighborId) {
        Height h = heights[neighborId];
        if (h.globalLeaderPair.compareTo(heights[nodeId].globalLeaderPair) < 0) {
            LeaderPair llp = heights[nodeId].localLeaderPair;
            int ldelta = heights[nodeId].localDelta;
            heights[nodeId] = h.copy();
            heights[nodeId].globalDelta++;
            heights[nodeId].localLeaderPair = llp;
            heights[nodeId].localDelta = ldelta;
            heights[nodeId].nodeId = nodeId;
            globalLeaderId = h.globalLeaderPair.leaderId;
        }else{
            sendMessage(neighbors[neighborId], heights[nodeId]);
        }
    }

    private void adoptLLPIfPriority(int neighborId) {
        Height h = heights[neighborId];
        if ((h.localDelta + 1 < heights[nodeId].localDelta)
                || ((h.localDelta + 1 == heights[nodeId].localDelta)
                        && (h.globalDelta + 1 < heights[nodeId].globalDelta))
                || ((h.localDelta + 1 == heights[nodeId].localDelta)
                        && (h.globalDelta + 1 == heights[nodeId].globalDelta)
                        && h.localLeaderPair.compareTo(heights[nodeId].localLeaderPair) < 0)) {
            heights[nodeId] = h.copy();
            heights[nodeId].globalDelta++;
            heights[nodeId].localDelta++;
            heights[nodeId].nodeId = nodeId;
        }else{
            sendMessage(neighbors[neighborId], heights[nodeId]);
        }
    }

    private void sendMessage(ActorRef target, Height height) {
        causalClock++;
        target.tell(new Update(causalClock, height), getSelf());
    }

    private void sendToNeihgbors(Height height) {
        for (ActorRef target : neighbors) {
            if (target != null)
                sendMessage(target, height);
        }
    }

    private void sendToForming(Height height) {
        for (ActorRef target : forming) {
            if (target != null)
                sendMessage(target, height);
        }
    }

    private void sendToAll(Height height) {
        sendToNeihgbors(height);
        for (ActorRef target : forming) {
            if (target != null && !Arrays.asList(neighbors).contains(target)) {
                sendMessage(target, height);
            }
        }
    }

    @Override
    public void onReceive(Object message) {
        Event e = (Event) message;
        causalClock = Math.max(causalClock, e.timestamp) + 1;
        if (e instanceof ChannelDown) {
            ChannelDown chdown = (ChannelDown) e;
            log.info("[{}]: ChannelDown received with timestamp {} from {}", nodeId , e.timestamp, chdown.neighborId);
            handleChannelDown(chdown);
        } else if (e instanceof ChannelUp) {
            ChannelUp chup = (ChannelUp) e;
            log.info("[{}]: ChannelUp received with timestamp {} from {}", nodeId , e.timestamp, chup.neighborId);
            handleChannelUp(chup);
        } else if (e instanceof Update) {
            Update u = (Update) e;
            log.info("[{}]: Update received with timestamp {} from {}", nodeId , e.timestamp, u.height.nodeId);
            handleUpdate(u);
        } else {
            // error?
        }
    }
}