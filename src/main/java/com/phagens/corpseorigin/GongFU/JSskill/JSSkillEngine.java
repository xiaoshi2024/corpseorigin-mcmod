package com.phagens.corpseorigin.GongFU.JSskill;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import net.minecraft.server.level.ServerPlayer;
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
    }

    public static JSSkillEngine getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JSSkillEngine();
        }
        return INSTANCE;
    }

    /**
     * 绑定 Java 类到 JS 环境
     */
    private void bindJavaClasses() {
        try {
            engine.put("SkillEffects", com.phagens.corpseorigin.GongFU.JSskill.SkillEffects.class);
            engine.put("ParticlePattern", com.phagens.corpseorigin.GongFU.JSskill.SkillEffects.class);
            CorpseOrigin.LOGGER.debug("JS 引擎初始化完成");
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("JS 引擎绑定 Java 类失败", e);
        }
    }

    /**
     * 执行功法技能脚本
     *
     * @param skillName 技能名称（如"shi_xian_jian"）
     * @param player 施法玩家
     * @param gongFaData 功法数据
     * @return 是否成功执行
     */
    public boolean executeSkill(String skillName, ServerPlayer player, GongFaData gongFaData) {
        try {
            // 获取脚本路径
            String scriptPath = "/assets/corpseorigin/scripts/gongfu/" +
                    skillName.toLowerCase().replace(" ", "_") + ".js";

            // 检查缓存
            CompiledScript script = scriptCache.get(scriptPath);

            // 如果没缓存，加载并编译
            if (script == null) {
                script = loadAndCompileScript(scriptPath);
                if (script == null) {
                    CorpseOrigin.LOGGER.error("找不到技能脚本：{}", skillName);
                    return false;
                }
                scriptCache.put(scriptPath, script);
            }

            ScriptContext context = engine.getContext();
            context.setAttribute("player", player, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("world", player.serverLevel(), ScriptContext.ENGINE_SCOPE);
            context.setAttribute("data", gongFaData, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("skillName", skillName, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("SkillEffects", com.phagens.corpseorigin.GongFU.JSskill.SkillEffects.class, ScriptContext.ENGINE_SCOPE);
            context.setAttribute("ParticlePattern", com.phagens.corpseorigin.GongFU.JSskill.SkillEffects.class, ScriptContext.ENGINE_SCOPE);

            Object scriptObj = script.eval(context);
            // 脚本应该返回一个包含 activate 方法的对象
            if (scriptObj instanceof org.openjdk.nashorn.api.scripting.ScriptObjectMirror) {
                org.openjdk.nashorn.api.scripting.ScriptObjectMirror scriptMirror =
                        (org.openjdk.nashorn.api.scripting.ScriptObjectMirror) scriptObj;

                if (scriptMirror.hasMember("activate")) {
                    // 从对象中获取 activate 函数成员，然后调用
                    var activateFunc = scriptMirror.getMember("activate");
                    if (activateFunc instanceof org.openjdk.nashorn.api.scripting.ScriptObjectMirror) {
                        try {
                            Object result = ((org.openjdk.nashorn.api.scripting.ScriptObjectMirror) activateFunc)
                                    .call(scriptMirror, player, player.serverLevel(), gongFaData);
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
