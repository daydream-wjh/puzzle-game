package com.wjh.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户数据（userinfo.txt）的读写入口。
 * <p>
 * 数据文件位置按下面的顺序解析，macOS / Linux / Windows 通用：
 * <ol>
 *     <li>系统属性 {@code -Dpuzzlegame.data=<文件或目录>}；</li>
 *     <li>环境变量 {@code PUZZLEGAME_DATA=<文件或目录>}；</li>
 *     <li>当前工作目录下的 {@code data/userinfo.txt}（存在时优先）；</li>
 *     <li>classpath 同级（源码根 / 工程根）的 {@code data/userinfo.txt}；</li>
 *     <li>都不存在时退回第 3 项，并自动创建父目录 + 一条默认账号。</li>
 * </ol>
 * 配置成目录时会自动补上 {@code userinfo.txt}；从工程根目录启动时行为与老版本一致。
 */
public final class UserStore {

    /** 系统属性名。 */
    public static final String DATA_PROPERTY = "puzzlegame.data";
    /** 环境变量名。 */
    public static final String DATA_ENV = "PUZZLEGAME_DATA";
    /** 新建数据文件时写入的默认账号。 */
    public static final String DEFAULT_LINE = "username=admin&password=123456";
    /** 数据文件名。 */
    public static final String FILE_NAME = "userinfo.txt";

    /** 用户数据文件，进程内只解析一次。 */
    public static final Path USER_FILE = resolveUserFile();

    private UserStore() {
    }

    /** 按行读取用户数据；文件不存在时自动创建（失败则回退到默认账号）。 */
    public static synchronized List<String> load() {
        Path file = USER_FILE;
        try {
            ensureFile(file);
            List<String> result = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line == null ? "" : line.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        } catch (IOException e) {
            // 读盘失败不应该让登录窗口起不来，退回默认账号并留下日志。
            System.err.println("[UserStore] 读取用户数据失败：" + file);
            e.printStackTrace();
            List<String> fallback = new ArrayList<>();
            fallback.add(DEFAULT_LINE);
            return fallback;
        }
    }

    /** 按行覆盖写入用户数据（每行一个 {@code username=x&password=y}）。 */
    public static synchronized void save(List<String> lines) {
        Path file = USER_FILE;
        try {
            ensureParent(file);
            List<String> content = lines == null ? Collections.<String>emptyList() : lines;
            Files.write(file, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("写入用户数据失败：" + file, e);
        }
    }

    private static Path resolveUserFile() {
        String configured = firstNonBlank(System.getProperty(DATA_PROPERTY), System.getenv(DATA_ENV));
        if (configured != null) {
            Path path = Paths.get(configured);
            if (Files.isDirectory(path)
                    || configured.endsWith("/")
                    || configured.endsWith("\\")) {
                path = path.resolve(FILE_NAME);
            }
            return path.toAbsolutePath().normalize();
        }

        // 先找已经存在的数据文件，工作目录优先，其次 classpath 同级的工程根目录。
        Path fallback = null;
        for (Path root : candidateRoots()) {
            Path candidate = root.resolve("data").resolve(FILE_NAME);
            if (fallback == null) {
                fallback = candidate;
            }
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return fallback;
    }

    private static List<Path> candidateRoots() {
        List<Path> roots = new ArrayList<>();
        roots.add(Paths.get("").toAbsolutePath().normalize());
        Path codeRoot = locateProjectRoot();
        if (codeRoot != null && !roots.contains(codeRoot)) {
            roots.add(codeRoot);
        }
        return roots;
    }

    /**
     * 从 classpath 位置向上找工程根目录（同时带 {@code img} 和 {@code src} 的那一层），
     * 兼容 IDEA 的 out/production/puzzlegame 与命令行编译目录。找不准就返回 null。
     */
    private static Path locateProjectRoot() {
        try {
            URL url = UserStore.class.getProtectionDomain().getCodeSource().getLocation();
            if (url == null) {
                return null;
            }
            Path cursor = Paths.get(url.toURI());
            if (!Files.isDirectory(cursor)) {
                return null;
            }
            for (int i = 0; i < 6 && cursor != null; i++) {
                if (Files.isDirectory(cursor.resolve("img")) && Files.isDirectory(cursor.resolve("src"))) {
                    return cursor;
                }
                cursor = cursor.getParent();
            }
        } catch (Exception ignored) {
            // 取不到 code source（自定义 classloader 等）就忽略，退回工作目录。
        }
        return null;
    }

    private static void ensureFile(Path file) throws IOException {
        if (Files.exists(file)) {
            return;
        }
        ensureParent(file);
        List<String> seed = legacySeed();
        if (seed.isEmpty()) {
            seed.add(DEFAULT_LINE);
        }
        Files.write(file, seed, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    /**
     * 首次创建数据文件时，若仓库里还留着老的 {@code src/com/wjh/ui/userinfo.txt}，
     * 就把里面的账号迁过来（只读不改原文件），避免升级后老账号登不进去。
     */
    private static List<String> legacySeed() {
        List<String> seed = new ArrayList<>();
        for (Path root : candidateRoots()) {
            Path legacy = root.resolve("src").resolve("com").resolve("wjh").resolve("ui").resolve(FILE_NAME);
            if (!Files.isRegularFile(legacy)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(legacy, StandardCharsets.UTF_8)) {
                    String trimmed = line == null ? "" : line.trim();
                    if (!trimmed.isEmpty()) {
                        seed.add(trimmed);
                    }
                }
                if (!seed.isEmpty()) {
                    return seed;
                }
            } catch (IOException ignored) {
                // 迁移失败就用默认账号。
            }
        }
        return seed;
    }

    private static void ensureParent(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }
}
