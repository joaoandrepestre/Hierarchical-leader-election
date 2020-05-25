import java.io.IOException;
import java.util.Scanner;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import events.*;
import network.*;

public class Main {

	private final ActorSystem system = ActorSystem.create("system");
	private ActorRef[] nodes;
	private ActorRef[][] channels;

	public Main(int[][] topologyGraph, int[] globalDeltas, int globalLeader, int[] localDeltas, int[] localLeaders) {
		nodes = new ActorRef[topologyGraph.length];
		channels = new ActorRef[topologyGraph.length][topologyGraph.length];

		for (int i = 0; i < topologyGraph.length; i++) {
			nodes[i] = system.actorOf(
					Node.createActor(i, globalDeltas[i], globalLeader, localDeltas[i], localLeaders[i], 6), "n" + i);
		}

		for (int i = 0; i < topologyGraph.length; i++) {
			for (int j = 0; j < topologyGraph.length; j++) {
				if (i != j) {
					channels[i][j] = system.actorOf(Channel.createActor(nodes[i], nodes[j], 0), "ch" + i + "" + j);
				}
			}
		}

		for (int i = 0; i < topologyGraph.length; i++) {
			for (int j = 0; j < topologyGraph.length; j++) {
				if (topologyGraph[i][j] == 1) {
					channels[i][j].tell(new ChannelUp(0, channels[j][i], i), ActorRef.noSender());
				}
			}
		}
	}

	public void dropChannel(int i, int j){
		channels[i][j].tell(new ChannelDown(0, channels[j][i], i), ActorRef.noSender());
		channels[j][i].tell(new ChannelDown(0, channels[i][j], j), ActorRef.noSender());

	}

	public void terminate() {
		system.terminate();
	}

	public static void main(String[] args) {
		int[][] topologyGraph = { { 0, 1, 1, 0, 0, 0 }, 
								  { 1, 0, 1, 1, 0, 0 }, 
								  { 1, 1, 0, 0, 1, 0 },
								  { 0, 1, 0, 0, 1, 0 }, 
								  { 0, 0, 1, 1, 0, 1 }, 
								  { 0, 0, 0, 0, 1, 0 } };
		int[] globalDeltas = { 3, 3, 2, 2, 1, 0 };
		int[] localDeltas = { 1, 1, 0, 2, 1, 0 };
		int[] localLeaders = { 2, 2, 2, 5, 5, 5 };
		Main m = new Main(topologyGraph, globalDeltas, 5, localDeltas, localLeaders);
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		//m.dropChannel(0, 2);
		scanner.nextLine();
		m.terminate();
	}
}