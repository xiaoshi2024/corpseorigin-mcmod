package xiaoshi2022.corpseorigin.skill.chapter;

public final class GourdBalanceTest {
    public static void main(String[] args) {
        check(GourdBalance.edible(8,20));
        check(!GourdBalance.edible(9,20));
        check(GourdBalance.edible(20,100));
        check(!GourdBalance.edible(21,100));
        check(!GourdBalance.edible(1,101));
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
        System.out.println("Gourd balance: 25 checks passed.");
    }
    private static void check(boolean value){if(!value)throw new AssertionError();}
}
