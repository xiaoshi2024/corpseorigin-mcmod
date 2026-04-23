package com.phagens.corpseorigin.compat.curios;

import com.phagens.corpseorigin.CorpseOrigin;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Curios 集成管理器 - 使用反射机制实现软依赖
 * 
 * 【软依赖设计】
 * 1. 使用反射机制动态检测 Curios 模组
 * 2. Curios 存在时启用饰品功能
 * 3. Curios 不存在时模组继续正常运行
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class CuriosIntegration {
    
    private static boolean curiosAvailable = false;
    
    /**
     * 通用端初始化
     */
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                // 检查 Curios 类是否存在
                Class.forName("top.theillusivec4.curios.api.SlotContext");
                Class.forName("top.theillusivec4.curios.api.type.capability.ICurioItem");
                
                curiosAvailable = true;
                CorpseOrigin.LOGGER.info("Curios 模组检测到，启用饰品集成");
                
            } catch (ClassNotFoundException e) {
                curiosAvailable = false;
                CorpseOrigin.LOGGER.info("Curios 模组未检测到，跳过饰品集成");
            }
        });
    }
    
    /**
     * 客户端初始化
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (curiosAvailable) {
                try {
                    // 注册 Curios 渲染器
                    registerCuriosRenderers();
                    CorpseOrigin.LOGGER.info("Curios 渲染器注册成功");
                } catch (Exception e) {
                    CorpseOrigin.LOGGER.warn("Curios 渲染器注册失败: {}", e.getMessage());
                }
            }
        });
    }
    
    /**
     * 注册 Curios 渲染器
     */
    private static void registerCuriosRenderers() throws Exception {
        // 获取 Curios 渲染器注册表
        Class<?> curiosRendererRegistryClass = Class.forName("top.theillusivec4.curios.api.client.CuriosRendererRegistry");
        
        // 创建 JuQue Curios 渲染器
        JuQueCuriosRenderer juqueCuriosRenderer = new JuQueCuriosRenderer();
        
        // 创建动态代理来实现 ICurioRenderer 接口
        Class<?> iCurioRendererClass = Class.forName("top.theillusivec4.curios.api.client.ICurioRenderer");
        Object proxyRenderer = java.lang.reflect.Proxy.newProxyInstance(
            CuriosIntegration.class.getClassLoader(),
            new Class[]{iCurioRendererClass},
            (proxy, method, args) -> {
                if ("render".equals(method.getName())) {
                    juqueCuriosRenderer.render(
                        (net.minecraft.world.item.ItemStack) args[0],
                        args[1],
                        args[2],
                        args[3],
                        args[4],
                        (int) args[5],
                        (float) args[6],
                        (float) args[7],
                        (float) args[8],
                        (float) args[9],
                        (float) args[10],
                        (float) args[11]
                    );
                }
                return null;
            }
        );
        
        // 注册渲染器
        // 获取 JuQue 物品实例
        try {
            Class<?> modItemsClass = Class.forName("com.phagens.corpseorigin.register.Moditems");
            
            // 获取 MING_JUQUE 物品
            Object mingJuque = modItemsClass.getField("MING_JUQUE").get(null);
            Object juqueItem = mingJuque.getClass().getMethod("get").invoke(mingJuque);
            
            // 创建 Supplier 对象
            java.util.function.Supplier<?> supplier = new java.util.function.Supplier<Object>() {
                @Override
                public Object get() {
                    return proxyRenderer;
                }
            };
            
            // 调用注册方法
            curiosRendererRegistryClass.getMethod("register", 
                net.minecraft.world.item.Item.class, 
                java.util.function.Supplier.class).invoke(null, 
                    juqueItem, supplier);
                    
            // 也注册 MING_JUQUE_TW
            try {
                Object mingJuqueTw = modItemsClass.getField("MING_JUQUE_TW").get(null);
                Object juqueItemTw = mingJuqueTw.getClass().getMethod("get").invoke(mingJuqueTw);
                
                curiosRendererRegistryClass.getMethod("register", 
                    net.minecraft.world.item.Item.class, 
                    java.util.function.Supplier.class).invoke(null, 
                        juqueItemTw, supplier);
            } catch (Exception e) {
                CorpseOrigin.LOGGER.warn("MING_JUQUE_TW 注册失败: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("JuQue 物品注册失败: {}", e.getMessage());
        }
    }
    
    /**
     * 检查 Curios 是否可用
     */
    public static boolean isCuriosAvailable() {
        return curiosAvailable;
    }
    
    /**
     * 安全地执行 Curios 相关操作
     */
    public static void safeExecute(Runnable action) {
        if (curiosAvailable) {
            try {
                action.run();
            } catch (Exception e) {
                CorpseOrigin.LOGGER.warn("Curios 操作执行失败: {}", e.getMessage());
            }
        }
    }
}