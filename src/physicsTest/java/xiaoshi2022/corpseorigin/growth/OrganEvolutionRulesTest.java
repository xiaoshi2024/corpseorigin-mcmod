package xiaoshi2022.corpseorigin.growth;
public final class OrganEvolutionRulesTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        check(!OrganEvolutionRules.mayAdvance(false,0,20,4),"High level needs points");
        check(OrganEvolutionRules.mayAdvance(false,0,1,5),"Unlock exact price");
        check(!OrganEvolutionRules.mayAdvance(false,1,4,100),"II needs level five");
        check(OrganEvolutionRules.mayAdvance(false,1,5,10),"II exact gates");
        check(!OrganEvolutionRules.mayAdvance(false,2,8,100),"III needs level nine");
        check(OrganEvolutionRules.mayAdvance(false,2,9,20),"III exact gates");
        check(OrganEvolutionRules.mayAdvance(true,2,1,0),"Creative bypass");
        check(!OrganEvolutionRules.mayAdvance(true,3,1,0),"Duplicate final stage cannot charge");
        check(!OrganEvolutionRules.mayAllocate(false,1,0,100),"Attribute needs II");
        check(OrganEvolutionRules.mayAllocate(false,2,0,2),"First allocation");
        check(!OrganEvolutionRules.mayAllocate(false,2,4,5),"Rising allocation price");
        check(!OrganEvolutionRules.mayAllocate(false,3,5,100),"Survival rank cap");
        check(OrganEvolutionRules.mayAllocate(true,0,50,0),"Creative beyond survival cap");
        String source=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/xiaoshi2022/corpseorigin/growth/SurvivalGrowth.java"));
        check(!source.contains("AFTER_DEATH.register")&&!source.contains("GrowthRules.inherit"),"Random prey inheritance removed");
        check(!source.contains("consume(player,"),"Raw fish organ unlock removed");
        System.out.println("Organ evolution: "+checks+" regression checks passed.");
    }
    private static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
}
