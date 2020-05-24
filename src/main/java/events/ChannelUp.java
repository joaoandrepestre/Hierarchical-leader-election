package events;

import akka.actor.ActorRef;

public class ChannelUp extends Event {

    public ActorRef channel;
    public int neighborId;

    public ChannelUp(int t, ActorRef c, int nid){
        super(t);
        channel = c;
        neighborId = nid;
    }

}