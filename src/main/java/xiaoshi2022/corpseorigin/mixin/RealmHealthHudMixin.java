package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Locale;

/** Prevent millions of vanilla heart sprites while retaining hunger/armor/air information. */
@Mixin(Hud.class)
public class RealmHealthHudMixin {
    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void corpseorigin$compactHealth(GuiGraphicsExtractor g, CallbackInfo ci) {
        Minecraft mc=Minecraft.getInstance();
        if (!(mc.getCameraEntity() instanceof Player p) || p.getMaxHealth()<=200 && p.getAbsorptionAmount()<=200) return;
        ci.cancel();
        int x=g.guiWidth()/2-91,y=g.guiHeight()-52;
        float fraction=Math.clamp(p.getHealth()/p.getMaxHealth(),0,1);
        g.fill(x,y,x+182,y+8,0xcc301820);
        g.fill(x+1,y+1,x+1+(int)(180*fraction),y+7,0xffd33852);
        g.centeredText(mc.font,net.minecraft.network.chat.Component.translatable("realm.corpseorigin.hud.health",number(p.getHealth()),number(p.getMaxHealth())),x+91,y-10,0xffffffff);
        var info=net.minecraft.network.chat.Component.translatable("realm.corpseorigin.hud.armor",p.getArmorValue(),p.getFoodData().getFoodLevel());
        if(p.getAbsorptionAmount()>0) info.append(net.minecraft.network.chat.Component.translatable("realm.corpseorigin.hud.shield",number(p.getAbsorptionAmount())));
        if(p.getAirSupply()<p.getMaxAirSupply()) info.append(net.minecraft.network.chat.Component.translatable("realm.corpseorigin.hud.air",Math.max(0,p.getAirSupply())));
        g.centeredText(mc.font,info,x+91,y+10,0xffeeeecc);
    }
    private static String number(float n) {
        if(n>=1e8) return net.minecraft.client.resources.language.I18n.get("realm.corpseorigin.number.hundred_million",String.format(Locale.ROOT,"%.2f",n/1e8));
        if(n>=1e4) return net.minecraft.client.resources.language.I18n.get("realm.corpseorigin.number.ten_thousand",String.format(Locale.ROOT,"%.2f",n/1e4));
        return String.format(Locale.ROOT,"%.0f",n);
    }
}
