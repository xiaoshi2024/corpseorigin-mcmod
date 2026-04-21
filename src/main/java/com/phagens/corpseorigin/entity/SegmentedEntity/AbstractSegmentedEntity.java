package com.phagens.corpseorigin.entity.SegmentedEntity;
//1. 架构分层设计
//基类：AbstractSegmentedEntity (抽象多节段生物)
//职责：定义所有多节段生物的“骨架”。
//核心功能：维护身体节段列表 (List<UUID>)、处理总血量与局部血量的联动、定义链式跟随的通用算法。
//头部基类：AbstractSegmentedHead
//继承自：Monster 或 PathfinderMob。
//职责：作为“主控端”，负责生成身体节段、同步位置路径、处理 AI 目标。
//关节基类：AbstractSegmentedJoint
//继承自：Entity。
//职责：作为“从属端”，只负责根据索引找到上一节并跟随，以及处理自身的断裂逻辑。
public class AbstractSegmentedEntity {
}
