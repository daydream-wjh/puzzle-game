package com.wjh.ui;

/**
 * 难度档位：3×3 / 4×4 / 5×5。
 * <p>
 * 4×4 用 {@code img/CS (1..16).png} 十六张现成切片；3×3 与 5×5 的切片数量对不上
 * （需要 9 / 24 张），所以直接拿 {@code img/all.png} 整图按格切（{@code getSubimage}），
 * 不新增任何图片文件。
 */
public enum Difficulty {

    /** 入门：3×3，8 个滑块，奇数宽度。 */
    EASY(3, "入门", "3 × 3"),
    /** 经典：4×4，15 个滑块，偶数宽度，素材用 16 张切片。 */
    CLASSIC(4, "经典", "4 × 4"),
    /** 困难：5×5，24 个滑块，奇数宽度。 */
    HARD(5, "困难", "5 × 5");

    private final int size;
    private final String label;
    private final String title;

    Difficulty(int size, String label, String title) {
        this.size = size;
        this.label = label;
        this.title = title;
    }

    /** 边长（列数 = 行数）。 */
    public int getSize() {
        return size;
    }

    /** 中文档位名，如「经典」。 */
    public String getLabel() {
        return label;
    }

    /** 展示用规格，如「4 × 4」。 */
    public String getTitle() {
        return title;
    }

    /** 每档的记录在 records.properties 里的前缀，如 {@code 4.}。 */
    public String getRecordPrefix() {
        return size + ".";
    }

    /** 总格数（含空位）。 */
    public int getCellCount() {
        return size * size;
    }

    /** 按边长找档位，找不到退回经典 4×4。 */
    public static Difficulty ofSize(int size) {
        for (Difficulty d : values()) {
            if (d.size == size) {
                return d;
            }
        }
        return CLASSIC;
    }
}
