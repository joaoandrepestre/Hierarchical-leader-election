# Hierarchical Leader Election

Simulator of an Assynchronous Leader Election Algorithm for Dynamic Networks using the Akka framework for message passing and Processing for visualization.

**How to run it**

This project uses ant to build and run.

- Download the repository
- ant build to compile
- ant dist to create a runnable jar file
- ant run to run the program

OR

- Download the jar file in dist/
- java -jar HierarchicalLeaderElection.jar to run it

**The start menu**

When the simulator starts it will open a setup menu so you can set the initial state of the network in a few steps:

- Defining the number of nodes in the network
- Defining the maximum number of hops allowed between a node and its local leader
- Choosing a local leader for each node
- Determining the number of hops between each node and its local leader
- Choosing the global leader
- Determining the number of hops between each node and the global leader

After completing these steps, the simulator is ready to run!

**Dependencies**

- Akka-actor 2.12
- Akka-testkit 2.12
- com.typesafe.config 1.4
- scala-library 2.12
- Processing.core 3.3.7

All dependencies are included in the lib directory and bundled in the runnable jar

**For more information**

- Akka: https://akka.io/
- Processing: https://processing.org/
- The algorithm: coming soon...
