package network;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Math;

import height.Height;
import events.*;

public class Node extends UntypedAbstractActor {
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
            for (ActorRef node : forming) {
                sendMessage(node, heights[nodeId]);
            }
        } else if (isSink()) {
            if (nodeId == localLeaderId)
                startNewRefLevelGlobal();
            else
                startNewRefLevelLocal();
            for (ActorRef node : neighbors) {
                sendMessage(node, heights[nodeId]);
            }
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
    }

    private boolean isSink() {
        return false;
    }

    private void startNewRefLevelGlobal() {
    }

    private void startNewRefLevelLocal() {
    }

    private void electSelfGlobal() {

    }

    private void sendMessage(ActorRef target, Height height) {
        causalClock++;
        target.tell(new Update(causalClock, height), getSelf());
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