package com.xiaoshi2022.corpseorigin.growth;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Read originals in place; write disposable converted resource packs for GeckoLib 4. */
public final class OrganPacks {
    private OrganPacks() {}
    public static List<Path> sources(Path directory) throws IOException {
        Files.createDirectories(directory);
        try (var paths = Files.list(directory)) {
            return paths.filter(p -> !p.getFileName().toString().equals("examples") &&
                    (Files.isDirectory(p) && Files.exists(p.resolve("pack.mcmeta")) || p.toString().toLowerCase(Locale.ROOT).endsWith(".zip")))
                    .sorted().toList();
        }
    }
    public static void readManifest(Path pack, Map<String, OrganDefinition> output, Set<String> custom) throws IOException {
        if (Files.isDirectory(pack)) {
            if (!Files.exists(pack.resolve("pack.mcmeta"))) throw OrganText.io("missing_meta");
            readDefinitions(Files.newInputStream(pack.resolve("organ.json")), output, custom);
        } else try (ZipFile zip = new ZipFile(pack.toFile())) {
            if (zip.getEntry("pack.mcmeta") == null || zip.getEntry("organ.json") == null)
                throw OrganText.io("zip_root");
            readDefinitions(zip.getInputStream(zip.getEntry("organ.json")), output, custom);
        }
    }
    public static void readDefinitions(InputStream input, Map<String, OrganDefinition> output, Set<String> custom) throws IOException {
        try (input) {
            byte[] bytes = input.readNBytes(262145);
            if (bytes.length > 262144) throw OrganText.io("catalog_size");
            var defs = OrganLibrary.JSON.fromJson(new String(bytes, StandardCharsets.UTF_8), OrganDefinition[].class);
            if (defs == null) throw OrganText.io("manifest_array");
            var candidate = new LinkedHashMap<>(output);
            for (var def : defs) {
                if (def == null || !def.valid()) throw OrganText.io("invalid_definition");
                candidate.put(def.id(), def.normalized());
            }
            if (candidate.size() > 128 || OrganLibrary.JSON.toJson(candidate.values()).length() > 262144)
                throw OrganText.io("catalog_limit");
            output.clear(); output.putAll(candidate);
            for (var def : defs) custom.add(def.id());
        }
    }
    public static void convert(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temp = Files.createTempFile(destination.getParent(), "organ-", ".tmp");
        try {
            try (var out = new ZipOutputStream(Files.newOutputStream(temp))) {
                out.putNextEntry(new ZipEntry("pack.mcmeta"));
                out.write("{\"pack\":{\"pack_format\":15,\"description\":\"CorpseOrigin organ resources\"}}".getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
                Set<String> names = new HashSet<>(); long[] total = {0};
                if (Files.isDirectory(source)) {
                    try (var paths = Files.walk(source.resolve("assets"))) {
                        for (Path p : paths.filter(Files::isRegularFile).sorted().toList())
                            copy(source.relativize(p).toString().replace('\\', '/'), Files.newInputStream(p), out, names, total);
                    }
                } else try (var zip = new ZipFile(source.toFile())) {
                    var entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        var e = entries.nextElement();
                        if (!e.isDirectory() && e.getName().startsWith("assets/")) copy(e.getName(), zip.getInputStream(e), out, names, total);
                    }
                }
            }
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    private static void copy(String name, InputStream input, ZipOutputStream out, Set<String> names, long[] total) throws IOException {
        try (input) {
            if (!name.startsWith("assets/") || name.contains("..") || name.contains("\\")) throw OrganText.io("invalid_path");
            name = name.replace("/geckolib/models/", "/geo/").replace("/geckolib/animations/", "/animations/");
            if (!names.add(name)) throw OrganText.io("duplicate_path",name);
            if (names.size() > 8192) throw OrganText.io("file_limit");
            out.putNextEntry(new ZipEntry(name));
            byte[] buffer = new byte[8192]; int count; long size = 0;
            while ((count = input.read(buffer)) != -1) {
                size += count; total[0] += count;
                if (size > 32L * 1024 * 1024 || total[0] > 128L * 1024 * 1024) throw OrganText.io("pack_size");
                out.write(buffer, 0, count);
            }
            out.closeEntry();
        }
    }
}
