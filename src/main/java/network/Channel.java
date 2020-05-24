package network;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.LinkedList;
import java.util.Queue;

import events.*;

class MessageForwarding extends Thread {
    public Channel channel;

    public MessageForwarding(Channel c) {
        channel = c;
    }

    public void run() {
        while (channel.status == 1) {
            Update message;
            if (!channel.messageQueue.isEmpty()) {
                message = channel.messageQueue.poll();
                channel.receiver.tell(message, channel.getSelf());
            }
            // wait some time?
        }
    }
}

public class Channel extends UntypedAbstractActor {
    public ActorRef sender;
    public ActorRef receiver;
    public int status;
    public Queue<Update> messageQueue;

    public Channel(ActorRef s, ActorRef r, int st) {
        sender = s;
        receiver = r;
        status = st;
        messageQueue = new LinkedList<Update>();
    }

    public static Props createActor(ActorRef s, ActorRef r, int st) {
        return Props.create(Channel.class, () -> {
            return new Channel(s, r, st);
        });
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof ChannelDown) {
            status = 0;
            messageQueue = new LinkedList<Update>();
            receiver.tell(message, getSelf());
        } else if (message instanceof ChannelUp) {
            status = 1;
            new MessageForwarding(this).start();
            receiver.tell(message, getSelf());
        } else if (message instanceof Update) {
            Update u = (Update) message;
            if (status == 1)
                messageQueue.add(u);
        } else {
            // error?
        }

    }
}