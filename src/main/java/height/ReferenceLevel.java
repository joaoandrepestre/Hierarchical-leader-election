package height;

public class ReferenceLevel {
    public int timestamp;
    public int originId;
    public int reflected;
    public int localHops;

    public ReferenceLevel(){
        timestamp = 0;
        originId = 0;
        reflected = 0;
        localHops = 0;
    }

    private ReferenceLevel(int t, int oid, int r, int lh){
        timestamp = t;
        originId = oid;
        reflected = r;
        localHops = lh;
    }

    public ReferenceLevel startNewRefLevelGlobal(int t, int oid){
        return new ReferenceLevel(t, oid, 0, 0);
    }

    public ReferenceLevel startNewRefLevelLocal(int t, int oid){
        return new ReferenceLevel(t, oid, 0, 1);
    }
}