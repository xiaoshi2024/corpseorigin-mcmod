package xiaoshi2022.corpseorigin.skin;

import net.fabricmc.loader.api.FabricLoader;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地皮肤名单（双端通用）：读 {@code config/corpseorigin/skins/<folder>/*.png} 的文件名。
 * <p>
 * 用法：把皮肤 PNG 丢进该文件夹（文件名 = 皮肤名，如 {@code coser1.png}），
 * 服务端 {@code /summonmanzhan} 从这里拿名单召唤；客户端
 * {@code LocalSkinStore} 按同名文件加载纹理，命中本地皮肤就不再查 Mojang。
 * <p>
 * 文件夹不存在时视为空名单（不会主动创建，避免把用户目录搞乱——
 * 指令提示里会给出应放置的完整路径）。
 */
public final class LocalSkinNames {

    private LocalSkinNames() {
    }

    /** 皮肤文件夹：{@code config/corpseorigin/skins/<spawn.manzhan.folder>} */
    public static Path folder() {
        var cfg = CorpseConfig.get().spawn.manzhan;
        String sub = cfg == null ? null : cfg.folder;
        sub = (sub == null || sub.isBlank()) ? "manzhan" : sub.trim();
        return FabricLoader.getInstance().getConfigDir()
                .resolve("corpseorigin").resolve("skins").resolve(sub);
    }

    /** 名单 = 文件名去掉 {@code .png} 后缀（按文件系统返回顺序）。 */
    public static List<String> listNames() {
        List<String> names = new ArrayList<>();
        Path dir = folder();
        if (!Files.isDirectory(dir)) {
            return names;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                if (name.toLowerCase().endsWith(".png")) {
                    name = name.substring(0, name.length() - 4);
                }
                if (!name.isBlank()) {
                    names.add(name);
                }
            }
        } catch (IOException e) {
            CorpseOrigin.LOGGER.warn("读取本地皮肤名单失败 {}: {}", dir, e.getMessage());
        }
        return names;
    }

    /** 该名字是否对应一个本地皮肤文件（忽略大小写）。 */
    public static boolean isLocal(String name) {
        return resolveFile(name) != null;
    }

    /**
     * 按皮肤名解析到具体 PNG 文件：先精确匹配，再忽略大小写兜底
     * （Windows 文件系统不区分大小写，Linux 区分，这里统一处理）。
     */
    public static Path resolveFile(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Path dir = folder();
        Path exact = dir.resolve(name + ".png");
        if (Files.isRegularFile(exact)) {
            return exact;
        }
        if (!Files.isDirectory(dir)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                if (fileName.equalsIgnoreCase(name + ".png")) {
                    return file;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
