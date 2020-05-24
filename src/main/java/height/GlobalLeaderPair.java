package height;

public class GlobalLeaderPair {
    public int negativeTimestamp;
    public int globalLeaderId;

    public GlobalLeaderPair(int nglts, int glid){
        negativeTimestamp = nglts;
        globalLeaderId = glid;
    }

    public String toString(){
        return "("+negativeTimestamp+","+globalLeaderId+")";
    }
}