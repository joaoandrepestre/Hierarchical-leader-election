package events;

import height.Height;

/* 
Update event. Updates a node on the height changes of a neighbor.
*/
public class Update extends Event {

    public Height height;/*
                          * Neighbors new height. Node i will receive this event from Channelji. This
                          * variable will hold the height of node j
                          */

    /*
     * Constructor. Initializes the variables
     * 
     * @param h Neighbors height
     */
    public Update(int t, Height h) {
        super(t);
        height = h;
    }

    /*
     * Creates a String representation of the event for logging purposes
     * 
     * @return The string representation
     */
    public String toString() {
        return "Update(" + timestamp + "," + height + ")";
    }
}