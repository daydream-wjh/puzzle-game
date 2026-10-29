package com.wjh.ui;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * 素材定位与读取。
 * <p>
 * 素材目录默认是工作目录下的 {@code img/}；如果用 {@code -Dpuzzlegame.assets=<目录>} 指定了目录，
 * 或从 {@code tools/} 之类的手上一层目录启动，会自动向上找一层带 {@code img/all.png} 的目录，
 * 免得截图脚本和游戏读的不是同一批图。素材本身只读，不改名、不改内容。
 */
public final class Assets {

    /** 素材目录系统属性。 */
    public static final String ASSETS_PROPERTY = "puzzlegame.assets";

    private static final File DIR = resolveDir();

    private Assets() {
    }

    /** 素材目录。 */
    public static File dir() {
        return DIR;
    }

    /** 取素材文件。 */
    public static File file(String name) {
        return new File(DIR, name);
    }

    /** 读一张图；读不到返回 null（调用方画占位色，不让界面起不来）。 */
    public static BufferedImage read(String name) {
        File f = file(name);
        if (!f.isFile()) {
            System.err.println("[Assets] 找不到素材：" + f.getAbsolutePath());
            return null;
        }
        try {
            return ImageIO.read(f);
        } catch (IOException e) {
            System.err.println("[Assets] 读取素材失败：" + f.getAbsolutePath());
            return null;
        }
    }

    private static File resolveDir() {
        String configured = System.getProperty(ASSETS_PROPERTY);
        if (configured != null && !configured.trim().isEmpty()) {
            return new File(configured.trim());
        }
        File cursor = new File(".").getAbsoluteFile();
        for (int i = 0; i < 5 && cursor != null; i++) {
            File candidate = new File(cursor, "img");
            if (new File(candidate, "all.png").isFile()) {
                return candidate;
            }
            cursor = cursor.getParentFile();
        }
        return new File("img");
    }
}
