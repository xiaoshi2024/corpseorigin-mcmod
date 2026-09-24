package xiaoshi2022.corpseorigin.skill.chapter;

/** Bounded biological conversion and cast timings, shared with regression tests. */
public final class GourdBalance {
    private GourdBalance() {}
    public static int duration(int form){return switch(form){case 1->400;case 2->90;case 3->100;case 4->100;case 5->140;case 6->240;default->20;};}
    public static boolean edible(float health,float maxHealth){return Float.isFinite(health)&&Float.isFinite(maxHealth)&&health>0&&maxHealth>0&&maxHealth<=100&&health<=Math.min(20,maxHealth*.4f);}
    public static boolean edible(float health,float maxHealth,boolean villager){
        if(villager && Float.isFinite(health) && Float.isFinite(maxHealth)
                && health>0 && health<=maxHealth && maxHealth<=20)return true;
        return edible(health,maxHealth);
    }
    public static int flesh(float maxHealth){return Math.clamp((int)Math.ceil(maxHealth*1.5),8,60);}
    public static float captureScale(int age){return (float)(1-.94*Math.clamp((age-8)/30.0,0,1));}
}
