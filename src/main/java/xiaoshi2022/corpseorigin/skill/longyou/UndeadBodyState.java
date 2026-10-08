package xiaoshi2022.corpseorigin.skill.longyou;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.jingang_zb.PeelShellSkill;

/** 0 normal; 1 scales; 2 sealed; 3 inside ship; 4 evolved second stage. */
public final class UndeadBodyState {
    public static final AttachmentType<Integer> STATE = AttachmentRegistry.create(CorpseOrigin.id("undead_body_state"),
            b -> b.initializer(() -> 0).persistent(com.mojang.serialization.Codec.INT)
                    .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));
    public static boolean sealed(LivingEntity e) { return e.getAttachedOrCreate(STATE) == 2; }
    public static boolean insideShip(LivingEntity e) { return e.getAttachedOrCreate(STATE) == 3; }
    public static void enterShip(ServerPlayer p) { p.setAttached(STATE, 3); p.setInvulnerable(true); }
    public static void evolveInside(ServerPlayer p) { if (insideShip(p)) p.setAttached(STATE, 4); }
    public static void escapeShip(ServerPlayer p) { p.setInvulnerable(false); p.setAttached(STATE, 4); }
    public static boolean eligible(ServerPlayer p) {
        return !p.getAttachedOrCreate(BodyPossession.SKIN).isEmpty()
                || "longyou".equals(PlayerCharacterData.get(p).getCharacterId(p.getUUID()));
    }

    // ==================== 动态技能（进入不死髅体后临时拥有） ====================

    private static final ISkill XUANWU_BODY = new XuanwuBodySkill();
    private static final ISkill PEEL_SHELL = new PeelShellSkill();
    private static final ISkill JINGANG_INFANT = new JingangInfantConvergenceSkill();

    /**
     * 少教主进入龙右借给他的不死髅体（STATE == 1）后，临时获得这两个躯体技能。
     * 不在这个状态就返回 null —— SkillManager 会当成“找不到技能定义”处理。
     */
    public static ISkill getDynamicSkill(ServerPlayer p, String path) {
        if (p.getAttachedOrCreate(BodyPossession.SKIN).isEmpty()) return null;
        return switch (path) {
            case "xuanwu_body" -> XUANWU_BODY;
            case "peel_shell" -> PEEL_SHELL;
            case "jingang_infant_convergence" -> JINGANG_INFANT;
            default -> null;
        };
    }

    public static void toggle(ServerPlayer p) {
        if (!eligible(p) || sealed(p)) return;
        int next = p.getAttachedOrCreate(STATE) == 1 ? 0 : 1;
        p.setAttached(STATE, next);

        PlayerCharacterData data = PlayerCharacterData.get(p);
        if (next == 1) {
            applyXuanwuAttributes(p);                    // ★ 披甲：套数值
            data.learnSkill(p.getUUID(), "xuanwu_body");
            data.learnSkill(p.getUUID(), "peel_shell");
        } else {
            removeXuanwuAttributes(p);                   // ★ 脱甲：摘数值
        }
        CorpseNetwork.sendEvolutionSync(p);
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((e, source, base, taken, blocked) -> {
            if (blocked || taken <= 0 || !(e instanceof ServerPlayer p) || !eligible(p)) return;
            if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                    && attacker.getMainHandItem().is(ModItems.RED_METEOR_SWORD) && !sealed(p)) {
                p.setAttached(STATE, 2);
                removeXuanwuAttributes(p);                       // ★ 封印：数值一并撤掉
                p.sendSystemMessage(Component.translatable("body.corpseorigin.sealed"));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!eligible(p)) {
                    p.setAttached(STATE, 0);
                    removeXuanwuAttributes(p);           // ★ 不再合格：摘数值
                    tickAutoInfection(p, 0);              // ★ 离体结束：收掉感染源泉
                    continue;
                }
                int state = p.getAttachedOrCreate(STATE);
                // ★ 状态自洽：STATE == 1 就该有加成，其它状态就不该有
                if (state == 1) {
                    applyXuanwuAttributes(p);
                } else {
                    removeXuanwuAttributes(p);
                }
                // ★ 不死髅体离体自动释放感染源泉：STATE == 1（披甲/夺舍）时肉体自动展开感染领域
                tickAutoInfection(p, state);
                if (p.isAlive() && !sealed(p) && p.tickCount % 20 == 0) p.heal(1);
            }
        });
    }

    /**
     * 不死髅体离体自动感染源泉（用户 2026-10-08 确认）。
     * <p>
     * 当玩家本体离体（{@link #STATE} == 1，即夺舍/披甲状态）时，
     * 肉体会自动展开"感染领域"作为感染源泉，不受 {@link InfectionDomainSkill#REQUIRED_REALM} 门槛限制 ——
     * 这是"不死髅体本身就是行走的感染源"的设定体现。
     * <p>
     * <b>自动模式 = 移动跟随</b>：领域跟着玩家走，不需要手动激活。
     * 玩家手动激活过的领域（含锚点模式）不会被自动逻辑覆盖；
     * 只有"未手动开启"且"处于离体状态"时才自动开。
     * <p>
     * <b>关闭时机</b>：离体结束（STATE 回到 0/2/3/4）时自动收掉自动开的领域。
     * 如果玩家在离体期间手动激活过，按手动逻辑走，不会被自动关闭。
     *
     * @param p     玩家
     * @param state 当前 {@link UndeadBodyState#STATE} 值
     */
    private static void tickAutoInfection(ServerPlayer p, int state) {
        boolean detached = (state == 1);
        boolean active = InfectionDomainHandler.isActive(p.getUUID());
        if (detached && !active) {
            // 离体且未手动开启 → 自动开启感染源泉（移动跟随模式）
            InfectionDomainHandler.toggle(p, false);
            // 标记为自动开启，便于离体结束时收掉
            p.setAttached(AUTO_INFECTION, true);
        } else if (!detached && active && p.getAttachedOrCreate(AUTO_INFECTION)) {
            // 离体结束且是自动开的 → 自动收掉
            InfectionDomainHandler.release(p.getUUID());
            p.setAttached(AUTO_INFECTION, false);
        }
    }

    /** 标记当前感染领域是"离体自动开"的，便于离体结束时收掉，不误伤玩家手动开的 */
    public static final AttachmentType<Boolean> AUTO_INFECTION =
            AttachmentRegistry.create(CorpseOrigin.id("auto_infection"),
                    b -> b.initializer(() -> false).persistent(com.mojang.serialization.Codec.BOOL)
                            .syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

    // ==================== 玄武体的数值加成 ====================

    private static final Identifier XUANWU_ARMOR     = CorpseOrigin.id("xuanwu_armor");
    private static final Identifier XUANWU_TOUGHNESS = CorpseOrigin.id("xuanwu_toughness");
    private static final Identifier XUANWU_KNOCKBACK = CorpseOrigin.id("xuanwu_knockback");
    private static final Identifier XUANWU_SPEED     = CorpseOrigin.id("xuanwu_speed");

    private static final double XUANWU_ARMOR_VALUE     = 18.0;
    private static final double XUANWU_TOUGHNESS_VALUE = 8.0;
    private static final double XUANWU_KNOCKBACK_VALUE = 0.6;

    /** 披上玄武鳞甲：套数值加成。重复调用安全（同 id 原地替换）。 */
    public static void applyXuanwuAttributes(LivingEntity e) {
        setAttr(e, Attributes.ARMOR,               XUANWU_ARMOR,     XUANWU_ARMOR_VALUE);
        setAttr(e, Attributes.ARMOR_TOUGHNESS,     XUANWU_TOUGHNESS, XUANWU_TOUGHNESS_VALUE);
        setAttr(e, Attributes.KNOCKBACK_RESISTANCE,XUANWU_KNOCKBACK, XUANWU_KNOCKBACK_VALUE);
        // Clear the persisted movement penalty from older versions, even while armored.
        removeAttr(e, Attributes.MOVEMENT_SPEED, XUANWU_SPEED);
    }

    /** 脱掉玄武鳞甲：摘掉数值加成。 */
    public static void removeXuanwuAttributes(LivingEntity e) {
        removeAttr(e, Attributes.ARMOR,               XUANWU_ARMOR);
        removeAttr(e, Attributes.ARMOR_TOUGHNESS,     XUANWU_TOUGHNESS);
        removeAttr(e, Attributes.KNOCKBACK_RESISTANCE,XUANWU_KNOCKBACK);
        removeAttr(e, Attributes.MOVEMENT_SPEED,      XUANWU_SPEED);
    }

    private static void setAttr(LivingEntity e, Holder<Attribute> attr, Identifier id, double target) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return;
        double delta = target - inst.getBaseValue();
        AttributeModifier old = inst.getModifier(id);
        if (old != null && old.operation() == AttributeModifier.Operation.ADD_VALUE && old.amount() == delta) return;
        inst.addOrReplacePermanentModifier(new AttributeModifier(id, delta, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeAttr(LivingEntity e, Holder<Attribute> attr, Identifier id) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst != null) inst.removeModifier(id);
    }
}
