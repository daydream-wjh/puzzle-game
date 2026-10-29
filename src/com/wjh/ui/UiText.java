package com.wjh.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * 统一的文字绘制小工具（左对齐 / 居中 / 右对齐 / 量宽）。
 * <p>
 * 游戏画面和登录页共用同一套，离屏截图与真实窗口的字距才一致。
 */
public final class UiText {

    private UiText() {
    }

    /** 左对齐，baseline 是文字基线。 */
    public static void text(Graphics2D g, String s, int x, int baseline, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        g.drawString(s, x, baseline);
    }

    /** 以 centerX 水平居中。 */
    public static void center(Graphics2D g, String s, int centerX, int baseline, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        g.drawString(s, centerX - g.getFontMetrics().stringWidth(s) / 2, baseline);
    }

    /** 以 rightX 右对齐。 */
    public static void right(Graphics2D g, String s, int rightX, int baseline, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        g.drawString(s, rightX - g.getFontMetrics().stringWidth(s), baseline);
    }

    /** 量一段文字的宽度。 */
    public static int width(Graphics2D g, String s, Font font) {
        g.setFont(font);
        return g.getFontMetrics().stringWidth(s);
    }
}
