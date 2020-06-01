package network;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.LinkedList;
import java.util.Queue;

import events.*;

/* 
* MessageForwarding Thread class. 
* Used by the channel to keep forwarding the messages in the queue while the status is up.
*/
class MessageForwarding extends Thread {
    public Channel channel; /* Channel that holds the messages to forward */
    private final Object lock = new Object(); /* Stops the message forwarding when the status is down */

    /*
     * Constructor. Initializes the variables.
     * 
     * @param c The channel
     */
    public MessageForwarding(Channel c) {
        channel = c;
    }

    /*
     * Restarts message forwarding when channel goes up.
     */
    public void forward() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /*
     * Main Thread method. If the channel is up, forwards the message at the head of
     * the queue every 2 seconds. IF the channel is down, pauses and waits for it to
     * go up.
     */
    public void run() {
        while (true) {
            synchronized (lock) {
                if (channel.status == 0) {
                    synchronized (lock) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                Update message;
                if (!channel.messageQueue.isEmpty()) {
                    message = channel.messageQueue.poll();
                    channel.log("Forwarding message " + message);
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
}

/*
 * Channel class. Implements the communication channel part of the network
 * model. Receives messages from nodes and forwards them to other nodes.
 */
public class Channel extends UntypedAbstractActor {
    public ActorRef sender; /* Sender node. Channel receives messages from this node */
    public ActorRef receiver; /* Receiver node. Channel forwards messages to this node */
    public int status; /*
                        * Status of the channel. 0 is down, 1 is up. When up, received messages are
                        * added to the queue and queued messages are forwarded. When down, messages are
                        * ignored and the queue is emptied.
                        */
    public Queue<Update> messageQueue; /* Queue of messages to forward */
    private MessageForwarding mf; /* MessageForwarding system */

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this); /* Akka logger */

    /*
     * Constructor. Initializes the variables.
     * 
     * @param s Sender node
     * 
     * @param r Receiver node
     * 
     * @param st Initial status
     */
    public Channel(ActorRef s, ActorRef r, int st) {
        sender = s;
        receiver = r;
        status = st;
        messageQueue = new LinkedList<Update>();
        mf = new MessageForwarding(this);
        mf.start();
    }

    /*
     * Creates an actor of type Channel in the akka system.
     * 
     * @param s Sender node
     * 
     * @param r Receiver node
     * 
     * @param st Initial status
     * 
     * @return Props object of the created actor
     */
    public static Props createActor(ActorRef s, ActorRef r, int st) {
        return Props.create(Channel.class, () -> {
            return new Channel(s, r, st);
        });
    }

    /*
     * Logs the given string using the akka logger.
     * 
     * @param s The string to log
     */
    public void log(String s) {
        log.info("\n[{}]: {}", getSelf().path().name(), s);
    }

    /*
     * Logs the full state of the channel. That is, the status and the message
     * queue.
     */
    public void logState() {
        String s = "Status:" + status + "\nMessages:\n";
        for (Update u : messageQueue) {
            s += "\t" + u + "\n";
        }
        log.info("\n[{}]: {}", getSelf().path().name(), s);
    }

    /*
     * Called when a message is received by the actor.
     * 
     * A ChannelDown event will change the status to 0 (down), empty the queue and
     * send the event to the receiver node.
     * 
     * A ChannelUp event will change the status to 1 (up), restart the
     * MessageForwarding thread and send the event to the receiver node.
     * 
     * An Update event will be added to the message queue if the channel is up.
     * 
     * A SetUp event will do the same as a ChannelUp.
     * 
     * @param message The received message
     */
    @Override
    public void onReceive(Object message) {
        if (message instanceof ChannelDown && status == 1) {
            ChannelDown chdown = (ChannelDown) message;
            status = 0;
            messageQueue = new LinkedList<Update>();
            receiver.tell(message, getSelf());
            log.info("\n[{}]: Received {}", getSelf().path().name(), chdown);
            logState();
        } else if (message instanceof ChannelUp && status == 0) {
            ChannelUp chup = (ChannelUp) message;
            status = 1;
            mf.forward();
            receiver.tell(message, getSelf());
            log.info("\n[{}]: Received {}", getSelf().path().name(), chup);
            logState();
        } else if (message instanceof Update) {
            Update u = (Update) message;
            if (status == 1)
                messageQueue.add(u);
            log.info("\n[{}]: Received {}", getSelf().path().name(), u);
            logState();
        } else if (message instanceof SetUp) {
            SetUp sup = (SetUp) message;
            status = 1;
            mf.forward();
            receiver.tell(message, getSelf());
            log.info("\n[{}]: Received {}", getSelf().path().name(), sup);
            logState();
        }

    }

    /*
     * Called when the actor is terminated. Destroys the MessageForwarding thread.
     */
    @Override
    public void postStop() {
        log.info("\n[{}]: Terminating...", getSelf().path().name());
        mf.destroy();
    }
}