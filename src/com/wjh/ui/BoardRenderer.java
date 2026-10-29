package com.wjh.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * 游戏界面的唯一绘制入口：给一个 {@link BoardView}，把整帧画面画到 {@link Graphics2D} 上。
 * <p>
 * 只吃 {@code Graphics2D}，不依赖组件树，所以同一份代码既能给真实窗口画（{@link GamePanel}），
 * 也能在 {@code tools/Snapshot} 里离屏画进 {@code BufferedImage} 存成 PNG —— 截图不会和界面漂移。
 * 几何计算（格子、状态栏按钮）也在这里，点击命中判定复用同一套坐标。
 */
public final class BoardRenderer {

    /** 画面宽（窗口内容区同宽）。 */
    public static final int WIDTH = 880;
    /** 画面高。 */
    public static final int HEIGHT = 880;

    /** 拼图区左上角。 */
    public static final int BOARD_X = 160;
    /** 拼图区上边。 */
    public static final int BOARD_Y = 132;
    /** 拼图区边长（不是单格尺寸：单格 = 边长 / 难度）。 */
    public static final int BOARD_SIDE = 560;

    private static final int STATUS_X = 36;
    private static final int STATUS_Y = 712;
    private static final int STATUS_W = 808;
    private static final int STATUS_H = 132;
    private static final int STATUS_PAD = 28;

    private static final Color BG_TOP = new Color(0x1A1512);
    private static final Color BG_BOTTOM = new Color(0x2C231B);
    private static final Color CREAM = new Color(0xF6EAD7);
    private static final Color MUTED = new Color(0x9C8B78);
    private static final Color GOLD = new Color(0xE8BE7C);
    private static final Color GOLD_DEEP = new Color(0xC08A2E);
    private static final Color INK = new Color(0x2A2118);
    private static final Color CARD = new Color(0xFBF1DE);

    /** 帧内缓存的窗口装饰图。 */
    private static Image winImage;

    private BoardRenderer() {
    }

    /** 整个拼图区的矩形。 */
    public static Rectangle boardRect() {
        return new Rectangle(BOARD_X, BOARD_Y, BOARD_SIDE, BOARD_SIDE);
    }

    /** 第 r 行第 c 列的像素矩形；用四舍五入切分，避免出现 1px 缝隙。 */
    public static Rectangle cellRect(int size, int row, int col) {
        double cell = (double) BOARD_SIDE / size;
        int x0 = BOARD_X + (int) Math.round(col * cell);
        int x1 = BOARD_X + (int) Math.round((col + 1) * cell);
        int y0 = BOARD_Y + (int) Math.round(row * cell);
        int y1 = BOARD_Y + (int) Math.round((row + 1) * cell);
        return new Rectangle(x0, y0, x1 - x0, y1 - y0);
    }

    /**
     * 把画面坐标换成格子坐标；不在拼图区里返回 null。
     * 鼠标点击和真实窗口共用它，点哪儿就是哪儿。
     */
    public static int[] cellAt(int size, int px, int py) {
        Rectangle board = boardRect();
        if (!board.contains(px, py)) {
            return null;
        }
        double cell = (double) BOARD_SIDE / size;
        int col = (int) ((px - BOARD_X) / cell);
        int row = (int) ((py - BOARD_Y) / cell);
        if (row < 0 || row >= size || col < 0 || col >= size) {
            return null;
        }
        return new int[]{row, col};
    }

    /** 状态栏「撤销」按钮的矩形。 */
    public static Rectangle undoButton() {
        return new Rectangle(STATUS_X + STATUS_PAD, STATUS_Y + 92, 120, 34);
    }

    /** 状态栏「重开」按钮的矩形。 */
    public static Rectangle restartButton() {
        return new Rectangle(STATUS_X + STATUS_PAD + 132, STATUS_Y + 92, 100, 34);
    }

    /** 底色：面板比 880×880 大时（窗口被拉伸）用它先铺满，避免露出系统默认灰。 */
    public static Color backgroundColor() {
        return BG_BOTTOM;
    }

    /** 毫秒 → MM:SS。 */
    public static String formatTime(long millis) {
        long totalSeconds = Math.max(0, millis) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    /** 画一帧。 */
    public static void paint(Graphics2D graphics, BoardView view, Tiles tiles) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        drawBackground(g);
        drawHeader(g, view);
        drawBoard(g, view, tiles);
        drawStatusBar(g, view);
        g.dispose();
    }

    // ------------------------------------------------------------------ 背景 / 标题

    private static void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, HEIGHT, BG_BOTTOM));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // 拼图区背后一团暖光，让画面中心亮起来。
        float[] fractions = {0f, 1f};
        Color[] colors = {new Color(232, 190, 124, 34), new Color(232, 190, 124, 0)};
        g.setPaint(new RadialGradientPaint(new Point2D.Float(WIDTH / 2f, 400f), 520f, fractions, colors));
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private static void drawHeader(Graphics2D g, BoardView view) {
        text(g, "拼图游戏", BOARD_X, 76, Fonts.bold(40), CREAM);
        text(g, "长沙 · 夜间街景 · 滑块拼图", BOARD_X, 106, Fonts.plain(15), MUTED);

        String badge = view.difficulty.getTitle() + " · " + view.difficulty.getLabel();
        Font badgeFont = Fonts.bold(15);
        int badgeW = textWidth(g, badge, badgeFont) + 32;
        int badgeX = STATUS_X + STATUS_W - badgeW;
        int badgeY = 42;
        g.setColor(new Color(232, 190, 124, 30));
        g.fill(new RoundRectangle2D.Float(badgeX, badgeY, badgeW, 34, 17, 17));
        g.setColor(new Color(232, 190, 124, 110));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Float(badgeX + 0.5f, badgeY + 0.5f, badgeW - 1, 33, 17, 17));
        centerText(g, badge, badgeX + badgeW / 2, badgeY + 23, badgeFont, GOLD);
    }

    // ------------------------------------------------------------------ 拼图区

    private static void drawBoard(Graphics2D g, BoardView view, Tiles tiles) {
        // 投影
        for (int i = 6; i >= 1; i--) {
            g.setColor(new Color(0, 0, 0, 10 + i * 3));
            g.fill(new RoundRectangle2D.Float(BOARD_X - 22 - i, BOARD_Y - 22 - i + 10,
                    BOARD_SIDE + 44 + i * 2, BOARD_SIDE + 44 + i * 2, 34, 34));
        }
        // 木质外框
        g.setPaint(new LinearGradientPaint(
                new Point2D.Float(BOARD_X - 20, BOARD_Y - 20),
                new Point2D.Float(BOARD_X + BOARD_SIDE + 20, BOARD_Y + BOARD_SIDE + 20),
                new float[]{0f, 0.45f, 1f},
                new Color[]{new Color(0x8C5C32), new Color(0xA9743F), new Color(0x5E3A1B)}));
        g.fill(new RoundRectangle2D.Float(BOARD_X - 20, BOARD_Y - 20, BOARD_SIDE + 40, BOARD_SIDE + 40, 30, 30));
        g.setColor(new Color(0x3A2411));
        g.setStroke(new BasicStroke(3f));
        g.draw(new RoundRectangle2D.Float(BOARD_X - 20, BOARD_Y - 20, BOARD_SIDE + 40, BOARD_SIDE + 40, 30, 30));
        // 框内高光
        g.setColor(new Color(255, 236, 200, 60));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Float(BOARD_X - 16.5f, BOARD_Y - 16.5f, BOARD_SIDE + 33, BOARD_SIDE + 33, 26, 26));

        // 盘面底
        g.setColor(new Color(0x120E0B));
        g.fillRect(BOARD_X, BOARD_Y, BOARD_SIDE, BOARD_SIDE);

        int size = view.difficulty.getSize();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int num = view.data[r][c];
                Rectangle rect = cellRect(size, r, c);
                if (num == 0) {
                    drawEmptyCell(g, rect);
                    continue;
                }
                BufferedImage img = tiles == null ? null : tiles.scaled(num, rect.width, rect.height);
                if (img == null) {
                    g.setColor(new Color(0x4A3623));
                    g.fillRect(rect.x, rect.y, rect.width, rect.height);
                    continue;
                }
                drawTile(g, rect, img);
            }
        }

        // 外框压住碎片的外沿，让画面干净。
        g.setClip(null);
        g.setColor(new Color(0x120E0B));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(BOARD_X - 1, BOARD_Y - 1, BOARD_SIDE + 2, BOARD_SIDE + 2);

        if (view.won) {
            drawWinOverlay(g, view);
        } else if (view.showingOriginal) {
            drawOriginalOverlay(g, view);
        }
    }

    private static void drawTile(Graphics2D g, Rectangle rect, BufferedImage img) {
        int gap = rect.width > 130 ? 3 : 2;
        int x = rect.x + gap;
        int y = rect.y + gap;
        int w = rect.width - gap * 2;
        int h = rect.height - gap * 2;
        g.drawImage(img, x, y, w, h, null);
        // 斜角高光 / 阴影，做出「一块一块」的立体感。
        g.setColor(new Color(255, 255, 255, 46));
        g.drawLine(x, y, x + w - 1, y);
        g.drawLine(x, y, x, y + h - 1);
        g.setColor(new Color(0, 0, 0, 90));
        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
    }

    private static void drawEmptyCell(Graphics2D g, Rectangle rect) {
        int gap = rect.width > 130 ? 3 : 2;
        int x = rect.x + gap;
        int y = rect.y + gap;
        int w = rect.width - gap * 2;
        int h = rect.height - gap * 2;
        // 空位是「木框里的凹槽」：温暖的深棕渐变 + 上左内阴影，而不是一块死黑。
        g.setPaint(new LinearGradientPaint(
                new Point2D.Float(x, y), new Point2D.Float(x + w, y + h),
                new float[]{0f, 1f},
                new Color[]{new Color(0x3A2A1C), new Color(0x211710)}));
        g.fillRect(x, y, w, h);

        int wall = Math.max(8, w / 6);
        for (int i = 0; i < wall; i++) {
            int alpha = 46 - i * 5;
            if (alpha <= 0) {
                break;
            }
            g.setColor(new Color(0, 0, 0, alpha));
            g.drawLine(x + i, y + i, x + w - 1 - i, y + i);
            g.drawLine(x + i, y + i, x + i, y + h - 1 - i);
        }
        g.setColor(new Color(255, 236, 200, 40));
        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
    }

    // ------------------------------------------------------------------ 叠加层

    private static void drawOriginalOverlay(Graphics2D g, BoardView view) {
        Image all = Assets.read("all.png");
        if (all == null) {
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.97f));
        g.drawImage(all, BOARD_X, BOARD_Y, BOARD_SIDE, BOARD_SIDE, null);
        g.setComposite(AlphaComposite.SrcOver);

        // 淡淡的格线：一眼能看出这是「叠在盘面上的原图」，也顺便看懂目标分格。
        int size = view.difficulty.getSize();
        g.setColor(new Color(255, 255, 255, 52));
        g.setStroke(new BasicStroke(1f));
        for (int i = 1; i < size; i++) {
            int offset = (int) Math.round(i * (BOARD_SIDE / (double) size));
            g.drawLine(BOARD_X + offset, BOARD_Y, BOARD_X + offset, BOARD_Y + BOARD_SIDE);
            g.drawLine(BOARD_X, BOARD_Y + offset, BOARD_X + BOARD_SIDE, BOARD_Y + offset);
        }

        g.setColor(GOLD);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(BOARD_X + 1, BOARD_Y + 1, BOARD_SIDE - 2, BOARD_SIDE - 2);

        String hint = "查看原图 · 按住 A";
        Font f = Fonts.bold(15);
        int w = textWidth(g, hint, f) + 36;
        int x = BOARD_X + (BOARD_SIDE - w) / 2;
        int y = BOARD_Y + BOARD_SIDE - 62;
        g.setColor(new Color(0, 0, 0, 165));
        g.fill(new RoundRectangle2D.Float(x, y, w, 36, 18, 18));
        g.setColor(new Color(232, 190, 124, 120));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, 35, 18, 18));
        centerText(g, hint, BOARD_X + BOARD_SIDE / 2, y + 24, f, CREAM);
    }

    private static void drawWinOverlay(Graphics2D g, BoardView view) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(BOARD_X, BOARD_Y, BOARD_SIDE, BOARD_SIDE);

        int cardW = 460;
        int cardH = 300;
        int cardX = BOARD_X + (BOARD_SIDE - cardW) / 2;
        int cardY = BOARD_Y + (BOARD_SIDE - cardH) / 2;

        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Float(cardX, cardY + 12, cardW, cardH, 28, 28));
        g.setColor(CARD);
        g.fill(new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 28, 28));
        g.setColor(new Color(0xE0C79B));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Float(cardX + 0.7f, cardY + 0.7f, cardW - 1.4f, cardH - 1.4f, 28, 28));

        if (winImage == null) {
            winImage = Assets.read("win.png");
        }
        if (winImage != null) {
            int imgH = 92;
            int imgW = (int) Math.round(imgH * (winImage.getWidth(null) / (double) winImage.getHeight(null)));
            int imgX = cardX + (cardW - imgW) / 2;
            int imgY = cardY + 28;
            // win.png 自带浅色底，垫一张白色圆角贴纸，看起来才不是「截图贴上去的」。
            g.setColor(new Color(0, 0, 0, 26));
            g.fill(new RoundRectangle2D.Float(imgX - 16, imgY - 10 + 3, imgW + 32, imgH + 20, 14, 14));
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(imgX - 16, imgY - 10, imgW + 32, imgH + 20, 14, 14));
            g.setColor(new Color(0xE8BE7C));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new RoundRectangle2D.Float(imgX - 15.5f, imgY - 9.5f, imgW + 31, imgH + 19, 14, 14));
            g.drawImage(winImage, imgX, imgY, imgW, imgH, null);
        }

        centerText(g, "拼好了！", cardX + cardW / 2, cardY + 186, Fonts.bold(30), INK);
        centerText(g, view.resultText(), cardX + cardW / 2, cardY + 216, Fonts.plain(17), new Color(0x6B5A46));
        centerText(g, view.recordText(), cardX + cardW / 2, cardY + 248,
                Fonts.bold(15), view.newRecordSteps || view.newRecordTime ? GOLD_DEEP : MUTED);
        centerText(g, "R 再来一局 · 1 / 2 / 3 换难度", cardX + cardW / 2, cardY + 278,
                Fonts.plain(13), new Color(0xA08D76));
    }

    // ------------------------------------------------------------------ 状态栏

    private static void drawStatusBar(Graphics2D g, BoardView view) {
        g.setColor(new Color(246, 234, 215, 16));
        g.fill(new RoundRectangle2D.Float(STATUS_X, STATUS_Y, STATUS_W, STATUS_H, 22, 22));
        g.setColor(new Color(232, 190, 124, 56));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Float(STATUS_X + 0.5f, STATUS_Y + 0.5f, STATUS_W - 1, STATUS_H - 1, 22, 22));

        int colX = STATUS_X + STATUS_PAD;
        int colStep = 190;
        metric(g, colX, "步数", String.valueOf(view.steps), Fonts.bold(26));
        metric(g, colX + colStep, "用时", formatTime(view.elapsedMs), Fonts.bold(26));
        metric(g, colX + colStep * 2, "难度", view.difficulty.getTitle() + " · " + view.difficulty.getLabel(),
                Fonts.bold(20));
        String best = view.best.hasSteps() || view.best.hasTime()
                ? view.best.stepsText() + " · " + view.best.timeText()
                : "暂无纪录";
        metric(g, colX + colStep * 3, "最佳纪录", best, Fonts.bold(18));

        g.setColor(new Color(246, 234, 215, 28));
        g.drawLine(STATUS_X + STATUS_PAD, STATUS_Y + 82, STATUS_X + STATUS_W - STATUS_PAD, STATUS_Y + 82);

        drawButton(g, undoButton(), "撤销  Ctrl+Z", view.canUndo);
        drawButton(g, restartButton(), "重开  R", true);
        rightText(g, "1 / 2 / 3 换难度 · 按住 A 看原图",
                STATUS_X + STATUS_W - STATUS_PAD, STATUS_Y + 114, Fonts.plain(13), MUTED);
    }

    private static void metric(Graphics2D g, int x, String label, String value, Font valueFont) {
        text(g, label, x, STATUS_Y + 32, Fonts.plain(13), MUTED);
        text(g, value, x, STATUS_Y + 68, valueFont, CREAM);
    }

    private static void drawButton(Graphics2D g, Rectangle rect, String label, boolean enabled) {
        Color fill = enabled ? new Color(232, 190, 124, 34) : new Color(246, 234, 215, 10);
        Color border = enabled ? new Color(232, 190, 124, 130) : new Color(246, 234, 215, 30);
        Color text = enabled ? CREAM : new Color(0x6E6053);
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height, 12, 12));
        g.setColor(border);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Float(rect.x + 0.5f, rect.y + 0.5f,
                rect.width - 1, rect.height - 1, 12, 12));
        centerText(g, label, rect.x + rect.width / 2, rect.y + 23, Fonts.bold(13), text);
    }

    // ------------------------------------------------------------------ 文字小工具

    private static void text(Graphics2D g, String s, int x, int baseline, Font f, Color c) {
        UiText.text(g, s, x, baseline, f, c);
    }

    private static void centerText(Graphics2D g, String s, int centerX, int baseline, Font f, Color c) {
        UiText.center(g, s, centerX, baseline, f, c);
    }

    private static void rightText(Graphics2D g, String s, int rightX, int baseline, Font f, Color c) {
        UiText.right(g, s, rightX, baseline, f, c);
    }

    private static int textWidth(Graphics2D g, String s, Font f) {
        return UiText.width(g, s, f);
    }
}
