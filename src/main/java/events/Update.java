package events;

import height.Height;

public class Update extends Event{

    public Height height;

    public Update(int t, Height h){
        super(t);
        height = h;
    }

    public String toString(){
        return "Update("+timestamp+","+height+")";
    }
}