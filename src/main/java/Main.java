import java.util.Scanner;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import events.*;
import height.Height;
import network.*;

/* 
* Main class. Holds the full network and runs the Akka actor system. 
*/
public class Main {

	private final ActorSystem system = ActorSystem.create("system"); /* Akka actor system */
	private ActorRef[] nodes; /* Set of computing nodes */
	private ActorRef[][] channels; /* Set of communication channels */

	/*
	 * Constructor. Initializes the variables. Creates the network based on the
	 * topology graph
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
	public Main(int[][] topologyGraph, int[] globalDeltas, int globalLeader, int[] localDeltas, int[] localLeaders) {
		nodes = new ActorRef[topologyGraph.length];
		channels = new ActorRef[topologyGraph.length][topologyGraph.length];
		Height[] heights = new Height[topologyGraph.length];

		// creating nodes
		for (int i = 0; i < topologyGraph.length; i++) {
			nodes[i] = system.actorOf(Node.createActor(i, globalDeltas[i], globalLeader, localDeltas[i],
					localLeaders[i], topologyGraph.length), "n" + i);
			heights[i] = new Height(globalDeltas[i], -1, globalLeader, localDeltas[i], -1, localLeaders[i], i);
		}

		// creating channels
		for (int i = 0; i < topologyGraph.length; i++) {
			for (int j = 0; j < topologyGraph.length; j++) {
				if (i != j) {
					channels[i][j] = system.actorOf(Channel.createActor(nodes[i], nodes[j], 0), "ch" + i + "" + j);
				}
			}
		}

		// turning up the channels according to topology graph
		for (int i = 0; i < topologyGraph.length; i++) {
			for (int j = 0; j < topologyGraph.length; j++) {
				if (topologyGraph[i][j] == 1) {
					channels[i][j].tell(new SetUp(0, channels[j][i], i, heights[i]), ActorRef.noSender());
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

	/*
	 * Terminates the system.
	 */
	public void terminate() {
		system.terminate();
	}

	public static void main(String[] args) {

		//Example 1: The graph we discussed in our meetings 
		/*int[][] topologyGraph = { { 0, 1, 1, 0, 0, 0 }, 
								  { 1, 0, 1, 1, 0, 0 }, 
								  { 1, 1, 0, 0, 1, 0 },
								  { 0, 1, 0, 0, 1, 0 }, 
								  { 0, 0, 1, 1, 0, 1 }, 
								  { 0, 0, 0, 0, 1, 0 } };
		int[] globalDeltas = { 3, 3, 2, 2, 1, 0 };
		int globalLeader = 5;
		int[] localDeltas = { 1, 1, 0, 2, 1, 0 };
		int[] localLeaders = { 2, 2, 2, 5, 5, 5 }; */

		// Exemple 2: Simpler 3 node clique, 0 is the only leader 
		int[][] topologyGraph = { { 0, 1, 1 }, 
								  { 1, 0, 1 }, 
								  { 1, 1, 0 } };
		int[] globalDeltas = { 0, 1, 1 };
		int globalLeader = 0;
		int[] localDeltas = { 0, 1, 1 };
		int[] localLeaders = { 0, 0, 0 };

		Main m = new Main(topologyGraph, globalDeltas, globalLeader, localDeltas, localLeaders);
		Scanner scanner = new Scanner(System.in);
		
		// Wait for the initial setup to finish (the actors will stop logging)
		// Press ENTER to continue
		scanner.nextLine(); 
		m.dropChannel(0, 1); // Dropping channel 0-1 should trigger a local search
		
		scanner.nextLine(); 
		m.dropChannel(0,2); // Dropping channel 0-2 should trigger a local search and then a global one
		
		scanner.nextLine(); 
		m.remakeChannel(0, 2); // Remaking channel 0-2 should change 0's leaders
		
		scanner.nextLine();
		scanner.close();
		m.terminate();
	}
}