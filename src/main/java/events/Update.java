package events;

import height.Height;

public class Update extends Event{

    public Height height;

    public Update(Height h){
        height = h;
    }
}