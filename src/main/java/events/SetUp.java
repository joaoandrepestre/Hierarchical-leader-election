package events;

import akka.actor.ActorRef;
import height.Height;

/* 
SetUp event. Used for setting up the initial topology. 
Behavior similar to ChannelUp, but stops the algorithm from running before the network is setup
*/
public class SetUp extends Event {

    public ActorRef channel; /*
                              * Mirrored channel. Node i will receive this event from Channelji. This
                              * variable will hold Channelij
                              */
    public int neighborId; /*
                            * The disconnected neighbor. Node i will receive this event from Channelji This
                            * variable will hold the id of node j
                            */
    public Height height; /*
                           * Neighbors initial height.Node i will receive this event from Channelji This
                           * variable will hold the height of node j
                           */

    /*
     * Constructor. Initializes the variables
     * 
     * @param t Timestamp of the event
     * 
     * @param c Mirrored channel
     * 
     * @param nid Neighbors id
     * 
     * @param h Neighbors height
     */
    public SetUp(int t, ActorRef c, int nid, Height h) {
        super(t);
        channel = c;
        neighborId = nid;
        height = h;
    }

    /*
     * Creates a String representation of the event for logging purposes
     * 
     * @return The string representation
     */
    public String toString() {
        return "SetUp(" + timestamp + "," + neighborId + ")";
    }
}