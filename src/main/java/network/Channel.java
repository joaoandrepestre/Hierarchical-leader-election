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
                channel.logState();
                message = channel.messageQueue.poll();
                channel.receiver.tell(message, channel.getSelf());
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

public class Channel extends UntypedAbstractActor {
    public ActorRef sender;
    public ActorRef receiver;
    public int status;
    public Queue<Update> messageQueue;
    private MessageForwarding mf;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public Channel(ActorRef s, ActorRef r, int st) {
        sender = s;
        receiver = r;
        status = st;
        messageQueue = new LinkedList<Update>();
        mf = new MessageForwarding(this);
    }

    public static Props createActor(ActorRef s, ActorRef r, int st) {
        return Props.create(Channel.class, () -> {
            return new Channel(s, r, st);
        });
    }

    public void logState() {
        String s = "Status:" + status + "\nMessages:\n";
        for (Update u : messageQueue) {
            s += "\t" + u + "\n";
        }
        log.info("\n[{}]: {}", getSelf().path().name(), s);
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof ChannelDown) {
            ChannelDown chdown = (ChannelDown) message;
            status = 0;
            messageQueue = new LinkedList<Update>();
            receiver.tell(message, getSelf());
            //log.info("\n[{}]: Received {}", getSelf().path().name(), chdown);
            //logState();
        } else if (message instanceof ChannelUp) {
            ChannelUp chup = (ChannelUp) message;
            status = 1;
            mf.start();
            receiver.tell(message, getSelf());
            //log.info("\n[{}]: Received {}", getSelf().path().name(), chup);
            //logState();
        } else if (message instanceof Update) {
            Update u = (Update) message;
            if (status == 1)
                messageQueue.add(u);
            //log.info("\n[{}]: Received {}", getSelf().path().name(), u);
            //logState();
        } else {
            // error?
        }

    }
}