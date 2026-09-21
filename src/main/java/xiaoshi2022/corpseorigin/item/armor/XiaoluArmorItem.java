package xiaoshi2022.corpseorigin.item.armor;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.google.common.base.Suppliers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.client.renderer.armor.XiaoluArmorRenderer;
import java.util.function.Consumer;

public class XiaoluArmorItem extends Item implements GeoItem {

 private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);

 public XiaoluArmorItem(ArmorMaterial m, ArmorType t, Properties p){
  super(p.humanoidArmor(m,t));GeoItem.registerSyncedAnimatable(this);
 }
 @Override public void createGeoRenderer(Consumer<GeoRenderProvider> c){
  c.accept(new GeoRenderProvider(){
   private final java.util.function.Supplier<XiaoluArmorRenderer<?>> r=Suppliers.memoize(XiaoluArmorRenderer::new);
   @Nullable public GeoArmorRenderer<?,?> getGeoArmorRenderer(ItemStack s, EquipmentSlot slot){
    return r.get();}});
 }
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {

 }
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){
  return cache;
 }
}
