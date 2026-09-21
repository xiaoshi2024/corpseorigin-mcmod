package xiaoshi2022.corpseorigin.client.skin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.core.ClientAsset;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import java.util.*;
import java.util.function.Supplier;

public final class ChameleonSkins {
    public static final PlayerSkin XIAOHUI=new PlayerSkin(new ClientAsset.ResourceTexture(CorpseOrigin.id("entity/xiaohuiskin")),null,null,PlayerModelType.WIDE,false);
    private static final Map<GameProfile,Supplier<PlayerSkin>> LOOKUPS=new HashMap<>();
    private ChameleonSkins(){}
    public static PlayerSkin resolve(GameProfile profile){
        return LOOKUPS.computeIfAbsent(profile,p->Minecraft.getInstance().getSkinManager().createLookup(p,false)).get();
    }
    public static void clear(){LOOKUPS.clear();}
}
