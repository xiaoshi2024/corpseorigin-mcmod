package com.phagens.corpseorigin.GongFU.JSskill;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jline.utils.InputStreamReader;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaScript 技能引擎 - 执行 JS 脚本实现功法技能效果
 */
public class JSSkillEngine {
    private static JSSkillEngine INSTANCE;
    // 缓存已加载的脚本
    private final Map<String, CompiledScript> scriptCache = new ConcurrentHashMap<>();

    // Nashorn JS 引擎工厂
    private final ScriptEngineFactory engineFactory;
    private final ScriptEngine engine;
    private JSSkillEngine() {
        // 创建 Nashorn 引擎（启用 Java 访问）
        this.engineFactory = new NashornScriptEngineFactory();
        this.engine = engineFactory.getScriptEngine(
        );

        // 绑定常用类到 JS 上下文
        bindJavaClasses();
        loadUtilityFunctions();
    }

    public static JSSkillEngine getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JSSkillEngine();
        }
        return INSTANCE;
    }

    private void loadUtilityFunctions() {
        try {
            String utilsPath = "/assets/corpseorigin/scripts/gongfu/gongfu_utils.js";
            var resource = getClass().getResourceAsStream(utilsPath);

            if (resource != null) {
                try (InputStreamReader reader = new InputStreamReader(resource)) {
                    engine.eval(reader);
                    CorpseOrigin.LOGGER.info("成功加载工具函数库");
                }
            } else {
                CorpseOrigin.LOGGER.warn("未找到工具函数文件：{}", utilsPath);
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("加载工具函数失败", e);
        }
    }

    /**
     * 绑定 Java 类到 JS 环境
     */
    private void bindJavaClasses() {
        try {
            engine.put("SkillEffects", com.phagens.corpseorigin.GongFU.JSskill.SkillEffects.class);
            engine.put("ProjectileManager", com.phagens.corpseorigin.GongFU.JSskill.Factory.ProjectileManager.getInstance());
            CorpseOrigin.LOGGER.debug("JS 引擎初始化完成");
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("JS 引擎绑定 Java 类失败", e);
        }
    }

    /**
     * 执行功法技能脚本（仅玩家 - 向后兼容）
     */
    public boolean executeSkill(String skillName, ServerPlayer player, GongFaData gongFaData) {
        return executeSkillForEntity(skillName, player, gongFaData);
    }



    /**
     * 执行功法技能脚本
     *
     * @param skillName 技能名称（如"shi_xian_jian"）
     * @param player 施法玩家
     * @param gongFaData 功法数据
     * @return 是否成功执行
     */
    /**
     * 执行功法技能脚本（支持任意实体）
     */
    public boolean executeSkillForEntity(String skillName, LivingEntity entity, GongFaData gongFaData) {
        try {
            String scriptPath = "/assets/corpseorigin/scripts/gongfu/" +
                    skillName.toLowerCase().replace(" ", "_") + ".js";

            CompiledScript script = scriptCache.get(scriptPath);
            if (script == null) {
                script = loadAndCompileScript(scriptPath);
                if (script == null) {
                    return false;
                }
                scriptCache.put(scriptPath, script);
            }
            ScriptContext context = engine.getContext();
            var lookAngle = entity.getLookAngle();

            context.setAttribute("entity", entity, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("player", entity instanceof ServerPlayer ? (ServerPlayer) entity : null, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("world", entity.level(), ScriptContext.ENGINE_SCOPE);
            context.setAttribute("data", gongFaData, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("skillName", skillName, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("SkillEffects", com.phagens.corpseorigin.GongFU.JSskill.SkillEffects.class, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("ProjectileManager", com.phagens.corpseorigin.GongFU.JSskill.Factory.ProjectileManager.getInstance(), ScriptContext.ENGINE_SCOPE);
            context.setAttribute("lookX", lookAngle.x, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("lookY", lookAngle.y, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("lookZ", lookAngle.z, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("playerX", entity.getX(), ScriptContext.ENGINE_SCOPE);
            context.setAttribute("playerY", entity.getY(), ScriptContext.ENGINE_SCOPE);
            context.setAttribute("playerZ", entity.getZ(), ScriptContext.ENGINE_SCOPE);
            context.setAttribute("eyeY", entity.getEyeY(), ScriptContext.ENGINE_SCOPE);

            Object scriptObj = script.eval(context);

            if (scriptObj instanceof org.openjdk.nashorn.api.scripting.ScriptObjectMirror) {
                org.openjdk.nashorn.api.scripting.ScriptObjectMirror scriptMirror =
                        (org.openjdk.nashorn.api.scripting.ScriptObjectMirror) scriptObj;

                if (scriptMirror.hasMember("activate")) {
                    var activateFunc = scriptMirror.getMember("activate");
                    if (activateFunc instanceof org.openjdk.nashorn.api.scripting.ScriptObjectMirror) {
                        try {
                            Object result = ((org.openjdk.nashorn.api.scripting.ScriptObjectMirror) activateFunc)
                                    .call(scriptMirror, entity, entity.level(), gongFaData);
                            CorpseOrigin.LOGGER.info("【JS 技能】{} 执行结果：{}", skillName, result);
                            return result instanceof Boolean ? (Boolean) result : true;
                        } catch (org.openjdk.nashorn.internal.runtime.ECMAException e) {
                            CorpseOrigin.LOGGER.error("JS 脚本执行异常：{}", skillName, e);
                            return false;
                        }
                    } else {
                        CorpseOrigin.LOGGER.error("activate 成员不是函数：{}", skillName);
                        return false;
                    }
                } else {
                    CorpseOrigin.LOGGER.error("JS 脚本中没有 activate 函数：{}", skillName);
                    return false;
                }
            } else {
                CorpseOrigin.LOGGER.error("脚本执行后未返回对象：{}", skillName);
                return false;
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("执行技能脚本失败：{}", skillName, e);
            return false;
        }
    }

    /**
     * 加载并编译 JS 脚本
     */
    private CompiledScript loadAndCompileScript(String path) {
        try {
            var resource = getClass().getResourceAsStream(path);
            if (resource == null) {
                CorpseOrigin.LOGGER.warn("脚本文件不存在：{}", path);
                return null;
            }

            try (InputStreamReader reader = new InputStreamReader(resource)) {
                CompiledScript compiled = ((Compilable) engine).compile(reader);
                CorpseOrigin.LOGGER.info("成功编译脚本：{}", path);
                return compiled;
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("加载脚本失败：{}", path, e);
            return null;
        }
    }

    /**
     * 清除脚本缓存（用于热重载）
     */
    public void clearCache() {
        scriptCache.clear();
        CorpseOrigin.LOGGER.info("已清除所有 JS 脚本缓存");
    }

    /**
     * 重新加载指定脚本
     */
    public void reloadScript(String skillName) {
        String scriptPath = "/assets/corpseorigin/scripts/gongfu/" +
                skillName.toLowerCase().replace(" ", "_") + ".js";
        scriptCache.remove(scriptPath);
        loadAndCompileScript(scriptPath);
    }



}
