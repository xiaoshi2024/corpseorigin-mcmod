package xiaoshi2022.corpseorigin.registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 客户端按键绑定
 */
public final class CorpseKeyBindings {

    private CorpseKeyBindings() {
    }

    /** 尸兄模组按键分类 */
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "main"));

    /** 打开技能轮盘 */
    public static KeyMapping openSkillWheel;
    /** 打开技能进化树 */
    public static KeyMapping openSkillTree;
    /** 切换 HUD 显示 */
    public static KeyMapping toggleHud;

    public static void register() {
        openSkillWheel = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.corpseorigin.open_skill_wheel",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY));
        openSkillTree = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.corpseorigin.open_skill_tree",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                CATEGORY));
        toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.corpseorigin.toggle_hud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                CATEGORY));
    }
}
