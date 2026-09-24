package xiaoshi2022.corpseorigin.growth;
public final class OpportunityRulesTest {
    private static int checks;
    public static void main(String[] args){
        check(!OpportunityRules.lessonDue(1,3),"First lesson accumulates");
        check(!OpportunityRules.lessonDue(2,3),"Second lesson accumulates");
        check(OpportunityRules.lessonDue(3,3),"Third lesson discovers");
        check(OpportunityRules.lessonDue(6,3),"Repeated cycle");
        check(OpportunityRules.lessonDue(1,0),"Invalid interval bounded");
        check(!OpportunityRules.lessonDue(0,1),"No lesson no reward");
        check(!OpportunityRules.roll(0,0),"Zero chance disables");
        check(OpportunityRules.roll(.999,1),"One chance guarantees");
        check(OpportunityRules.roll(.49,.5),"Below threshold");
        check(!OpportunityRules.roll(.5,.5),"Threshold excluded");
        check(OpportunityRules.roll(.2,Double.NaN),"Invalid chance fallback");
        check(OpportunityRules.ready(0,Long.MIN_VALUE,12000),"First event immediate");
        check(!OpportunityRules.ready(11999,0,12000),"Cooldown blocked");
        check(OpportunityRules.ready(12000,0,12000),"Exact cooldown");
        check(OpportunityRules.ready(0,0,0),"Config disables cooldown");
        check(OpportunityRules.ready(10,20,12000),"Clock rollback recovery");
        System.out.println("Opportunity rules: "+checks+" regression checks passed.");
    }
    private static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
}
