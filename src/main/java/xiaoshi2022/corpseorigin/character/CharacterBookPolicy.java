package xiaoshi2022.corpseorigin.character;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** The server is authoritative; the attachment only disables matching client UI rows. */
public final class CharacterBookPolicy {
    private CharacterBookPolicy(){}
    public static void init() {}
    public static final AttachmentType<String> DISABLED = AttachmentRegistry.create(
            xiaoshi2022.corpseorigin.CorpseOrigin.id("disabled_character_books"),
            b->b.initializer(()->"").syncWith(ByteBufCodecs.STRING_UTF8,AttachmentSyncPredicate.targetOnly()));
    public static Set<String> normalize(Collection<String> entries){
        if(entries==null)return Set.of();
        var out=new TreeSet<String>();
        for(String entry:entries){if(entry==null)continue;String id=entry.trim().toLowerCase(Locale.ROOT);if(id.startsWith("corpseorigin:"))id=id.substring(13);if(id.matches("[a-z0-9_]+"))out.add(id);}
        return Set.copyOf(out);
    }
    public static Set<String> serverDisabled(){return normalize(xiaoshi2022.corpseorigin.config.CorpseConfig.get().characterBooks.disabledCharacters);}
    public static void sync(ServerPlayer p){p.setAttached(DISABLED,String.join("\n",serverDisabled()));}
    public static boolean disabled(Player p,String id){return p.level().isClientSide()?Arrays.asList(p.getAttachedOrCreate(DISABLED).split("\n")).contains(id):serverDisabled().contains(id);}
    public static boolean allow(ServerPlayer p,String id){
        if(!disabled(p,id))return true;
        p.sendOverlayMessage(Component.translatable("message.corpseorigin.character_book.disabled",CharacterManager.getInstance().getCharacter(id).getName()));return false;
    }
}
