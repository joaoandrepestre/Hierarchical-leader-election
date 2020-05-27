package events;

/* 
Event base class. All events extend Event.
*/
public class Event {
    public int timestamp; /* Timestamp of the event. Used for nodes to keep logical causal clocks */

    /*
     * Constructor. Initializes the variables.
     * 
     * @param t Timestamp of the event
     */
    public Event(int t) {
        timestamp = t;
    }

    /*
     * Creates a String representation of the event for logging purposes
     * 
     * @return The string representation
     */
    public String toString() {
        return "Event(" + timestamp + ")";
    }
}