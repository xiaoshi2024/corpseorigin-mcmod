package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/**
 * 大叔（少女漫画家）- NPC。
 * <p>
 * 平时胆小怕怪物，会躲开尸兄；被玩家用木棍敲或右键触到「开关」后，
 * 会依次播放 open → flee → drop 三段动画，在 drop 阶段从体内钻出一只尸兄虫，
 * 并让附近的 CoCo 企鹅盯上这只虫子。
 */
public class UncleEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 动画 ====================
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    protected static final RawAnimation FLEE_ANIM = RawAnimation.begin().thenPlay("flee");
    protected static final RawAnimation OPEN_ANIM = RawAnimation.begin().thenPlay("open");
    protected static final RawAnimation DROP_ANIM = RawAnimation.begin().thenPlay("drop");

    // ==================== 同步数据 ====================
    private static final EntityDataAccessor<Integer> DATA_ANIMATION_STATE =
            SynchedEntityData.defineId(UncleEntity.class, EntityDataSerializers.INT);

    /** 三段动画各段时长（tick），按模型动画长度写死 */
    private static final int OPEN_ANIM_DURATION = 58;
    private static final int FLEE_ANIM_DURATION = 20;
    private static final int DROP_ANIM_DURATION = 40;

    private int dialogueCooldown = 0;
    private int animationTimer = 0;
    private boolean isWormExposed = false;
    private boolean hasDroppedWorm = false;

    private static final String[] RANDOM_DIALOGUES = {
            "少女的内心，你懂吗？",
            "我的漫画可是一等奖！",
            "Coco别乱跑...",
            "这个月的稿费...",
            "平果4S就是我的命！",
            "为了艺术献身！",
            "你看过《鑫瓶梅》吗？"
    };

    public UncleEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANIMATION_STATE, 0);
    }

    public int getAnimationState() {
        return this.entityData.get(DATA_ANIMATION_STATE);
    }

    public void setAnimationState(int state) {
        this.entityData.set(DATA_ANIMATION_STATE, state);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));

        // 怕尸兄、怕怪物；原著里的龙右实体目标项目还没有，故只保留这两条躲避
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LowerLevelZbEntity.class, 10.0F, 1.2D, 1.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 1.2D, 1.5D));

        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // 只打「怪物」，且把已经躲着的尸兄排除掉，免得一边躲一边打
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Monster>(this, Monster.class,
                0, true, false,
                (target, level) -> target instanceof Monster && !(target instanceof LowerLevelZbEntity)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ATTACK_SPEED, 1.0D);
    }

    // ==================== 动画控制器 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 5, this::controlAnimation));
    }

    private PlayState controlAnimation(AnimationTest<UncleEntity> test) {
        int state = getAnimationState();
        if (state == 1) {
            return test.setAndContinue(OPEN_ANIM);
        }
        if (state == 2) {
            return test.setAndContinue(FLEE_ANIM);
        }
        if (state == 3) {
            return test.setAndContinue(DROP_ANIM);
        }

        if (this.getAttackAnim(test.renderState().getPartialTick()) > 0) {
            return test.setAndContinue(ATTACK_ANIM);
        }
        if (test.isMoving()) {
            return test.setAndContinue(WALK_ANIM);
        }
        return test.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== 触发吐虫 ====================

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!this.hasDroppedWorm && source.getDirectEntity() instanceof Player player
                && player.getMainHandItem().is(Items.STICK)) {
            triggerOpenAnimation();
        }
        return super.hurtServer(level, source, amount);
    }

    public void triggerOpenAnimation() {
        if (this.level().isClientSide()) return;
        if (getAnimationState() != 0) return;

        setAnimationState(1);
        this.animationTimer = OPEN_ANIM_DURATION;
        this.isWormExposed = true;

        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0D))) {
            player.sendOverlayMessage(Component.literal("§e大叔：§f别...别看！"));
        }
    }

    private void spawnZbWorm() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        this.hasDroppedWorm = true;

        ZbWormEntity worm = new ZbWormEntity(ModEntities.ZB_WORM, serverLevel);
        worm.snapTo(this.getX(), this.getY() + 0.5, this.getZ());
        serverLevel.addFreshEntity(worm);

        for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0D))) {
            player.sendOverlayMessage(Component.literal("§e大叔：§f一只尸兄虫子从体内钻出！"));
        }

        // 让附近的企鹅直接盯上刚钻出来的虫子，接上「企鹅吃虫变尸兄」那条线
        for (CocoPenguinEntity penguin : serverLevel.getEntitiesOfClass(CocoPenguinEntity.class,
                this.getBoundingBox().inflate(20.0D))) {
            penguin.setTarget(worm);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;

        if (animationTimer > 0) {
            animationTimer--;
            if (animationTimer <= 0) {
                if (getAnimationState() == 1) {
                    setAnimationState(2);
                    animationTimer = FLEE_ANIM_DURATION;
                } else if (getAnimationState() == 2) {
                    setAnimationState(3);
                    animationTimer = DROP_ANIM_DURATION;
                    spawnZbWorm();
                } else if (getAnimationState() == 3) {
                    setAnimationState(0);
                }
            }
        }

        if (dialogueCooldown > 0) {
            dialogueCooldown--;
        } else if (this.tickCount % 200 == 0 && this.random.nextFloat() < 0.1F) {
            sayRandomDialogue();
        }
    }

    private void sayRandomDialogue() {
        if (RANDOM_DIALOGUES.length == 0) return;

        String dialogue = RANDOM_DIALOGUES[this.random.nextInt(RANDOM_DIALOGUES.length)];
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0D))) {
            player.sendOverlayMessage(Component.literal("§e大叔：§f" + dialogue));
        }
        dialogueCooldown = 400;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (itemStack.is(Items.STICK) && !hasDroppedWorm) {
            if (!this.level().isClientSide()) {
                triggerOpenAnimation();
            }
            return InteractionResult.SUCCESS;
        }

        if (!this.level().isClientSide()) {
            String greeting = RANDOM_DIALOGUES[this.random.nextInt(RANDOM_DIALOGUES.length)];
            player.sendOverlayMessage(Component.literal("§e大叔：§f" + greeting));
        }
        return InteractionResult.SUCCESS;
    }

    public boolean isWormExposed() {
        return this.isWormExposed;
    }

    public boolean hasDroppedWorm() {
        return this.hasDroppedWorm;
    }

    // ==================== 持久化 ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("WormExposed", this.isWormExposed);
        output.putBoolean("HasDroppedWorm", this.hasDroppedWorm);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.isWormExposed = input.getBooleanOr("WormExposed", false);
        this.hasDroppedWorm = input.getBooleanOr("HasDroppedWorm", false);
    }
}
