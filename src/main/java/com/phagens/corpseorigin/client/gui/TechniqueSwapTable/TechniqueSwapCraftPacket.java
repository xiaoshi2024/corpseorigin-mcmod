package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.Recipe.TechniqueSwapRecipe;
import com.phagens.corpseorigin.Recipe.TechniqueSwapRecipeManager;
import com.phagens.corpseorigin.block.entity.TechniqueSwapTableEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 功法兑换台合成请求包（客户端→服务端）
 */
public record TechniqueSwapCraftPacket(
        BlockPos blockPos,
        int selectedRecipeIndex
) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            CorpseOrigin.MODID, "technique_swap_craft");

    public static final Type<TechniqueSwapCraftPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<ByteBuf, TechniqueSwapCraftPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TechniqueSwapCraftPacket::blockPos,
            ByteBufCodecs.INT,
            TechniqueSwapCraftPacket::selectedRecipeIndex,
            TechniqueSwapCraftPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务端处理合成请求
     */
    public static void handle(TechniqueSwapCraftPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();

            if (level instanceof ServerLevel serverLevel) {
                if (serverLevel.getBlockEntity(packet.blockPos()) instanceof TechniqueSwapTableEntity tableEntity) {
                    // 在服务端重新匹配配方（防止作弊）
                    ItemStack[] inputItems = new ItemStack[6];
                    for (int i = 0; i < 6; i++) {
                        inputItems[i] = tableEntity.getItem(i).copy();
                    }

                    var matchedRecipes = TechniqueSwapRecipeManager.getInstance().findMatchingRecipes(inputItems);

                    if (packet.selectedRecipeIndex >= 0 && packet.selectedRecipeIndex < matchedRecipes.size()) {
                        TechniqueSwapRecipe recipe = matchedRecipes.get(packet.selectedRecipeIndex);

                        // 再次验证材料是否足够
                        if (recipe.matches(inputItems)) {
                            // 消耗材料
                            recipe.craft(inputItems);

                            // 写回消耗后的物品
                            for (int i = 0; i < 6; i++) {
                                tableEntity.setItem(i, inputItems[i]);
                            }

                            // 生成输出物品到输出槽
                            ItemStack output = recipe.getOutput().copy();
                            tableEntity.setItem(6, output);

                            // 标记BlockEntity需要保存和同步
                            tableEntity.setChanged();

                            CorpseOrigin.LOGGER.info("服务端合成完成: {}", output.toString());
                        }
                    }
                }
            }
        });
    }
}
