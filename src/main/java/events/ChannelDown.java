package events;

import akka.actor.ActorRef;

public class ChannelDown extends Event {

    public ActorRef channel;
    public int neighborId;

    public ChannelDown(int t, ActorRef c, int nid){
        super(t);
        channel = c;
        neighborId = nid;
    }
}