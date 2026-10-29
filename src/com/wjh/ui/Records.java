package com.wjh.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 最佳纪录读写：默认落在 {@code ~/.puzzle-game/records.properties}，
 * 用 {@link java.util.Properties} 存，不依赖任何第三方库。
 * <p>
 * 每个难度一组键（前缀是边长，如 {@code 4.}）：
 * <pre>
 * 4.steps = 148      # 最少步数
 * 4.time  = 91       # 最快用时（秒）
 * </pre>
 * 文件位置可以用系统属性 {@code -Dpuzzlegame.records=<文件或目录>} 临时改掉（截图脚本/测试用），
 * 配置成目录时会自动补上 {@code records.properties}。
 */
public final class Records {

    /** 系统属性名：覆盖纪录文件位置。 */
    public static final String RECORDS_PROPERTY = "puzzlegame.records";
    /** 默认目录名（用户主目录下）。 */
    public static final String DIR_NAME = ".puzzle-game";
    /** 文件名。 */
    public static final String FILE_NAME = "records.properties";

    private Records() {
    }

    /** 纪录文件路径（每次调用都重新解析，方便测试里改系统属性）。 */
    public static Path file() {
        String configured = System.getProperty(RECORDS_PROPERTY);
        if (configured != null && !configured.trim().isEmpty()) {
            Path path = Paths.get(configured.trim());
            String raw = configured.trim();
            if (Files.isDirectory(path) || raw.endsWith("/") || raw.endsWith("\\")) {
                path = path.resolve(FILE_NAME);
            }
            return path.toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.home", "."), DIR_NAME, FILE_NAME);
    }

    /** 读某一难度的纪录；没有记录时返回 {@link Best#EMPTY}。 */
    public static Best load(Difficulty difficulty) {
        Properties props = readAll();
        String prefix = difficulty.getRecordPrefix();
        int steps = parseInt(props.getProperty(prefix + "steps"), -1);
        int timeSeconds = parseInt(props.getProperty(prefix + "time"), -1);
        return new Best(steps, timeSeconds);
    }

    /**
     * 提交一次成绩，返回这一局打破的纪录情况；并把刷新后的纪录写盘。
     * 步数与用时分别判定，互不影响。
     */
    public static Result submit(Difficulty difficulty, int steps, int timeSeconds) {
        Best old = load(difficulty);
        Best merged = new Best(
                old.hasSteps() ? Math.min(old.getSteps(), steps) : steps,
                old.hasTime() ? Math.min(old.getTimeSeconds(), timeSeconds) : timeSeconds);
        Properties props = readAll();
        String prefix = difficulty.getRecordPrefix();
        props.setProperty(prefix + "steps", String.valueOf(merged.getSteps()));
        props.setProperty(prefix + "time", String.valueOf(merged.getTimeSeconds()));
        writeAll(props);
        return new Result(old, merged).mark(steps, timeSeconds);
    }

    /** 把成绩和旧纪录比一下，只标注、不落盘（截图/展示用）。 */
    public static Result compare(Best old, int steps, int timeSeconds) {
        return new Result(old, old).mark(steps, timeSeconds);
    }

    private static Properties readAll() {
        Properties props = new Properties();
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return props;
        }
        try (java.io.InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            // 读不出来就当没有纪录，不让游戏起不来。
            System.err.println("[Records] 读取纪录失败：" + path);
        }
        return props;
    }

    private static void writeAll(Properties props) {
        Path path = file();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (java.io.OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "puzzle-game best records");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("写入纪录失败：" + path, e);
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** 某一难度的纪录快照，-1 表示还没有成绩。 */
    public static final class Best {
        /** 空纪录。 */
        public static final Best EMPTY = new Best(-1, -1);

        private final int steps;
        private final int timeSeconds;

        /** 构造一条纪录（截图工具、测试用；-1 表示该项暂无）。 */
        public static Best of(int steps, int timeSeconds) {
            return new Best(steps, timeSeconds);
        }

        Best(int steps, int timeSeconds) {
            this.steps = steps;
            this.timeSeconds = timeSeconds;
        }

        public int getSteps() {
            return steps;
        }

        public int getTimeSeconds() {
            return timeSeconds;
        }

        public boolean hasSteps() {
            return steps >= 0;
        }

        public boolean hasTime() {
            return timeSeconds >= 0;
        }

        /** 步数纪录展示串。 */
        public String stepsText() {
            return hasSteps() ? steps + " 步" : "暂无";
        }

        /** 用时纪录展示串。 */
        public String timeText() {
            return hasTime() ? BoardRenderer.formatTime(timeSeconds * 1000L) : "暂无";
        }

        @Override
        public String toString() {
            return "Best{steps=" + steps + ", time=" + timeSeconds + "s}";
        }
    }

    /** 一次成绩的判定结果：这一步/这一秒是不是刷新了纪录。 */
    public static final class Result {
        private final Best before;
        private Best after;
        private boolean newSteps;
        private boolean newTime;

        Result(Best before, Best after) {
            this.before = before;
            this.after = after;
        }

        Result mark(int steps, int timeSeconds) {
            newSteps = !before.hasSteps() || steps < before.getSteps();
            newTime = !before.hasTime() || timeSeconds < before.getTimeSeconds();
            return this;
        }

        public boolean isNewSteps() {
            return newSteps;
        }

        public boolean isNewTime() {
            return newTime;
        }

        public boolean isAny() {
            return newSteps || newTime;
        }

        public Best getBefore() {
            return before;
        }

        public Best getAfter() {
            return after;
        }

        /** 纪录变化的展示串，如「新纪录：最少步数 / 最快用时」。 */
        public String text() {
            if (newSteps && newTime) {
                return "新纪录：最少步数 + 最快用时";
            }
            if (newSteps) {
                return "新纪录：最少步数";
            }
            if (newTime) {
                return "新纪录：最快用时";
            }
            return "未破纪录，继续加油";
        }
    }
}
