package xiaoshi2022.corpseorigin.skill.chapter;

public final class GourdBalanceTest {
    public static void main(String[] args) {
        check(GourdBalance.edible(8,20));
        check(GourdBalance.edible(9,20));
        check(!GourdBalance.edible(10,20));
        check(!GourdBalance.edible(11,20));
        check(GourdBalance.edible(20,100));
        check(GourdBalance.edible(49,100));
        check(!GourdBalance.edible(50,100));
        check(GourdBalance.edible(1,101));
        check(GourdBalance.edible(20,10000));
        check(GourdBalance.edible(4999,10000));
        check(!GourdBalance.edible(5000,10000));
        check(!GourdBalance.edible(0,20));
        check(!GourdBalance.edible(Float.NaN,20));
        check(GourdBalance.flesh(2)==8);
        check(GourdBalance.flesh(20)==30);
        check(GourdBalance.flesh(100)==60);
        check(GourdBalance.duration(5)>14+9*10);
        check(GourdBalance.duration(4)>14+3*20);
        check(GourdBalance.captureScale(0)==1f);
        check(GourdBalance.captureScale(8)==1f);
        check(GourdBalance.captureScale(23)<.6f && GourdBalance.captureScale(23)>.5f);
        check(Math.abs(GourdBalance.captureScale(38)-.06f)<.0001f);
        check(GourdBalance.captureScale(100)==GourdBalance.captureScale(38));
        check(GourdBalance.edible(20,20,true));
        check(GourdBalance.edible(12,20,true));
        check(!GourdBalance.edible(20,20,false));
        check(!GourdBalance.edible(40,40,true));
        check(GourdBalance.edible(16,40,true));
        check(!GourdBalance.edible(0,20,true));
        check(!GourdBalance.edible(Float.NaN,20,true));
        check(!GourdBalance.edible(20,Float.POSITIVE_INFINITY,true));
        check(GourdBalance.petFlesh(20)==15);
        check(GourdBalance.petFlesh(10000)==15);
        check(GourdBalance.petFlesh(2)==4);
        check(GourdBalance.transfer(120,590)==10);
        check(GourdBalance.transfer(120,600)==0);
        check(GourdBalance.transfer(120,0)==120);
        check(GourdBalance.transfer(0,0)==0);
        check(GourdBalance.PET_COOLDOWN==600 && GourdBalance.PET_CAPACITY==120);
        System.out.println("Gourd balance: 39 checks passed.");
    }
    private static void check(boolean value){if(!value)throw new AssertionError();}
}
