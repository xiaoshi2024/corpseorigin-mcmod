package xiaoshi2022.corpseorigin.growth;

public final class OrganPresetTest {
    private static int checks;
    public static void main(String[] args) {
        var body=new OrganSlot("demo_body","full_body",0,0,0,0,0,0,1,false);
        var arm=new OrganSlot("demo_arm","left_arm",0,0,0,0,0,0,1,false);
        check(OrganLibrary.parseSlots("[]").isEmpty());
        check(OrganLibrary.parseSlots(OrganLibrary.JSON.toJson(java.util.List.of(arm))).getFirst().equals(arm));
        check(OrganLibrary.parseSlots(OrganLibrary.JSON.toJson(java.util.List.of(body,arm))).size()==2);
        reject(OrganLibrary.JSON.toJson(java.util.List.of(body,body)));
        reject(OrganLibrary.JSON.toJson(java.util.Collections.nCopies(9,arm)));
        reject("[null]");
        reject("null");
        reject("x".repeat(8193));
        check(new OrganDefinition.Anatomy(2,1,2,.02f,100,true).valid());
        check(!new OrganDefinition.Anatomy(9,1,0,0,0,false).valid());
        check(!new OrganDefinition.Anatomy(1,Float.NaN,0,0,0,false).valid());
        check(new OrganDefinition.WaterJet(100,25,10,20,6,16,null).valid());
        check(new OrganDefinition.WaterJet(100,25,10,20,6,16,"mud").valid());
        check(new OrganDefinition.WaterJet(100,25,10,20,6,16,"sand").valid());
        check(!new OrganDefinition.WaterJet(100,25,10,0,6,16,"water").valid());
        check(!new OrganDefinition.WaterJet(100,25,10,20,6,16,"unknown").valid());
        System.out.println("Organ presets: "+checks+" regression checks passed.");
    }
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Preset regression");}
    private static void reject(String json){try{OrganLibrary.parseSlots(json);}catch(RuntimeException expected){checks++;return;}throw new AssertionError("Invalid preset accepted");}
}
