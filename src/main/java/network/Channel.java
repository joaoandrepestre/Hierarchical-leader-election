package network;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.Queue;
import java.lang.Math;

import events.*;

public class Channel extends UntypedAbstractActor{
    public Node sender;
    public Node receiver;
    public int status;
    public Queue<Update> messageQueue;

    public Channel(Node s, Node r,int st){
        sender = s;
        receiver = r;
        status = st;
        messageQueue = new Queue<Update>();
    }

    public Props createActor(Node s, Node r,int st){
        return Props.create(Channel.class, ()->{
            return new Channel(Node s, Node r,int st);
        });
    }

    private void whileUp(){
        while(status == 1){
            Update message = messageQueue.poll();
            receiver.tell(message, getSelf());
            //wait some time?
        }
    }

    @Override
    public void onReceive(Object message){
        if(message instanceof ChannelDown){
            status = 0;
            messageQueue = new Queue<Update>();
            receiver.tell(message, getSelf());
        }
        else if(message instanceof ChannelUp){
            status = 1;
            whileUp();
            receiver.tell(message, getSelf());
        }
        else if(message instanceof Update){
            if(status == 1) messageQueue.add(message);
        }
        else{
            // error?
        }

    }
}