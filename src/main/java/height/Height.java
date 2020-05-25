package height;

public class Height implements Comparable<Height> {
    public ReferenceLevel rl;
    public int globalDelta;
    public LeaderPair globalLeaderPair;
    public int localDelta;
    public LeaderPair localLeaderPair;
    public int nodeId;

    public Height(int gd, int nglts, int glid, int ld, int nllts, int llid, int id) {
        rl = new ReferenceLevel();
        globalDelta = gd;
        globalLeaderPair = new LeaderPair(nglts, glid);
        localDelta = ld;
        localLeaderPair = new LeaderPair(nllts, llid);
        nodeId = id;
    }

    public Height copy() {
        Height tmp = new Height(globalDelta, 0, 0, localDelta, 0, 0, nodeId);
        tmp.rl = rl.copy();
        tmp.globalLeaderPair = globalLeaderPair.copy();
        tmp.localLeaderPair = localLeaderPair.copy();
        return tmp;
    }

    public void electGlobal(int timestamp, int id) {
        rl = new ReferenceLevel();
        globalLeaderPair = new LeaderPair(-timestamp, id);
        globalDelta = 0;
        if (globalLeaderPair.leaderId != localLeaderPair.leaderId)
            electLocal(timestamp, id);
    }

    public void electLocal(int timestamp, int id) {
        rl = new ReferenceLevel();
        localLeaderPair = new LeaderPair(-timestamp, id);
        localDelta = 0;
    }

    public void startNewReferenceLevelGlobal(int timestamp, int originId) {
        rl = new ReferenceLevel(timestamp, originId, 0, 0);
        globalDelta = 0;
    }

    public void startNewReferenceLevelLocal(int timestamp, int originId) {
        rl = new ReferenceLevel(timestamp, originId, 0, 1);
        localDelta = Integer.MAX_VALUE;
    }

    public void reflectReferenceLevel() {
        rl.reflect();
        if (rl.localHops == 0) {
            globalDelta = 0;
        } else {
            localDelta = 0;
        }
    }

    public String toString() {
        return "(" + rl + "," + globalDelta + "," + globalLeaderPair + "," + localDelta + "," + localLeaderPair+ "," + nodeId + ")";
    }

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
                        } else if (localLeaderPair.compareTo(h.localLeaderPair)< 0) {
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