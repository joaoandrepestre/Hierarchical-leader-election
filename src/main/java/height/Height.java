package height;

/* 
Height of a node. Used to orient links on the network. 
The direction of a link goes from the heighest to the lowest node.
*/
public class Height implements Comparable<Height> {
    public ReferenceLevel rl; /* Reference level */
    public int globalDelta; /*
                             * Orients links in the direction of a global search. When the network
                             * converges, holds the number of hops to the global leader.
                             */
    public LeaderPair globalLeaderPair; /* LeaderPair for the global leader */
    public int localDelta; /*
                            * Orients links in the direction of a local search. When the network converges,
                            * holds the number of hops to the local leader.
                            */
    public LeaderPair localLeaderPair; /* LeaderPair for the local leader */
    public int nodeId; /* Id of the node */

    /*
     * Constructor. Initializes the variables.
     * 
     * @param gd Global delta
     * 
     * @param nglts Negative global leader timestamp
     * 
     * @param glid Global leader id
     * 
     * @param ld Local delta
     * 
     * @param nllts Negative local leader timestamp
     * 
     * @param id Node id
     */
    public Height(int gd, int nglts, int glid, int ld, int nllts, int llid, int id) {
        rl = new ReferenceLevel();
        globalDelta = gd;
        globalLeaderPair = new LeaderPair(nglts, glid);
        localDelta = ld;
        localLeaderPair = new LeaderPair(nllts, llid);
        nodeId = id;
    }

    /*
     * Copy constructor. Copies the values of this Height to a new one.
     * 
     * @return The new Height
     */
    public Height copy() {
        Height tmp = new Height(globalDelta, 0, 0, localDelta, 0, 0, nodeId);
        tmp.rl = rl.copy();
        tmp.globalLeaderPair = globalLeaderPair.copy();
        tmp.localLeaderPair = localLeaderPair.copy();
        return tmp;
    }

    /*
     * Redefines the components of the Height to elect the given node as the global
     * leader.
     * 
     * @param timestamp Timestamp of when the node was elected
     * 
     * @param id Id of the elected node
     */
    public void electGlobal(int timestamp, int id) {
        rl = new ReferenceLevel();
        globalLeaderPair = new LeaderPair(-timestamp, id);
        globalDelta = 0;
        if (globalLeaderPair.leaderId != localLeaderPair.leaderId)
            electLocal(timestamp, id);
    }

    /*
     * Redefines the components of the Height to elect the given node as the local
     * leader.
     * 
     * @param timestamp Timestamp of when the node was elected
     * 
     * @param id Id of the elected node
     */
    public void electLocal(int timestamp, int id) {
        rl = new ReferenceLevel();
        localLeaderPair = new LeaderPair(-timestamp, id);
        localDelta = 0;
    }

    /*
     * Redefines the components of the Height to start a search for a global leader.
     * 
     * @param timestamp Timestamp of when the search was started
     * 
     * @param originId Id of the node that started the search
     */
    public void startNewReferenceLevelGlobal(int timestamp, int originId) {
        rl = new ReferenceLevel(timestamp, originId, 0, 0);
        globalDelta = 0;
    }

    /*
     * Redefines the components of the Height to start a search for a local leader.
     * 
     * @param timestamp Timestamp of when the search was started
     * 
     * @param originId Id of the node that started the search
     */
    public void startNewReferenceLevelLocal(int timestamp, int originId) {
        rl = new ReferenceLevel(timestamp, originId, 0, 1);
        localDelta = -1;
    }

    /*
     * Copies the given ReferenceLevel and reflects it. Redefines the deltas
     * accordingly.
     * 
     * @param rl The ReferenceLevel to be copied and relfected
     */
    public void reflectReferenceLevel(ReferenceLevel rl) {
        this.rl = rl.copy();
        this.rl.reflect();
        if (this.rl.localHops == 0) {
            globalDelta = 0;
        } else {
            localDelta = -1;
        }
    }

    /*
     * Creates a String representation of the height for logging purposes
     * 
     * @return The string representation
     */
    public String toString() {
        return "(" + rl + "," + globalDelta + "," + globalLeaderPair + "," + localDelta + "," + localLeaderPair + ","
                + nodeId + ")";
    }

    /*
     * Compares to another Height lexicographicaly.
     * 
     * @param h The compared Height
     * 
     * @return 0 if they are equal
     * 
     * @return 1 if this is greater than h
     * 
     * @return -1 if this is smaller than h
     */
    @Override
    public int compareTo(Height h) {
        if (rl.compareTo(h.rl) == 0) {
            if (globalDelta == h.globalDelta) {
                if (globalLeaderPair.compareTo(h.globalLeaderPair) == 0) {
                    if (localDelta == h.localDelta) {
                        if (localLeaderPair.compareTo(h.localLeaderPair) == 0) {
                            if (nodeId == h.nodeId) {
                                return 0;
                            } else if (nodeId < h.nodeId) {
                                return -1;
                            } else {
                                return 1;
                            }
                        } else if (localLeaderPair.compareTo(h.localLeaderPair) < 0) {
                            return -1;
                        } else {
                            return 1;
                        }
                    } else if (localDelta < h.localDelta) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else if (globalLeaderPair.compareTo(h.globalLeaderPair) < 0) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (globalDelta < h.globalDelta) {
                return -1;
            } else {
                return 1;
            }
        } else if (rl.compareTo(h.rl) < 0) {
            return -1;
        } else {
            return 1;
        }
    }
}