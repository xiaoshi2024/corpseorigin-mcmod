// 尸巢召唤 - 尸兄玩家的终极技能
var skill = {
    activate: function(player, world, data) {
        // 1. 检查玩家是否在服务器端
        if (!world.isClientSide) {
            // 2. 播放召唤特效粒子
            for (var i = 0; i < 100; i++) {
                var spawnX = player.getX() + (Math.random() - 0.5) * 8;
                var spawnY = player.getY() + (Math.random() - 0.5) * 6;
                var spawnZ = player.getZ() + (Math.random() - 0.5) * 8;
                
                world.sendParticles(
                    Packages.net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                    spawnX, spawnY, spawnZ,
                    1, (Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3, 0
                );
            }

            // 3. 播放血肉生长音效
            world.playSound(
                null, player.getX(), player.getY(), player.getZ(),
                Packages.net.minecraft.sounds.SoundEvents.SLIME_BLOCK_PLACE,
                Packages.net.minecraft.sounds.SoundSource.PLAYERS, 2.0, 0.5
            );

            // 4. 在玩家脚下生成尸巢肉块
            var blockPos = player.blockPosition();
            var newPos = new Packages.net.minecraft.core.BlockPos(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ());
            
            // 5. 生成尸巢肉块方块
            var fleshBlock = Packages.net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                Packages.net.minecraft.resources.ResourceLocation.tryParse("corpseorigin:zbr_flesh")
            );
            
            if (fleshBlock) {
                world.setBlock(newPos, fleshBlock.defaultBlockState(), 3);
                
                // 6. 给肉块方块添加初始击杀数和主人信息
                var blockEntity = world.getBlockEntity(newPos);
                if (blockEntity) {
                    // 使用反射设置击杀数和主人
                    try {
                        var killsField = blockEntity.getClass().getDeclaredField("kills");
                        killsField.setAccessible(true);
                        killsField.setInt(blockEntity, 5); // 初始击杀数为5
                        
                        var ownerField = blockEntity.getClass().getDeclaredField("owner");
                        ownerField.setAccessible(true);
                        ownerField.set(blockEntity, player.getUUID()); // 设置主人
                    } catch (e) {
                        // 如果无法设置，就保持默认值
                    }
                }
            }
            
            // 7. 通知周围64格内的所有尸兄奴仆
            var searchBox = new Packages.net.minecraft.world.phys.AABB(
                player.getX() - 64, player.getY() - 64, player.getZ() - 64,
                player.getX() + 64, player.getY() + 64, player.getZ() + 64
            );
            
            var zbEntities = world.getEntitiesOfClass(
                Java.type("com.phagens.corpseorigin.entity.LowerLevelZbEntity").class,
                searchBox
            );
            
            // 添加调试日志
            if (isPlayer) {
                player.sendSystemMessage(
                    Packages.net.minecraft.network.chat.Component.literal("找到 " + zbEntities.size() + " 个尸兄奴仆")
                );
            }
            
            // 8. 命令尸兄奴仆自我吞噬，为尸巢贡献击杀数
            var totalKillsAdded = 0;
            var isCreative = false;
            var isPlayer = player instanceof Packages.net.minecraft.server.level.ServerPlayer;
            
            // 只有玩家才有 isCreative 和 sendSystemMessage 方法
            if (isPlayer) {
                isCreative = player.isCreative();
                player.sendSystemMessage(
                    Packages.net.minecraft.network.chat.Component.literal("玩家模式: " + (isCreative ? "创造" : "生存"))
                );
            }
            
            for (var i = 0; i < zbEntities.size(); i++) {
                var zb = zbEntities.get(i);
                
                // 检查是否是玩家的奴仆（创造模式下不检查，非玩家实体也不检查）
                var isPlayerServant = true; // 非玩家实体默认所有尸兄都是奴仆
                
                if (isPlayer) {
                    try {
                        var masterField = zb.getClass().getDeclaredField("masterUUID");
                        masterField.setAccessible(true);
                        var master = masterField.get(zb);
                        if (master && master.equals(player.getUUID())) {
                            isPlayerServant = true;
                        } else {
                            isPlayerServant = false;
                        }
                    } catch (e) {
                        // 如果无法获取主人信息，跳过这个尸兄
                        if (isPlayer) {
                            player.sendSystemMessage(
                                Packages.net.minecraft.network.chat.Component.literal("无法获取尸兄主人信息: " + e)
                            );
                        }
                        continue;
                    }
                }
                
                // 创造模式或非玩家实体下不检查，否则只有被玩家奴役的尸兄才行
                if (isPlayer && !isCreative && !isPlayerServant) {
                    player.sendSystemMessage(
                        Packages.net.minecraft.network.chat.Component.literal("跳过非玩家奴仆的尸兄")
                    );
                    continue; // 跳过不是玩家奴仆的尸兄
                }
                
                if (isPlayer) {
                    player.sendSystemMessage(
                        Packages.net.minecraft.network.chat.Component.literal("处理尸兄 " + (isPlayerServant ? "玩家奴仆" : "非玩家奴仆"))
                    );
                }
                
                // 先让尸兄移动到肉块位置，贡献击杀数
                var distance = Math.sqrt(
                    Math.pow(zb.getX() - newPos.getX(), 2) +
                    Math.pow(zb.getY() - (newPos.getY() + 1), 2) +
                    Math.pow(zb.getZ() - newPos.getZ(), 2)
                );
                
                // 如果尸兄距离肉块较远，先传送到肉块附近
                if (distance > 5) {
                    zb.teleportTo(newPos.getX() + 0.5, newPos.getY() + 1, newPos.getZ() + 0.5);
                }
                
                // 播放自我吞噬特效
                for (var j = 0; j < 20; j++) {
                    var effectX = zb.getX() + (Math.random() - 0.5) * 2;
                    var effectY = zb.getY() + zb.getBbHeight() / 2 + (Math.random() - 0.5) * 2;
                    var effectZ = zb.getZ() + (Math.random() - 0.5) * 2;
                    
                    world.sendParticles(
                        Packages.net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                        effectX, effectY, effectZ,
                        1, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.2, 0
                    );
                }
                
                // 尸兄自我吞噬，为尸巢贡献击杀数
                zb.hurt(world.damageSources().magic(), 9999); // 自我牺牲
                totalKillsAdded++; // 记录贡献的击杀数
                
                // 播放自我吞噬音效
                world.playSound(
                    null, zb.getX(), zb.getY(), zb.getZ(),
                    Packages.net.minecraft.sounds.SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                    Packages.net.minecraft.sounds.SoundSource.PLAYERS, 1.0, 0.8
                );
            }
            
            // 8.1 直接为肉块增加击杀数，确保尸巢能够蔓延
            if (totalKillsAdded > 0) {
                var finalBlockEntity = world.getBlockEntity(newPos);
                if (finalBlockEntity) {
                    try {
                        var killsField = finalBlockEntity.getClass().getDeclaredField("kills");
                        killsField.setAccessible(true);
                        var currentKills = killsField.getInt(finalBlockEntity);
                        killsField.setInt(finalBlockEntity, currentKills + totalKillsAdded); // 增加击杀数
                        
                        if (isPlayer) {
                            player.sendSystemMessage(
                                Packages.net.minecraft.network.chat.Component.literal("尸巢肉块击杀数更新: " + currentKills + " -> " + (currentKills + totalKillsAdded))
                            );
                        }
                    } catch (e) {
                        if (isPlayer) {
                            player.sendSystemMessage(
                                Packages.net.minecraft.network.chat.Component.literal("设置击杀数失败: " + e)
                            );
                        }
                    }
                }
            } else {
                if (isPlayer) {
                    player.sendSystemMessage(
                        Packages.net.minecraft.network.chat.Component.literal("没有尸兄贡献击杀数")
                    );
                }
            }

            // 9. 播放震撼音效
            world.playSound(
                null, player.getX(), player.getY(), player.getZ(),
                Packages.net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                Packages.net.minecraft.sounds.SoundSource.PLAYERS, 1.5, 0.8
            );
            
            // 10. 发送消息给玩家
            if (isPlayer) {
                player.sendSystemMessage(
                    Packages.net.minecraft.network.chat.Component.literal("尸巢已召唤！奴仆们正在自我吞噬为尸巢贡献力量...")
                );
            }
        }

        return true;
    }
};

// 返回技能对象
skill;