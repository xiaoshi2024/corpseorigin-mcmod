/**
 * 根据层级获取倍率
 * @param {string} ceng - 层级标识 (copy_1 ~ copy_9)
 * @return {number} 倍率值
 */
function getCengMultiplier(ceng) {
    switch (ceng) {
        case "copy_1": return 1.0;   // 一重天：100%
        case "copy_2": return 1.2;   // 二重天：120%
        case "copy_3": return 1.4;   // 三重天：140%
        case "copy_4": return 1.6;   // 四重天：160%
        case "copy_5": return 1.8;   // 五重天：180%
        case "copy_6": return 2.0;   // 六重天：200%
        case "copy_7": return 2.2;   // 七重天：220%
        case "copy_8": return 2.5;   // 八重天：250%
        case "copy_9": return 3.0;   // 九重天：300%
        default: return 1.0;         // 默认 100%
    }
}

/**
 * 计算最终伤害
 * @param {number} baseDamage - 基础伤害
 * @param {string} ceng - 层级标识
 * @return {number} 最终伤害
 */
function calculateFinalDamage(baseDamage, ceng) {
    return baseDamage * getCengMultiplier(ceng);
}

/**
 * 计算最终效果持续时间
 * @param {number} baseDuration - 基础持续时间（tick）
 * @param {string} ceng - 层级标识
 * @return {number} 最终持续时间（tick）
 */
function calculateFinalDuration(baseDuration, ceng) {
    return Math.floor(baseDuration * getCengMultiplier(ceng));
}

/**
 * 计算最终攻击范围
 * @param {number} baseRange - 基础范围
 * @param {string} ceng - 层级标识
 * @return {number} 最终范围
 */
function calculateFinalRange(baseRange, ceng) {
    return baseRange * getCengMultiplier(ceng);
}

/**
 * 获取层级数字
 * @param {string} ceng - 层级标识
 * @return {number} 层级数字 (1-9)
 */
function getCengNumber(ceng) {
    switch (ceng) {
        case "copy_1": return 1;
        case "copy_2": return 2;
        case "copy_3": return 3;
        case "copy_4": return 4;
        case "copy_5": return 5;
        case "copy_6": return 6;
        case "copy_7": return 7;
        case "copy_8": return 8;
        case "copy_9": return 9;
        default: return 1;
    }
}

/**
 * 调试日志输出
 * @param {string} message - 日志消息
 */
function gongfuLog(message) {
    print("[GongFu Skill] " + message);
}
