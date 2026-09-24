package xiaoshi2022.corpseorigin.growth;

public final class OrganSlotTest {
    public static void main(String[] args) {
        for(String joint:OrganSlot.JOINTS) check(new OrganSlot("bat_red",joint,0,0,0,0,0,0,1,false).valid());
        check(!new OrganSlot("a","unknown",0,0,0,0,0,0,1,false).valid());
        check(!new OrganSlot("a","body",Float.NaN,0,0,0,0,0,1,false).valid());
        check(!new OrganSlot("a","body",0,0,0,0,0,0,Float.POSITIVE_INFINITY,false).valid());
        check(!new OrganSlot("a","body",49,0,0,0,0,0,1,false).valid());
        check(!new OrganSlot("a","body",0,0,0,181,0,0,1,false).valid());
        check(!new OrganSlot("a","body",0,0,0,0,0,0,0,false).valid());
        check(new OrganSlot("a","body",-48,48,0,-180,180,0,.1f,true).valid());
        check(new OrganSlot("a","body",0,0,0,0,0,0,3,false).valid());
        check(new OrganSlot("body","full_body",0,0,0,0,0,0,1,false).replacesBody());
        check(!new OrganSlot("body","body",0,0,0,0,0,0,1,false).replacesBody());
        System.out.println("Organ transform: 17 regression checks passed.");
    }
    private static void check(boolean b){if(!b)throw new AssertionError("Organ transform validation failed");}
}
