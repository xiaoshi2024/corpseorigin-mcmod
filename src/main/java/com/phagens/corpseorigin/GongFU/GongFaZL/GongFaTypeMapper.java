package com.phagens.corpseorigin.GongFU.GongFaZL;

import com.phagens.corpseorigin.CorpseOrigin;

import java.util.HashMap;
import java.util.Map;

public class GongFaTypeMapper {
    
    private static final Map<String, GongFaCategory> TYPE_TO_CATEGORY = new HashMap<>();
    
    static {
        initializeDefaultMappings();
    }
    
    private static void initializeDefaultMappings() {
        TYPE_TO_CATEGORY.put("GF", GongFaCategory.GF);
        TYPE_TO_CATEGORY.put("XM", GongFaCategory.XM);
        TYPE_TO_CATEGORY.put("YN", GongFaCategory.YN);
        TYPE_TO_CATEGORY.put("fb", GongFaCategory.FB);
        TYPE_TO_CATEGORY.put("st", GongFaCategory.ST);
        TYPE_TO_CATEGORY.put("sg", GongFaCategory.SG);
        TYPE_TO_CATEGORY.put("sz", GongFaCategory.SZ);
        TYPE_TO_CATEGORY.put("tfst", GongFaCategory.TFST);
        TYPE_TO_CATEGORY.put("xs", GongFaCategory.QY);
    }
    
    public static GongFaCategory getCategoryForType(String type) {
        if (type == null || type.isEmpty()) {
            return GongFaCategory.UNIVERSAL;
        }
        
        String upperType = type.toUpperCase();
        
        GongFaCategory category = TYPE_TO_CATEGORY.get(upperType);
        if (category != null) {
            return category;
        }
        
        for (Map.Entry<String, GongFaCategory> entry : TYPE_TO_CATEGORY.entrySet()) {
            if (upperType.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return GongFaCategory.UNIVERSAL;
    }
    
    public static void registerMapping(String type, GongFaCategory category) {
        TYPE_TO_CATEGORY.put(type.toUpperCase(), category);
        CorpseOrigin.LOGGER.info("注册功法类型映射: {} -> {}", type, category.getName());
    }
}
