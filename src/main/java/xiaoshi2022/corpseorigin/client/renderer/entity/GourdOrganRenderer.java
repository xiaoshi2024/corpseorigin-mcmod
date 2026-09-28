package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.GourdOrganEntity;

public final class GourdOrganRenderer extends GeoEntityRenderer<GourdOrganEntity,LivingEntityRenderState>{
    private static final DataTicket<Integer> FORM=DataTicket.create("gourd_form",Integer.class);
    public GourdOrganRenderer(EntityRendererProvider.Context context){super(context,new DefaultedEntityGeoModel<GourdOrganEntity>(CorpseOrigin.id("zbr_gourd")){
        @Override public Identifier getTextureResource(GeoRenderState state){
            Integer form=state.getGeckolibData(FORM);String color=form==null?"dark":switch(form){case 2,6->"gold";case 4->"purple";case 5->"red";default->"dark";};
            return CorpseOrigin.id("textures/entity/zbr_gourd/"+color+".png");
        }
    });withScale(.45f);shadowRadius=.25f;}
    @Override public void extractRenderState(GourdOrganEntity entity,LivingEntityRenderState state,float partial){
        state.addGeckolibData(FORM,((GourdOrganEntity)entity).form());super.extractRenderState(entity,state,partial);
        state.addGeckolibData(xiaoshi2022.corpseorigin.client.render.GourdMouthAnchors.ENTITY,entity.getId());
    }
    @Override public void preRenderPass(com.geckolib.renderer.base.RenderPassInfo<LivingEntityRenderState> pass,net.minecraft.client.renderer.SubmitNodeCollector collector){super.preRenderPass(pass,collector);xiaoshi2022.corpseorigin.client.render.GourdMouthAnchors.listen(pass,false);}
}
