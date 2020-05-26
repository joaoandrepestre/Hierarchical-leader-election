package events;
public class Event {
    public int timestamp;

    public Event(int t){
        timestamp = t;
    }

    public String toString(){
        return "Event("+timestamp+")";
    }
}