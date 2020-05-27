package events;

import akka.actor.ActorRef;

/*
ChannelUp event. Informs a node the channel is active again. 
 */
public class ChannelUp extends Event {

    public ActorRef channel;/*
                             * Mirrored channel. Node i will receive this event from Channelji. This
                             * variable will hold Channelij
                             */
    public int neighborId;/*
                           * The disconnected neighbor. Node i will receive this event from Channelji. This
                           * variable will hold the id of node j
                           */

    /*
     * Constructor. Initializes the variables
     * 
     * @param t Timestamp of the event
     * 
     * @param c Mirrored channel
     * 
     * @param nid Neighbor id
     */
    public ChannelUp(int t, ActorRef c, int nid) {
        super(t);
        channel = c;
        neighborId = nid;
    }

    /*
     * Creates a String representation of the event for logging purposes
     * 
     * @return The string representation
     */
    public String toString() {
        return "ChannelUp(" + timestamp + "," + neighborId + ")";
    }

}