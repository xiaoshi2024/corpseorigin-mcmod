package xiaoshi2022.corpseorigin.growth;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import xiaoshi2022.corpseorigin.CorpseOrigin;
public record OrganEvolutionPayload(String organ,String action) implements CustomPacketPayload {
    public static final Type<OrganEvolutionPayload> TYPE=new Type<>(CorpseOrigin.id("organ_evolve"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OrganEvolutionPayload> CODEC=StreamCodec.ofMember(
        (p,b)->{b.writeUtf(p.organ,96);b.writeUtf(p.action,16);},b->new OrganEvolutionPayload(b.readUtf(96),b.readUtf(16)));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
