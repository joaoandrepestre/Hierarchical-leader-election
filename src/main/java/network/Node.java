package network;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;
import java.lang.Math;

import height.Height;
import events.*;

public class Node extends UntypedAbstractActor {
    private int nodeId;
    private ArrayList<Channel> forming;
    private ArrayList<Channel> neighbors;
    private int globalLeaderId;
    private int localLeaderId;
    private int causalClock;
    public ArrayList<Height> heights;

    public Node(int id, int glid, int llid){
        nodeId = id;
        forming = new ArrayList<Channel>();
        neighbors = new ArrayList<Channel>();
        globalLeaderId = glid;
        localLeaderId = llid;
        causalClock = 0;
        heights = new ArrayList<Height>();
    }

    public Props createActor(int id, int glid, int llid){
        return Props.create(Node.class, ()->{
            return new Node(int id, int glid, int llid);
        });
    }

    private void handleChannelDown(){

    }

    private void handleChannelUp(){

    }

    private void handleUpdate(){

    }

    @Override
    public void onReceive(Object message){
        Event e = (Event) message;
        causalClock = max(causalClock, e.timestamp) + 1;
        if(e instanceof ChannelDown){
            handleChannelDown();
        }
        else if(e instanceof ChannelUp){
            handleChannelUp();
        }
        else if(e instanceof Update){
            handleUpdate();
        }
        else{
            // error?
        }
    }
}