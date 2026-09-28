package xiaoshi2022.corpseorigin.item;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.growth.ThermalSurvey;
public final class ThermalScannerItem extends Item {
    public ThermalScannerItem(Properties p){super(p);}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){out.accept(Component.translatable("thermal.corpseorigin.hint"));}
    @Override public InteractionResult use(Level l,Player p,InteractionHand h){
        var stack=p.getItemInHand(h);if(p.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;
        if(p instanceof ServerPlayer sp){ThermalSurvey.report(sp);p.getCooldowns().addCooldown(stack,100);}
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack s,Player p,LivingEntity e,InteractionHand h){
        if(p.getCooldowns().isOnCooldown(s))return InteractionResult.FAIL;
        if(p instanceof ServerPlayer){
            String state=ThermalSurvey.isCorpse(e)?"corpse":e.hasEffect(xiaoshi2022.corpseorigin.registry.ModEffects.QIANS)?"infected":"clear";
            p.sendSystemMessage(Component.translatable("thermal.corpseorigin.identify",e.getDisplayName(),Component.translatable("thermal.corpseorigin."+state)));p.getCooldowns().addCooldown(s,20);
        }return InteractionResult.SUCCESS;
    }
}
