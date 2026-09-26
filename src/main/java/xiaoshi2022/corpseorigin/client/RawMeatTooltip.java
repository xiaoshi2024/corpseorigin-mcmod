package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.skill.longyou.RawMeatDigestion;
import xiaoshi2022.corpseorigin.skill.longyou.RawMeatDigestionRules;

public final class RawMeatTooltip {
    private RawMeatTooltip() {}

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            var player = Minecraft.getInstance().player;
            if (player == null || !RawMeatDigestion.supports(player, stack)) return;
            if (RawMeatDigestion.mode(player) == RawMeatDigestion.INSTANT) {
                int amount = stack.is(ConventionalItemTags.RAW_MEAT_FOODS) ? 20 : 10;
                lines.add(Component.translatable("tooltip.corpseorigin.raw_meat.instant", amount)
                        .withStyle(ChatFormatting.RED));
            } else {
                lines.add(Component.translatable("tooltip.corpseorigin.raw_meat.slow",
                        RawMeatDigestionRules.BLOOD_PER_MEAT,
                        RawMeatDigestionRules.TICKS_PER_POINT / 20).withStyle(ChatFormatting.RED));
                lines.add(Component.translatable("tooltip.corpseorigin.raw_meat.pending",
                        player.getAttachedOrCreate(RawMeatDigestion.PENDING),
                        RawMeatDigestionRules.MAX_PENDING, RawMeatDigestion.accepted(player))
                        .withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.translatable("tooltip.corpseorigin.raw_meat.full_hunger")
                    .withStyle(ChatFormatting.GRAY));
        });
    }
}
