package network;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import events.ChannelDown;
import events.ChannelUp;
import events.SetUp;

/* 
* Network class. Holds the full network. 
*/
public class Network {

    public static int MAX_HOPS; /* Constant maximum number of hops between any node and its local leader */


    public Node[] nodes; /* Set of computing nodes */
    private ActorRef[][] channels; /* Set of communication channels */

    /*
     * Constructor. Initializes the variables. Creates the network based on the
     * topology graph
     * 
     * @param system ActorSystem where the actors will be created
     * 
     * @param topologyGraph Graph representing the topology of the network. If
     * topologyGraph[i][j] is 1, then both Channelij and Channelji are up. All other
     * channels are down.
     * 
     * @param globalDeltas Set of initial global deltas. globalDelta[i] is the
     * global delta for node i
     * 
     * @param globalLeader Id of the global leader of the system
     * 
     * @param localDeltas Set of initial local deltas. localDelta[i] is the local
     * delta for node i
     * 
     * @param localLeaders Set of initial local leaders. localLeaders[i] is the
     * local leader for node i
     */
    public Network(ActorSystem system, int[][] topologyGraph, int[] globalDeltas, int globalLeader, int[] localDeltas,
            int[] localLeaders, int maxHops) {
        Network.MAX_HOPS = maxHops;
        nodes = new Node[topologyGraph.length];
        channels = new ActorRef[topologyGraph.length][topologyGraph.length];

        // creating nodes
        for (int i = 0; i < topologyGraph.length; i++) {
            nodes[i] = new Node(system, i, globalDeltas[i], globalLeader, localDeltas[i], localLeaders[i],
                    topologyGraph.length);
        }

        // creating channels
        for (int i = 0; i < topologyGraph.length; i++) {
            for (int j = 0; j < topologyGraph.length; j++) {
                if (i != j) {
                    channels[i][j] = system.actorOf(Channel.createActor(nodes[i].nodeActor, nodes[j].nodeActor, 0),
                            "ch" + i + "" + j);
                }
            }
        }

        // turning up the channels according to topology graph
        for (int i = 0; i < topologyGraph.length; i++) {
            for (int j = 0; j < topologyGraph.length; j++) {
                if (topologyGraph[i][j] == 1) {
                    channels[i][j].tell(new SetUp(0, channels[j][i], i, nodes[i].getHeight()), ActorRef.noSender());
                }
            }
        }
    }

    /*
     * Sends ChannelDown event to the channels on both directions.
     * 
     * @param i Id of one of the nodes connected to the channels
     * 
     * @param j Id of the other node connected to the channels
     */
    public void dropChannel(int i, int j) {
        channels[i][j].tell(new ChannelDown(0, channels[j][i], i), ActorRef.noSender());
        channels[j][i].tell(new ChannelDown(0, channels[i][j], j), ActorRef.noSender());
    }

    /*
     * Sends ChannelUp event to the channels on both directions.
     * 
     * @param i Id of one of the nodes connected to the channels
     * 
     * @param j Id of the other node connected to the channels
     */
    public void remakeChannel(int i, int j) {
        channels[i][j].tell(new ChannelUp(0, channels[j][i], i), ActorRef.noSender());
        channels[j][i].tell(new ChannelUp(0, channels[i][j], j), ActorRef.noSender());
    }

}