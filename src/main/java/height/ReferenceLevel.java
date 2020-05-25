package height;

public class ReferenceLevel implements Comparable<ReferenceLevel> {
    public int timestamp;
    public int originId;
    public int reflected;
    public int localHops;

    public ReferenceLevel() {
        timestamp = 0;
        originId = 0;
        reflected = 0;
        localHops = 0;
    }

    public ReferenceLevel(int t, int oid, int r, int lh) {
        timestamp = t;
        originId = oid;
        reflected = r;
        localHops = lh;
    }

    public ReferenceLevel copy(){
        return new ReferenceLevel(timestamp, originId, reflected, localHops);
    }

    public void reflect(){
        reflected=1;
    }

    public String toString() {
        return "(" + timestamp + "," + originId + "," + reflected + "," + localHops + ")";
    }

    @Override
    public int compareTo(ReferenceLevel rl) {
        if (timestamp == rl.timestamp) {
            if (originId == rl.originId) {
                if (reflected == rl.reflected) {
                    if (((localHops==0) && (rl.localHops==0)) || ((localHops>0) && (rl.localHops>0))) {
                        return 0;
                    } else if ((localHops==0) && (rl.localHops>0)) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else if (reflected < rl.reflected) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (originId < rl.originId) {
                return -1;
            } else {
                return 1;
            }
        } else if (timestamp < rl.timestamp) {
            return -1;
        } else {
            return 1;
        }
    }
}