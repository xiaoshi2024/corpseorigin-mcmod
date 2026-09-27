package xiaoshi2022.corpseorigin.growth;

public final class BalanceRulesTest {
    private static int checks;
    public static void main(String[] args){
        check(BalanceRules.executeDamage(21,100,30,.2f,20)==0);
        check(BalanceRules.executeDamage(20,100,30,.2f,20)==20);
        check(BalanceRules.executeDamage(2000,10000,1000,.2f,20)==20);
        check(BalanceRules.executeDamage(0,100,30,.2f,20)==0);
        check(BalanceRules.executeDamage(Float.NaN,100,30,.2f,20)==0);
        check(BalanceRules.executeDamage(1,0,30,.2f,20)==0);
        check(BalanceRules.executeDamage(1,100,2,.2f,20)==3);
        check(BalanceRules.bounded(Float.NaN,.35f,.05f,1)==.35f);
        check(BalanceRules.bounded(-1,.35f,.05f,1)==.05f);
        var window=new BalanceRules.Window();
        check(window.take(100,3,4)==3);
        check(window.take(100,3,4)==1);
        check(window.take(119,10,4)==0);
        check(window.take(120,10,4)==4);
        check(window.take(121,-10,4)==0);
        check(window.take(80,10,4)==4);
        check(window.take(100,10,0)==0);
        check(BalanceRules.discoveryLevel(3,true)==9);
        check(BalanceRules.discoveryLevel(12,true)==12);
        check(BalanceRules.discoveryLevel(2,false)==2);
        check(BalanceRules.swordQiDamage(10000,100,15,15)==25);
        check(BalanceRules.swordQiDamage(10000,100,1,15)==10);
        check(BalanceRules.swordQiDamage(10000,100,20,1)<=60.001f);
        check(BalanceRules.swordQiDamage(10,100,1,15)<2);
        check(BalanceRules.swordQiDamage(10,100,15,15)==10);
        check(BalanceRules.swordQiDamage(100,100,15,0)==100);
        check(BalanceRules.swordQiDamage(Float.NaN,100,15,15)==0);
        check(BalanceRules.swordQiDamage(Float.POSITIVE_INFINITY,100,15,15)==0);
        check(BalanceRules.swordQiDamage(-10,100,15,15)==0);
        System.out.println("Balance rules: "+checks+" regression checks passed.");
    }
    private static void check(boolean result){checks++;if(!result)throw new AssertionError("Balance check "+checks);}
}
