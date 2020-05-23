package height;

public class Height {
    public ReferenceLevel rl;
    public int globalDelta;
    public GlobalLeaderPair glp;
    public int localDelta;
    public LocalLeaderPair llp;
    public int nodeId;

    public Height(int nglts, int glid, int nllts, int llid, int id){
        rl = new ReferenceLevel();
        globalDelta = 0;
        glp = new GlobalLeaderPair(nglts, glid);
        localDelta = 0;
        llp = new LocalLeaderPair(nllts, llid);
        nodeId = id;
    }
}