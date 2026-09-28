package xiaoshi2022.corpseorigin.skill.chapter;

/** Real world dimensions in blocks, shared by rendering, collision and excavation. */
public final class SwordQiRules {
    private SwordQiRules() {}
    public static int tier(int level) { return Math.clamp(level,1,20); }
    public static float height(int level) {
        int l=tier(level);
        return l<10 ? 4+l*2 : switch(l) {case 10->96;case 11->144;case 12->224;case 13->272;case 14->320;default->384+(l-15)*48;};
    }
    public static float depth(int level) { return tier(level)<10 ? 2+tier(level)*.6f : 16+(tier(level)-10)*3; }
    public static double range(int level) { int l=tier(level);return l<10?48+l*12:l<15?192+(l-10)*112:768+(l-15)*128; }
    public static int riftLength(int level) { int l=tier(level);return l<10?0:l<15?96+(l-10)*64:512+(l-15)*96; }
    public static int riftHeight(int level) { return tier(level)<10?0:Math.min(640,(int)height(level)+32); }
    public static int riftWidth(int level) { int l=tier(level);return l<10?0:l<15?5+(l-10)*2:19+(l-15)*2; }
    public static int slashes(int level) { return 5+tier(level); }
    /** Broad at the crest and narrower at the bottom, with a nonzero central cut. */
    public static int halfWidth(int width,int y,int height) {
        return Math.max(1,(int)Math.round((width-1)*.5*(.4+.6*Math.clamp(y/(double)Math.max(1,height-1),0,1))));
    }
}
