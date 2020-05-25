package height;

public class Height implements Comparable {
    public ReferenceLevel rl;
    public int globalDelta;
    public GlobalLeaderPair glp;
    public int localDelta;
    public LocalLeaderPair llp;
    public int nodeId;

    public Height(int gd, int nglts, int glid, int ld, int nllts, int llid, int id){
        rl = new ReferenceLevel();
        globalDelta = gd;
        glp = new GlobalLeaderPair(nglts, glid);
        localDelta = ld;
        llp = new LocalLeaderPair(nllts, llid);
        nodeId = id;
    }

    public String toString(){
        return "("+rl+","+globalDelta+","+glp+","+localDelta+","+llp+","+nodeId+")";
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}