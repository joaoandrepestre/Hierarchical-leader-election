package height;

public class LocalLeaderPair {
    public int negativeTimestamp;
    public int localLeaderId;

    public LocalLeaderPair(int nllts, int llid){
        negativeTimestamp = nllts;
        localLeaderId = llid;
    }

    public String toString(){
        return "("+negativeTimestamp+","+localLeaderId+")";
    }
}