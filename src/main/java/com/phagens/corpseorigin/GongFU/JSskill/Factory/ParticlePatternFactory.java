package com.phagens.corpseorigin.GongFU.JSskill.Factory;


import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
//粒子工厂
public class ParticlePatternFactory {
    
    /**
     * 生成龙卷风粒子效果
     * @param level 世界
     * @param x 中心 X 坐标
     * @param y 底部 Y 坐标
     * @param z 中心 Z 坐标
     * @param height 高度
     * @param radius 底部半径
     * @param particle 粒子类型
     * @param count 粒子数量
     * @param speed 上升速度
     */
    public static void spawnTornado(Level level, double x, double y, double z, 
                                    double height, double radius, 
                                    ParticleOptions particle, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double progress = (double) i / count;
            double currentY = y + progress * height;
            double currentRadius = radius * (1.0 - progress * 0.5);
            double angle = progress * Math.PI * 2 * 3;
            
            double posX = x + Math.cos(angle) * currentRadius;
            double posZ = z + Math.sin(angle) * currentRadius;
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, currentY, posZ, 1, 0, 0, 0, speed);
            }
        }
    }
    
    /**
     * 生成椭圆形粒子环
     * @param level 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radiusX X 轴半径
     * @param radiusZ Z 轴半径
     * @param particle 粒子类型
     * @param count 粒子数量
     * @param yOffset Y 轴偏移（用于多层）
     */
    public static void spawnEllipse(Level level, double x, double y, double z,
                                    double radiusX, double radiusZ,
                                    ParticleOptions particle, int count, double yOffset) {
        for (int i = 0; i < count; i++) {
            double angle = (double) i / count * Math.PI * 2;
            double posX = x + Math.cos(angle) * radiusX;
            double posZ = z + Math.sin(angle) * radiusZ;
            double posY = y + yOffset;
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, posY, posZ, 1, 0, 0, 0, 0);
            }
        }
    }
    
    /**
     * 生成圆弧形粒子效果
     * @param level 世界
     * @param x 圆心 X 坐标
     * @param y 圆心 Y 坐标
     * @param z 圆心 Z 坐标
     * @param radius 半径
     * @param startAngle 起始角度（弧度）
     * @param endAngle 结束角度（弧度）
     * @param particle 粒子类型
     * @param count 粒子数量
     * @param yOffset Y 轴偏移
     */
    public static void spawnArc(Level level, double x, double y, double z,
                               double radius, double startAngle, double endAngle,
                               ParticleOptions particle, int count, double yOffset) {
        for (int i = 0; i < count; i++) {
            double progress = (double) i / count;
            double angle = startAngle + progress * (endAngle - startAngle);
            double posX = x + Math.cos(angle) * radius;
            double posZ = z + Math.sin(angle) * radius;
            double posY = y + yOffset;
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, posY, posZ, 1, 0, 0, 0, 0);
            }
        }
    }
    
    /**
     * 生成奥运五环图案
     * @param level 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param ringRadius 每个环的半径
     * @param particle 粒子类型
     * @param particlesPerRing 每个环的粒子数
     */
    public static void spawnOlympicRings(Level level, double x, double y, double z,
                                        double ringRadius, ParticleOptions particle, int particlesPerRing) {
        // 五环位置：上排 3 个，下排 2 个
        double[][] positions = {
            {-ringRadius * 1.2, 0},  // 左上
            {0, ringRadius * 0.3},   // 中上
            {ringRadius * 1.2, 0},   // 右上
            {-ringRadius * 0.6, -ringRadius * 0.8},  // 左下
            {ringRadius * 0.6, -ringRadius * 0.8}    // 右下
        };
        
        for (double[] pos : positions) {
            spawnCircle(level, x + pos[0], y, z + pos[1], ringRadius, particle, particlesPerRing, 0);
        }
    }
    
    /**
     * 生成圆形图案
     * @param level 世界
     * @param x 圆心 X 坐标
     * @param y 圆心 Y 坐标
     * @param z 圆心 Z 坐标
     * @param radius 半径
     * @param particle 粒子类型
     * @param count 粒子数量
     * @param yOffset Y 轴偏移
     */
    public static void spawnCircle(Level level, double x, double y, double z,
                                  double radius, ParticleOptions particle, int count, double yOffset) {
        for (int i = 0; i < count; i++) {
            double angle = (double) i / count * Math.PI * 2;
            double posX = x + Math.cos(angle) * radius;
            double posZ = z + Math.sin(angle) * radius;
            double posY = y + yOffset;
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, posY, posZ, 1, 0, 0, 0, 0);
            }
        }
    }
    
    /**
     * 生成地面图案（在地面上生成粒子）
     * @param level 世界
     * @param x 中心 X 坐标
     * @param y 地面 Y 坐标
     * @param z 中心 Z 坐标
     * @param pattern 图案类型 ("circle", "spiral", "star")
     * @param size 图案大小
     * @param particle 粒子类型
     * @param density 密度（粒子数量）
     */
    public static void spawnGroundPattern(Level level, double x, double y, double z,
                                         String pattern, double size, 
                                         ParticleOptions particle, int density) {
        switch (pattern.toLowerCase()) {
            case "circle" -> spawnGroundCircle(level, x, y, z, size, particle, density);
            case "spiral" -> spawnGroundSpiral(level, x, y, z, size, particle, density);
            case "star" -> spawnGroundStar(level, x, y, z, size, particle, density);
            default -> spawnGroundCircle(level, x, y, z, size, particle, density);
        }
    }
    
    private static void spawnGroundCircle(Level level, double x, double y, double z,
                                         double size, ParticleOptions particle, int density) {
        for (int i = 0; i < density; i++) {
            double angle = (double) i / density * Math.PI * 2;
            double r = size * Math.sqrt((double) i / density);
            double posX = x + Math.cos(angle) * r;
            double posZ = z + Math.sin(angle) * r;
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, y + 0.1, posZ, 1, 0, 0, 0, 0);
            }
        }
    }
    
    private static void spawnGroundSpiral(Level level, double x, double y, double z,
                                         double size, ParticleOptions particle, int density) {
        for (int i = 0; i < density; i++) {
            double angle = (double) i / density * Math.PI * 4;
            double r = size * ((double) i / density);
            double posX = x + Math.cos(angle) * r;
            double posZ = z + Math.sin(angle) * r;
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, y + 0.1, posZ, 1, 0, 0, 0, 0);
            }
        }
    }
    
    private static void spawnGroundStar(Level level, double x, double y, double z,
                                       double size, ParticleOptions particle, int density) {
        int points = 5;
        for (int i = 0; i < density; i++) {
            double progress = (double) i / density;
            double angle = progress * Math.PI * 2 * points;
            double r = size * (0.5 + 0.5 * Math.cos(progress * Math.PI * points));
            double posX = x + Math.cos(angle) * r;
            double posZ = z + Math.sin(angle) * r;
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, y + 0.1, posZ, 1, 0, 0, 0, 0);
            }
        }
    }
    
    /**
     * 生成球形粒子效果
     * @param level 世界
     * @param x 球心 X 坐标
     * @param y 球心 Y 坐标
     * @param z 球心 Z 坐标
     * @param radius 半径
     * @param particle 粒子类型
     * @param count 粒子数量
     */
    public static void spawnSphere(Level level, double x, double y, double z,
                                  double radius, ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.acos(2 * Math.random() - 1);
            
            double posX = x + radius * Math.sin(phi) * Math.cos(theta);
            double posY = y + radius * Math.cos(phi);
            double posZ = z + radius * Math.sin(phi) * Math.sin(theta);
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, posY, posZ, 1, 0, 0, 0, 0);
            }
        }
    }
    
    /**
     * 生成柱状粒子效果
     * @param level 世界
     * @param x 中心 X 坐标
     * @param y 底部 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param height 高度
     * @param particle 粒子类型
     * @param count 粒子数量
     */
    public static void spawnCylinder(Level level, double x, double y, double z,
                                    double radius, double height,
                                    ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double r = Math.random() * radius;
            double h = Math.random() * height;
            
            double posX = x + Math.cos(angle) * r;
            double posY = y + h;
            double posZ = z + Math.sin(angle) * r;
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, posY, posZ, 1, 0, 0, 0, 0);
            }
        }
    }
    
    /**
     * 生成心形图案
     * @param level 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param size 大小
     * @param particle 粒子类型
     * @param count 粒子数量
     */
    public static void spawnHeart(Level level, double x, double y, double z,
                                 double size, ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            double t = (double) i / count * Math.PI * 2;
            double scale = size / 15.0;
            
            double heartX = 16 * Math.pow(Math.sin(t), 3);
            double heartY = -(13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t));
            double heartZ = 16 * Math.pow(Math.sin(t), 3);
            
            double posX = x + heartX * scale;
            double posY = y + heartY * scale;
            double posZ = z + heartZ * scale;
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, posY, posZ, 1, 0, 0, 0, 0);
            }
        }
    }
    /**
     * 生成向前移动的斩击特效
     * @param level 世界
     * @param startX 起始 X 坐标
     * @param startY 起始 Y 坐标
     * @param startZ 起始 Z 坐标
     * @param dirX 方向 X 分量
     * @param dirZ 方向 Z 分量
     * @param distance 移动距离
     * @param width 斩击宽度
     * @param height 斩击高度
     * @param particle 粒子类型
     * @param segments 分段数量（越多越平滑）
     * @param speed 移动速度
     */
    public static void spawnSlashingAttack(Level level, double startX, double startY, double startZ,
                                           double dirX, double dirZ, double distance,
                                           double width, double height,
                                           ParticleOptions particle, int segments, double speed) {
        // 归一化方向
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len > 0) {
            dirX /= len;
            dirZ /= len;
        }

        // 垂直方向（用于宽度）
        double perpX = -dirZ;
        double perpZ = dirX;

        for (int s = 0; s < segments; s++) {
            double progress = (double) s / segments;
            double currentDist = progress * distance;

            // 当前中心位置
            double centerX = startX + dirX * currentDist;
            double centerY = startY;
            double centerZ = startZ + dirZ * currentDist;

            // 生成弧形斩击
            int particlesPerArc = 20;
            for (int i = 0; i < particlesPerArc; i++) {
                double angle = ((double) i / particlesPerArc - 0.5) * Math.PI;
                double arcWidth = width * Math.cos(angle);

                double posX = centerX + perpX * arcWidth;
                double posY = centerY + height * (0.5 + 0.5 * Math.sin(angle));
                double posZ = centerZ + perpZ * arcWidth;

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(particle, posX, posY, posZ, 1, 0, 0, 0, speed);
                }
            }
        }
    }

    /**
     * 生成旋转的魔法阵
     * @param level 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param layers 层数
     * @param rotationAngle 旋转角度（弧度）
     * @param particle 粒子类型
     * @param particlesPerLayer 每层粒子数
     * @param rotationSpeed 旋转速度
     */
    public static void spawnRotatingMagicCircle(Level level, double x, double y, double z,
                                                double radius, int layers, double rotationAngle,
                                                ParticleOptions particle, int particlesPerLayer,
                                                double rotationSpeed) {
        for (int layer = 0; layer < layers; layer++) {
            double layerRadius = radius * (1.0 - (double) layer / layers * 0.5);
            double layerY = y + (layer * 0.3);
            double layerRotation = rotationAngle + layer * Math.PI / 6;

            // 外层圆环
            for (int i = 0; i < particlesPerLayer; i++) {
                double angle = (double) i / particlesPerLayer * Math.PI * 2 + layerRotation;
                double posX = x + Math.cos(angle) * layerRadius;
                double posZ = z + Math.sin(angle) * layerRadius;

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(particle, posX, layerY, posZ, 1, 0, 0, 0, rotationSpeed);
                }
            }

            // 内部符文（6 个对称点）
            for (int i = 0; i < 6; i++) {
                double runeAngle = (double) i / 6 * Math.PI * 2 + layerRotation;
                double runeRadius = layerRadius * 0.6;
                double runeX = x + Math.cos(runeAngle) * runeRadius;
                double runeZ = z + Math.sin(runeAngle) * runeRadius;

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(particle, runeX, layerY + 0.2, runeZ, 1, 0, 0, 0, rotationSpeed);
                }
            }
        }
    }

    /**
     * 生成向上升腾的粒子效果
     * @param level 世界
     * @param x 中心 X 坐标
     * @param y 底部 Y 坐标
     * @param z 中心 Z 坐标
     * @param height 升腾高度
     * @param radius 底部半径
     * @param particle 粒子类型
     * @param count 粒子数量
     * @param swirlIntensity 旋转强度
     */
    public static void spawnAscendingSwirl(Level level, double x, double y, double z,
                                           double height, double radius,
                                           ParticleOptions particle, int count,
                                           double swirlIntensity) {
        for (int i = 0; i < count; i++) {
            double progress = (double) i / count;
            double currentY = y + progress * height;
            double currentRadius = radius * (1.0 - progress * 0.7);
            double angle = progress * Math.PI * 2 * 3 * swirlIntensity;

            double posX = x + Math.cos(angle) * currentRadius;
            double posZ = z + Math.sin(angle) * currentRadius;

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, currentY, posZ, 1, 0, 0, 0, 0.1);
            }
        }
    }

    /**
     * 生成多重环绕轨道
     * @param level 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param orbits 轨道数量
     * @param orbitRadius 轨道半径
     * @param tiltAngle 倾斜角度
     * @param particle 粒子类型
     * @param particlesPerOrbit 每条轨道粒子数
     */
    public static void spawnOrbitRings(Level level, double x, double y, double z,
                                       int orbits, double orbitRadius, double tiltAngle,
                                       ParticleOptions particle, int particlesPerOrbit) {
        for (int o = 0; o < orbits; o++) {
            double orbitTilt = tiltAngle + (double) o / orbits * Math.PI / 4;

            for (int i = 0; i < particlesPerOrbit; i++) {
                double angle = (double) i / particlesPerOrbit * Math.PI * 2;

                // 3D 旋转轨道
                double cosA = Math.cos(angle);
                double sinA = Math.sin(angle);
                double cosT = Math.cos(orbitTilt);
                double sinT = Math.sin(orbitTilt);

                double posX = x + orbitRadius * cosA;
                double posY = y + orbitRadius * sinA * cosT;
                double posZ = z + orbitRadius * sinA * sinT;

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(particle, posX, posY, posZ, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    /**
     * 生成爆炸冲击波特效
     * @param level 世界
     * @param x 爆炸中心 X
     * @param y 爆炸中心 Y
     * @param z 爆炸中心 Z
     * @param maxRadius 最大半径
     * @param particle 粒子类型
     * @param density 密度
     */
    public static void spawnShockwave(Level level, double x, double y, double z,
                                      double maxRadius, ParticleOptions particle, int density) {
        for (int ring = 0; ring < 5; ring++) {
            double ringRadius = maxRadius * ((double) ring / 5);
            int ringParticles = density + ring * 10;

            for (int i = 0; i < ringParticles; i++) {
                double angle = (double) i / ringParticles * Math.PI * 2;
                double spread = 0.2 * (5 - ring);

                double posX = x + Math.cos(angle) * ringRadius;
                double posY = y + (ring * 0.3);
                double posZ = z + Math.sin(angle) * ringRadius;

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(particle, posX, posY, posZ, 1, spread, spread, spread, 0.5);
                }
            }
        }
    }

    /**
     * 生成螺旋弹道特效（从起点到终点）
     * @param level 世界
     * @param startX 起点 X
     * @param startY 起点 Y
     * @param startZ 起点 Z
     * @param endX 终点 X
     * @param endY 终点 Y
     * @param endZ 终点 Z
     * @param rotations 旋转圈数
     * @param radius 螺旋半径
     * @param particle 粒子类型
     * @param count 粒子数量
     */
    public static void spawnSpiralProjectile(Level level, double startX, double startY, double startZ,
                                             double endX, double endY, double endZ,
                                             double rotations, double radius,
                                             ParticleOptions particle, int count) {
        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;

        for (int i = 0; i < count; i++) {
            double progress = (double) i / count;
            double angle = progress * Math.PI * 2 * rotations;

            // 直线插值
            double lineX = startX + dx * progress;
            double lineY = startY + dy * progress;
            double lineZ = startZ + dz * progress;

            // 垂直方向的偏移
            double perpX = -dz;
            double perpZ = dx;
            double perpLen = Math.sqrt(perpX * perpX + perpZ * perpZ);
            if (perpLen > 0) {
                perpX /= perpLen;
                perpZ /= perpLen;
            }

            double spiralX = Math.cos(angle) * radius * perpX;
            double spiralZ = Math.sin(angle) * radius * perpZ;
            double spiralY = Math.sin(angle) * radius * (dy > 0 ? 1 : -1);

            double posX = lineX + spiralX;
            double posY = lineY + spiralY;
            double posZ = lineZ + spiralZ;

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, posX, posY, posZ, 1, 0, 0, 0, 0.2);
            }
        }
    }

    /**
     * 生成羽翼展开特效
     * @param level 世界
     * @param x 中心 X
     * @param y 中心 Y
     * @param z 中心 Z
     * @param wingspan 翼展宽度
     * @param featherLength 羽毛长度
     * @param particle 粒子类型
     * @param density 密度
     */
    public static void spawnWingExpansion(Level level, double x, double y, double z,
                                          double wingspan, double featherLength,
                                          ParticleOptions particle, int density) {
        // 左翼
        for (int i = 0; i < density; i++) {
            double progress = (double) i / density;
            double wingX = x - progress * wingspan * 0.5;
            double wingY = y + Math.sin(progress * Math.PI) * featherLength * 0.3;
            double wingZ = z + Math.cos(progress * Math.PI * 0.5) * featherLength;

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, wingX, wingY, wingZ, 1, 0.1, 0.1, 0.1, 0.1);
            }
        }

        // 右翼
        for (int i = 0; i < density; i++) {
            double progress = (double) i / density;
            double wingX = x + progress * wingspan * 0.5;
            double wingY = y + Math.sin(progress * Math.PI) * featherLength * 0.3;
            double wingZ = z + Math.cos(progress * Math.PI * 0.5) * featherLength;

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particle, wingX, wingY, wingZ, 1, 0.1, 0.1, 0.1, 0.1);
            }
        }
    }

    /**
     * 生成锁链连接特效（连接两个点）
     * @param level 世界
     * @param x1 起点 X
     * @param y1 起点 Y
     * @param z1 起点 Z
     * @param x2 终点 X
     * @param y2 终点 Y
     * @param z2 终点 Z
     * @param chainCount 锁链数量
     * @param sagAmount 下垂程度
     * @param particle 粒子类型
     */
    public static void spawnChainLinks(Level level, double x1, double y1, double z1,
                                       double x2, double y2, double z2,
                                       int chainCount, double sagAmount,
                                       ParticleOptions particle) {
        for (int c = 0; c < chainCount; c++) {
            double offset = (c - chainCount / 2.0) * 0.3;
            int segments = 20;

            for (int i = 0; i <= segments; i++) {
                double progress = (double) i / segments;

                // 线性插值
                double lx = x1 + (x2 - x1) * progress;
                double ly = y1 + (y2 - y1) * progress;
                double lz = z1 + (z2 - z1) * progress;

                // 添加下垂曲线
                double curveHeight = Math.sin(progress * Math.PI) * sagAmount;
                double chainX = lx + offset;
                double chainY = ly - curveHeight;
                double chainZ = lz;

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(particle, chainX, chainY, chainZ, 1, 0.05, 0.05, 0.05, 0);
                }
            }
        }
    }

    /**
     * 生成领域展开特效（球形扩张）
     * @param level 世界
     * @param x 中心 X
     * @param y 中心 Y
     * @param z 中心 Z
     * @param maxRadius 最大半径
     * @param shells 壳层数量
     * @param particle 粒子类型
     * @param particlesPerShell 每壳层粒子数
     */
    public static void spawnDomainExpansion(Level level, double x, double y, double z,
                                            double maxRadius, int shells,
                                            ParticleOptions particle, int particlesPerShell) {
        for (int shell = 0; shell < shells; shell++) {
            double shellRadius = maxRadius * ((double) shell / shells);

            for (int i = 0; i < particlesPerShell; i++) {
                double theta = Math.random() * Math.PI * 2;
                double phi = Math.acos(2 * Math.random() - 1);

                double posX = x + shellRadius * Math.sin(phi) * Math.cos(theta);
                double posY = y + shellRadius * Math.cos(phi);
                double posZ = z + shellRadius * Math.sin(phi) * Math.sin(theta);

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(particle, posX, posY, posZ, 1, 0, 0, 0, 0.05);
                }
            }
        }
    }
}
