package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.client.ClientState;
import xiaoshi2022.corpseorigin.client.render.HeartPreviewEntity;
import xiaoshi2022.corpseorigin.entity.SkillConstructEntity;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;

/** Screen-owned preview: no world entity, network interpolation, or inventory item. */
@Mixin(InventoryScreen.class)
public abstract class InventoryHeartPreviewMixin extends AbstractContainerScreen<InventoryMenu> {
    @Unique private static final java.util.concurrent.atomic.AtomicInteger corpseorigin$nextPreviewId =
            new java.util.concurrent.atomic.AtomicInteger(-1_000_000);
    @Unique private SkillConstructEntity corpseorigin$heart;
    @Unique private Button corpseorigin$pageButton;
    @Unique private boolean corpseorigin$heartPage;

    protected InventoryHeartPreviewMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void corpseorigin$initPages(CallbackInfo ci) {
        corpseorigin$pageButton = addRenderableWidget(Button.builder(corpseorigin$pageLabel(), button -> {
            corpseorigin$heartPage = !corpseorigin$heartPage;
            button.setMessage(corpseorigin$pageLabel());
        }).bounds(leftPos + 25, topPos + 8, 51, 12).build());
        corpseorigin$pageButton.setTooltip(Tooltip.create(Component.translatable("gui.corpseorigin.preview.switch")));
        corpseorigin$pageButton.visible = corpseorigin$hasHeart();
    }

    @Unique
    private Component corpseorigin$pageLabel() {
        return Component.translatable(corpseorigin$heartPage
                ? "gui.corpseorigin.preview.heart_page" : "gui.corpseorigin.preview.player_page");
    }

    @Unique
    private boolean corpseorigin$hasHeart() {
        var player = minecraft.player;
        if (player == null || !player.isAlive() || !ClientState.hasLearned("black_gold_heart")) {
            return false;
        }
        // 黑小飞本人，或已经是尸兄的身体 —— 与服务端 HeartImplant.canBear 同一口径。
        // 客户端读不到 PLAYER_CORPSE 附件（它不参与同步），所以"尸兄"这半边走广播下来的缓存，
        // 和感染条 HUD 用的是同一份数据。
        if ("heixiaofei".equals(player.getAttachedOrCreate(ChapterActorState.ROLE))) {
            return true;
        }
        var self = xiaoshi2022.corpseorigin.client.CorpseOriginClient.corpseDataCache.get(player.getUUID());
        return self != null && self.isCorpse;
    }

    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void corpseorigin$updatePages(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                         float partialTick, CallbackInfo ci) {
        if (corpseorigin$pageButton == null) return;
        corpseorigin$pageButton.setPosition(leftPos + 26, topPos + 8);
        // Screen extracts the background BEFORE widgets. Reset here so the preview
        // redirect can enable the button before Screen extracts the widget list.
        // Resetting at extractRenderState HEAD hides it again just before drawing.
        corpseorigin$pageButton.visible = false;
        if (!corpseorigin$hasHeart()) {
            corpseorigin$heartPage = false;
            corpseorigin$heart = null;
            corpseorigin$pageButton.setMessage(corpseorigin$pageLabel());
        }
    }

    @Redirect(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"))
    private void corpseorigin$preview(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2,
                                     int scale, float offset, float mouseX, float mouseY,
                                     net.minecraft.world.entity.LivingEntity entity) {
        boolean available = corpseorigin$hasHeart();
        if (corpseorigin$pageButton != null) corpseorigin$pageButton.visible = available;
        if (!available || !corpseorigin$heartPage) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x1, y1, x2, y2,
                    scale, offset, mouseX, mouseY, entity);
            return;
        }
        var player = minecraft.player;
        if (corpseorigin$heart == null || corpseorigin$heart.level() != player.level()) {
            corpseorigin$heart = new HeartPreviewEntity(player.level());
            // Off-world previews never receive a level-assigned ID. ItemModelResolver still requires one.
            corpseorigin$heart.setId(corpseorigin$nextPreviewId.getAndDecrement());
        }
        // The renderer supplies the preview's own clock instead of vanilla's frozen GUI tick.
        corpseorigin$heart.setPos(player.position());
        // Replace the vanilla preview rather than drawing a second entity over it.
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x1, y1 + 13,
                x2, y2, 65, 0.0F, mouseX, mouseY, corpseorigin$heart);
    }
}
