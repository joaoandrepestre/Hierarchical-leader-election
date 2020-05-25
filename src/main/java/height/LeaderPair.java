package height;

public class LeaderPair implements Comparable<LeaderPair> {
    public int negativeTimestamp;
    public int leaderId;

    public LeaderPair(int nglts, int glid){
        negativeTimestamp = nglts;
        leaderId = glid;
    }

    public LeaderPair copy(){
        return new LeaderPair(negativeTimestamp, leaderId);
    }

    public String toString(){
        return "("+negativeTimestamp+","+leaderId+")";
    }

    @Override
    public int compareTo(LeaderPair lp) {
        if(negativeTimestamp == lp.negativeTimestamp){
            if(leaderId == lp.leaderId){
                return 0;
            } else if(leaderId < lp.leaderId){
                return -1;
            }else{
                return 1;
            }
        } else if(negativeTimestamp < lp.negativeTimestamp){
            return -1;
        }else{
            return 1;
        }
    }
}