package com.wjh.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Set;

/**
 * 字体挑选：界面里有中文，优先用能显示汉字的系统字体，找不到再退回逻辑字体 SansSerif。
 * <p>
 * 离屏渲染（无显示环境）时同样按这套顺序找，保证截图和真实窗口的字形一致。
 */
public final class Fonts {

    /** 候选字体，按优先级排列（macOS → Windows → Linux → 兜底）。 */
    private static final String[] CANDIDATES = {
            "PingFang SC", "Hiragino Sans GB", "Heiti SC",
            "Microsoft YaHei", "Noto Sans CJK SC", "Source Han Sans SC",
            "WenQuanYi Micro Hei", "SansSerif"
    };

    private static final String FAMILY = pickFamily();

    private Fonts() {
    }

    /** 常规字重。 */
    public static Font plain(int size) {
        return new Font(FAMILY, Font.PLAIN, size);
    }

    /** 粗体。 */
    public static Font bold(int size) {
        return new Font(FAMILY, Font.BOLD, size);
    }

    /** 选中字体的族名（调试用）。 */
    public static String family() {
        return FAMILY;
    }

    private static String pickFamily() {
        Set<String> installed = new HashSet<>();
        try {
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                installed.add(name);
            }
        } catch (Throwable ignored) {
            // 极端环境下取不到字体列表，直接按候选名尝试。
        }
        for (String name : CANDIDATES) {
            Font font = new Font(name, Font.PLAIN, 14);
            boolean ok = font.canDisplay('拼') && font.canDisplay('图');
            if (!ok) {
                continue;
            }
            // 没装这个字体时 Java 会悄悄替换成默认字体，这里确认族名真的对得上，
            // 避免「号称 PingFang SC 其实是 Dialog」。
            if (installed.isEmpty() || installed.contains(font.getFamily())) {
                return font.getFamily();
            }
        }
        return Font.SANS_SERIF;
    }
}
