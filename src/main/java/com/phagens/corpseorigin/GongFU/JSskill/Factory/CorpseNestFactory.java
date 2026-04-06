package com.phagens.corpseorigin.GongFU.JSskill.Factory;



import com.phagens.corpseorigin.block.entity.ZBRFleshBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 尸巢结构工厂
 * 
 * 【功能说明】
 * 提供各种尸巢结构的生成功能，所有几何运算由Java完成
 * 支持半圆、圆形、环形等多种结构
 * 
 * 【使用方式】
 * 在JS脚本中通过 Java.type() 调用此类的方法
 * 
 * @author Phagens
 * @version 1.0
 */
public class CorpseNestFactory {

    /**
     * 生成半圆形尸巢结构
     * 
     * @param level 服务器世界
     * @param center 中心位置（玩家脚下）
     * @param radius 半径（格数）
     * @param ownerUUID 主人UUID
     * @param initialKills 初始击杀数
     */
    public static void generateSemiCircle(ServerLevel level, BlockPos center, int radius, UUID ownerUUID, int initialKills) {
        var fleshBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
            net.minecraft.resources.ResourceLocation.tryParse("corpseorigin:zbr_flesh")
        );
        
        if (fleshBlock == null) {
            return;
        }
        
        BlockState fleshState = fleshBlock.defaultBlockState();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                
                if (distance <= radius && z >= 0) {
                    int heightOffset = (int) Math.floor((radius - distance) / 3.0);
                    
                    for (int y = 0; y <= heightOffset; y++) {
                        BlockPos pos = center.offset(x, y, z);
                        
                        if (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced()) {
                            level.setBlock(pos, fleshState, 3);
                            setBlockEntityData(level, pos, ownerUUID, initialKills);
                        }
                    }
                }
            }
        }
    }

    /**
     * 生成完整圆形尸巢结构
     * 
     * @param level 服务器世界
     * @param center 中心位置
     * @param radius 半径
     * @param ownerUUID 主人UUID
     * @param initialKills 初始击杀数
     */
    public static void generateFullCircle(ServerLevel level, BlockPos center, int radius, UUID ownerUUID, int initialKills) {
        var fleshBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
            net.minecraft.resources.ResourceLocation.tryParse("corpseorigin:zbr_flesh")
        );
        
        if (fleshBlock == null) {
            return;
        }
        
        BlockState fleshState = fleshBlock.defaultBlockState();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                
                if (distance <= radius) {
                    int heightOffset = (int) Math.floor((radius - distance) / 3.0);
                    
                    for (int y = 0; y <= heightOffset; y++) {
                        BlockPos pos = center.offset(x, y, z);
                        
                        if (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced()) {
                            level.setBlock(pos, fleshState, 3);
                            setBlockEntityData(level, pos, ownerUUID, initialKills);
                        }
                    }
                }
            }
        }
    }

    /**
     * 生成环形尸巢结构
     * 
     * @param level 服务器世界
     * @param center 中心位置
     * @param innerRadius 内半径
     * @param outerRadius 外半径
     * @param ownerUUID 主人UUID
     * @param initialKills 初始击杀数
     */
    public static void generateRing(ServerLevel level, BlockPos center, int innerRadius, int outerRadius, UUID ownerUUID, int initialKills) {
        var fleshBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
            net.minecraft.resources.ResourceLocation.tryParse("corpseorigin:zbr_flesh")
        );
        
        if (fleshBlock == null) {
            return;
        }
        
        BlockState fleshState = fleshBlock.defaultBlockState();
        
        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int z = -outerRadius; z <= outerRadius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                
                if (distance >= innerRadius && distance <= outerRadius) {
                    BlockPos pos = center.offset(x, 0, z);
                    
                    if (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced()) {
                        level.setBlock(pos, fleshState, 3);
                        setBlockEntityData(level, pos, ownerUUID, initialKills);
                    }
                }
            }
        }
    }

    /**
     * 生成扇形尸巢结构
     * 
     * @param level 服务器世界
     * @param center 中心位置
     * @param radius 半径
     * @param startAngle 起始角度（度）
     * @param endAngle 结束角度（度）
     * @param ownerUUID 主人UUID
     * @param initialKills 初始击杀数
     */
    public static void generateSector(ServerLevel level, BlockPos center, int radius, 
                                     double startAngle, double endAngle, UUID ownerUUID, int initialKills) {
        var fleshBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
            net.minecraft.resources.ResourceLocation.tryParse("corpseorigin:zbr_flesh")
        );
        
        if (fleshBlock == null) {
            return;
        }
        
        BlockState fleshState = fleshBlock.defaultBlockState();
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(endAngle);
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                double angle = Math.atan2(z, x);
                
                if (angle < 0) angle += Math.PI * 2;
                
                if (distance <= radius && angle >= startRad && angle <= endRad) {
                    int heightOffset = (int) Math.floor((radius - distance) / 3.0);
                    
                    for (int y = 0; y <= heightOffset; y++) {
                        BlockPos pos = center.offset(x, y, z);
                        
                        if (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced()) {
                            level.setBlock(pos, fleshState, 3);
                            setBlockEntityData(level, pos, ownerUUID, initialKills);
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置方块实体数据
     */
    private static void setBlockEntityData(ServerLevel level, BlockPos pos, UUID ownerUUID, int kills) {
        try {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ZBRFleshBlockEntity fleshEntity) {
                fleshEntity.setOwner(ownerUUID);
                fleshEntity.setKills(kills);
            }
        } catch (Exception e) {
        }
    }
}
