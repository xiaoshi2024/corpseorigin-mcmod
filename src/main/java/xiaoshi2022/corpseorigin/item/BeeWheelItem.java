package xiaoshi2022.corpseorigin.item;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import com.geckolib.animatable.*;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import xiaoshi2022.corpseorigin.skill.SkillManager;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.entity.SkillConstructEntity;
public class BeeWheelItem extends Item implements GeoItem {
 private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
 public BeeWheelItem(Properties properties){super(properties);}
 @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
  if(player instanceof ServerPlayer p)return SkillManager.activate(p,"tiger_claw_bee_wheel")?InteractionResult.SUCCESS:InteractionResult.FAIL;
  return InteractionResult.SUCCESS;
 }
 public static void throwWheel(ServerPlayer p){
  var wheel=SkillConstructEntity.spawn(p,ModEntities.BEE_WHEEL,null,35);
  wheel.setDeltaMovement(p.getLookAngle().scale(1.5));wheel.hurtMarked=true;
 }
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
 @Override public void createGeoRenderer(java.util.function.Consumer<GeoRenderProvider> consumer){
  consumer.accept(new GeoRenderProvider(){
   private com.geckolib.renderer.GeoItemRenderer<BeeWheelItem> renderer;
   @Override public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer(){
    if(renderer==null)renderer=new com.geckolib.renderer.GeoItemRenderer<>(new com.geckolib.model.DefaultedItemGeoModel<>(xiaoshi2022.corpseorigin.CorpseOrigin.id("bee_wheel")));
    return renderer;
   }
  });
 }
}
