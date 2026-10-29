package com.wjh.ui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * 拼图碎片的来源：把「编号 → 图片」这件事收在一处。
 * <ul>
 *     <li>4×4：直接用现成的 {@code img/CS (1..16).png} 十六张切片（每张 105×105）；</li>
 *     <li>3×3 / 5×5：素材里没有 9 张和 24 张切片，于是拿 {@code img/all.png}（420×420）
 *     用 {@code BufferedImage.getSubimage} 按格切，不新增图片文件。</li>
 * </ul>
 * 缩放结果按「编号 + 目标尺寸」缓存，拖动/重绘时不会反复做插值运算。
 */
public final class Tiles {

    /** 4×4 切片边长。 */
    public static final int SLICE_SIZE = 105;

    private final Difficulty difficulty;
    private final BufferedImage all;
    private final Image[] slices = new Image[16];
    private final Map<Long, BufferedImage> cache = new HashMap<>();

    public Tiles(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.all = Assets.read("all.png");
        if (difficulty == Difficulty.CLASSIC) {
            for (int i = 0; i < slices.length; i++) {
                slices[i] = new javax.swing.ImageIcon(Assets.file("CS (" + (i + 1) + ").png").getPath()).getImage();
            }
        }
    }

    /** 取编号对应的原尺寸底图。 */
    public Image base(int num) {
        if (num <= 0) {
            return null;
        }
        if (difficulty == Difficulty.CLASSIC && num <= slices.length) {
            return slices[num - 1];
        }
        if (all == null) {
            return null;
        }
        int n = difficulty.getSize();
        int cellW = all.getWidth() / n;
        int cellH = all.getHeight() / n;
        int row = (num - 1) / n;
        int col = (num - 1) % n;
        // 最后一行/列吃下整除余数，避免右边和下边漏一条边。
        int x = col * cellW;
        int y = row * cellH;
        int w = (col == n - 1) ? all.getWidth() - x : cellW;
        int h = (row == n - 1) ? all.getHeight() - y : cellH;
        return all.getSubimage(x, y, w, h);
    }

    /** 取缩放到指定像素尺寸的碎片（带缓存）。 */
    public BufferedImage scaled(int num, int width, int height) {
        if (num <= 0 || width <= 0 || height <= 0) {
            return null;
        }
        long key = ((long) num << 40) ^ ((long) width << 20) ^ height;
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Image base = base(num);
        if (base == null) {
            return null;
        }
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(base, 0, 0, width, height, null);
        g.dispose();
        cache.put(key, out);
        return out;
    }

    /** 清空缩放缓存（换难度时调用，避免缓存越滚越大）。 */
    public void clearCache() {
        cache.clear();
    }
}
