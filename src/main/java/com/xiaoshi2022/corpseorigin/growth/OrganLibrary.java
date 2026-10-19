package com.xiaoshi2022.corpseorigin.growth;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

/**
 * 器官配置简单序列化工具（1.20.1 Forge 简化版）。
 * 只提供 parseSlots / toJson 两个核心方法，不依赖完整的器官定义数据库。
 * 后续扩展器官系统时可替换为完整实现。
 */
public final class OrganLibrary {
    private static final Gson GSON = new Gson();
    private static final Type SLOT_LIST = new TypeToken<List<OrganSlot>>() {}.getType();

    private OrganLibrary() {}

    public static List<OrganSlot> parseSlots(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<OrganSlot> result = GSON.fromJson(json, SLOT_LIST);
            return result == null ? List.of() : result;
        } catch (Exception e) {
            return List.of();
        }
    }

    public static String toJson(List<OrganSlot> slots) {
        if (slots == null) return "[]";
        try { return GSON.toJson(slots, SLOT_LIST); }
        catch (Exception e) { return "[]"; }
    }
}
