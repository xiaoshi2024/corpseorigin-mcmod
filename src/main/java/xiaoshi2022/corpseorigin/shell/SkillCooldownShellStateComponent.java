package xiaoshi2022.corpseorigin.shell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemCooldowns;
import xiaoshi2022.corpseorigin.mixin.CooldownInstanceAccessor;
import xiaoshi2022.corpseorigin.mixin.ItemCooldownsAccessor;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.SkillManager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 技能冷却：跟着身体走。
 * <p>
 * 冷却有两套，都要搬：
 * <ul>
 *     <li>模组技能冷却（{@link SkillManager} 的静态表，按玩家 UUID 记，覆盖所有角色的技能）</li>
 *     <li>原版物品冷却（巨阙 / 大剑这类直接调 {@code getCooldowns()} 的技能）</li>
 * </ul>
 * 存的是"剩余 tick"，所以身体躺在仓里时冷却不会偷偷恢复，换回来接着走；
 * 换到另一具身体时，旧身体带走的冷却会在客户端清掉。
 */
public class SkillCooldownShellStateComponent extends ShellStateComponent {

    /** 技能路径 → 剩余 tick */
    private CompoundTag skillCooldowns = new CompoundTag();
    /** 冷却组 id → 剩余 tick */
    private CompoundTag itemCooldowns = new CompoundTag();

    public SkillCooldownShellStateComponent() {}

    public SkillCooldownShellStateComponent(ServerPlayer player) {
        this.capture(player);
    }

    @Override
    public String getId() { return "corpseorigin:skill_cooldown"; }

    @Override
    public void clone(ShellStateComponent component) {
        SkillCooldownShellStateComponent other = component.as(SkillCooldownShellStateComponent.class);
        if (other != null) {
            this.skillCooldowns = other.skillCooldowns.copy();
            this.itemCooldowns = other.itemCooldowns.copy();
        }
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        tag.put("Skills", this.skillCooldowns.copy());
        tag.put("Items", this.itemCooldowns.copy());
    }

    @Override
    public void readNbt(CompoundTag tag) {
        this.skillCooldowns = tag.getCompound("Skills").orElse(new CompoundTag());
        this.itemCooldowns = tag.getCompound("Items").orElse(new CompoundTag());
    }

    @Override
    public void applyTo(ServerPlayer player) {
        this.applySkillCooldowns(player);
        this.applyItemCooldowns(player);
    }

    // ==================== 抓取 ====================

    private void capture(ServerPlayer player) {
        CompoundTag skills = new CompoundTag();
        SkillManager.snapshotRemaining(player).forEach(skills::putInt);
        this.skillCooldowns = skills;

        CompoundTag items = new CompoundTag();
        ItemCooldowns cooldowns = player.getCooldowns();
        int now = ((ItemCooldownsAccessor) cooldowns).getTickCount();
        ((ItemCooldownsAccessor) cooldowns).getCooldowns().forEach((group, instance) -> {
            int remaining = ((CooldownInstanceAccessor) instance).getEndTime() - now;
            if (remaining > 0) {
                items.putInt(group.toString(), remaining);
            }
        });
        this.itemCooldowns = items;
    }

    // ==================== 应用 ====================

    private void applySkillCooldowns(ServerPlayer player) {
        Map<String, Integer> stored = readInts(this.skillCooldowns);
        Set<String> previous = new LinkedHashSet<>(SkillManager.snapshotRemaining(player).keySet());

        SkillManager.restoreRemaining(player, stored);
        stored.forEach((skillPath, ticks) ->
                CorpseNetwork.sendCooldownSync(player, skillPath, ticks));
        // 旧身体残留的冷却要在客户端清掉，否则 HUD 还显示着上一具身体的冷却
        previous.stream()
                .filter(skillPath -> !stored.containsKey(skillPath))
                .forEach(skillPath -> CorpseNetwork.sendCooldownSync(player, skillPath, 0));
    }

    private void applyItemCooldowns(ServerPlayer player) {
        ItemCooldowns cooldowns = player.getCooldowns();
        int now = ((ItemCooldownsAccessor) cooldowns).getTickCount();

        Set<Identifier> previous = new LinkedHashSet<>();
        ((ItemCooldownsAccessor) cooldowns).getCooldowns().forEach((group, instance) -> {
            if (((CooldownInstanceAccessor) instance).getEndTime() - now > 0) {
                previous.add(group);
            }
        });

        Map<Identifier, Integer> stored = new LinkedHashMap<>();
        for (String key : this.itemCooldowns.keySet()) {
            Identifier group = Identifier.tryParse(key);
            this.itemCooldowns.getInt(key).ifPresent(ticks -> {
                if (group != null) {
                    stored.put(group, ticks);
                }
            });
        }

        previous.stream()
                .filter(group -> !stored.containsKey(group))
                .forEach(cooldowns::removeCooldown);
        // addCooldown 会顺带把冷却同步给客户端
        stored.forEach((group, ticks) -> {
            if (ticks > 0) {
                cooldowns.addCooldown(group, ticks);
            }
        });
    }

    private static Map<String, Integer> readInts(CompoundTag tag) {
        Map<String, Integer> result = new HashMap<>();
        for (String key : tag.keySet()) {
            tag.getInt(key).ifPresent(value -> result.put(key, value));
        }
        return result;
    }
}
