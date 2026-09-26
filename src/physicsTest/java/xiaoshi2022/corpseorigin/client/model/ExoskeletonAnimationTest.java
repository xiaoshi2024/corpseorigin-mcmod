package xiaoshi2022.corpseorigin.client.model;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Reuse one real model exactly as multiplayer rendering does. */
public final class ExoskeletonAnimationTest {
    public static void main(String[] args) {
        var model = new ExoskeletonModel(ExoskeletonModel.createBodyLayer().bakeRoot());
        var idle = state(1, 0);
        var attacking = state(2, .5f);
        model.setupAnim(idle);
        float idleAngle = model.getGroup3().xRot;
        model.setupAnim(attacking);
        float attackAngle = model.getGroup3().xRot;
        if (Math.abs(attackAngle - idleAngle) < .1f)
            throw new AssertionError("Attacker must animate");
        for (int frame = 0; frame < 120; frame++) {
            model.setupAnim(idle);
            equal(model.getGroup3().xRot, idleAngle, "Another player's swing leaked into idle player");
            model.setupAnim(attacking);
            equal(model.getGroup3().xRot, attackAngle, "Render frequency changed swing progress");
        }
        attacking.attackTime = 0;
        model.setupAnim(attacking);
        equal(model.getGroup3().xRot, idleAngle, "Finished swing left stale pose");
        idle.attackTime = .5f;
        model.setupAnim(idle);
        equal(model.getGroup3().xRot, attackAngle, "First player cannot animate independently");
        model.setupAnim(attacking);
        equal(model.getGroup3().xRot, idleAngle, "First player's swing leaked into second player");
        System.out.println("Exoskeleton animation: shared-model isolation and frame independence passed.");
    }

    private static AvatarRenderState state(int id, float attackTime) {
        var state = new AvatarRenderState();
        state.id = id;
        state.ageInTicks = 100;
        state.attackTime = attackTime;
        return state;
    }

    private static void equal(float actual, float expected, String message) {
        if (Math.abs(actual - expected) > .00001f) throw new AssertionError(message);
    }
}
