package height;

/* 
Reference Level component of height. This vector is used in the search for leaders in the network.
*/
public class ReferenceLevel implements Comparable<ReferenceLevel> {
    public int timestamp; /* Timestamp of when the search was started. */
    public int originId; /* Id of the node that started the search */
    public int reflected; /* Reflected flag. If 0 the search hasn't been reflected, if 1 it has */
    public int localHops; /*
                           * Number of hops taken in the search of a local leader. If the search is
                           * global, this number is 0
                           */

    /*
     * Constructor. Initializes the variables. All values to 0 represent that a
     * search isn't happening
     */
    public ReferenceLevel() {
        timestamp = 0;
        originId = 0;
        reflected = 0;
        localHops = 0;
    }

    /*
     * Constructor. Initializes the variables.
     * 
     * @param t The timestamp
     * 
     * @param oid The origin id
     * 
     * @param The reflected flag
     * 
     * @param The number of hops
     */
    public ReferenceLevel(int t, int oid, int r, int lh) {
        timestamp = t;
        originId = oid;
        reflected = r;
        localHops = lh;
    }

    /*
     * Copy constructor. Copies the values of this ReferenceLevel to a new one.
     * 
     * @return The new ReferenceValue
     */
    public ReferenceLevel copy() {
        return new ReferenceLevel(timestamp, originId, reflected, localHops);
    }

    /*
     * Reflects the search switching the flag to 1;
     */
    public void reflect() {
        reflected = 1;
    }

    /*
     * Creates a String representation of the height component for logging purposes
     * 
     * @return The string representation
     */
    public String toString() {
        return "(" + timestamp + "," + originId + "," + reflected + "," + localHops + ")";
    }

    /*
     * Compares to another ReferenceLevel lexicographicaly. For the purposes of this
     * comparison, all localHops greater than 0 are the same
     * 
     * @param rl The compared ReferenceLevel
     * 
     * @return 0 if they are equal
     * 
     * @return 1 if this is greater than rl
     * 
     * @return -1 if this is smaller than rl
     */
    @Override
    public int compareTo(ReferenceLevel rl) {
        if (timestamp == rl.timestamp) {
            if (originId == rl.originId) {
                if (reflected == rl.reflected) {
                    if (((localHops == 0) && (rl.localHops == 0)) || ((localHops > 0) && (rl.localHops > 0))) {
                        return 0;
                    } else if ((localHops == 0) && (rl.localHops > 0)) {
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