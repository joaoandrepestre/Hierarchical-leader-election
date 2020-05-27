package height;

/* 
Leader Pair component of height. Used to identify the global and local leaders of a node.
*/
public class LeaderPair implements Comparable<LeaderPair> {
    public int negativeTimestamp; /*
                                   * Negative timestamp of when the leader was elected. It's important that it is
                                   * negative so that the most recent LeaderPair will be the smallest.
                                   */
    public int leaderId; /* Id of the leader node */

    /*
     * Constructor. Initializes the variables
     * 
     * @param nlts Negative timestamp
     * 
     * @param lid Leader id
     */
    public LeaderPair(int nlts, int lid) {
        negativeTimestamp = nlts;
        leaderId = lid;
    }

    /*
     * Copy constructor. Copies the values of this LeaderPair to a new one.
     * 
     * @return The new LeaderPair
     */
    public LeaderPair copy() {
        return new LeaderPair(negativeTimestamp, leaderId);
    }

    /*
     * Creates a String representation of the height component for logging purposes
     * 
     * @return The string representation
     */
    public String toString() {
        return "(" + negativeTimestamp + "," + leaderId + ")";
    }

    /*
     * Compares to another LeaderPair lexicographicaly.
     * 
     * @param lp The compared LeaderPair
     * 
     * @return 0 if they are equal
     * 
     * @return 1 if this is greater than lp
     * 
     * @return -1 if this is smaller than lp
     */
    @Override
    public int compareTo(LeaderPair lp) {
        if (negativeTimestamp == lp.negativeTimestamp) {
            if (leaderId == lp.leaderId) {
                return 0;
            } else if (leaderId < lp.leaderId) {
                return -1;
            } else {
                return 1;
            }
        } else if (negativeTimestamp < lp.negativeTimestamp) {
            return -1;
        } else {
            return 1;
        }
    }
}