package com.wjh.ui;

/**
 * 滑块局面的可解性判据。
 * <p>
 * 数组里 {@code 0} 表示空位，其余是 1..n²-1 的编号，按「行优先」铺进 width 列的网格。
 * 判据和宽度奇偶有关，不能一套公式通吃：
 * <ul>
 *     <li><b>奇数宽度</b>（3×3、5×5）：左右移动不改变逆序数；上下移动时被跳过的方块数是
 *     {@code width - 1}（偶数），逆序数奇偶同样不变。目标局面逆序数为 0，所以
 *     <b>可解 ⇔ 逆序数为偶数</b>。</li>
 *     <li><b>偶数宽度</b>（4×4、2×4）：上下移动会让逆序数奇偶翻转一次，同时空位的
 *     「自底向上行号」也翻转一次，两者之和的奇偶不变。目标局面逆序数 0、空位在最底行
 *     （rowFromBottom = 1），和是 1（奇数），所以
 *     <b>可解 ⇔ (逆序数 + 空位自底向上行号) 为奇数</b>。</li>
 * </ul>
 * 两个分支都由 {@code tools/SolvabilityTest} 用 BFS 全状态空间比对过：
 * 3×3 全排列 362880 个、2×4 全排列 40320 个，判据与可达性 0 分歧。
 */
public final class Solvability {

    private Solvability() {
    }

    /** 按数组长度和宽度推断盘面形状后判断。 */
    public static boolean isSolvable(int[] tiles, int width) {
        if (tiles == null || width <= 0 || tiles.length == 0 || tiles.length % width != 0) {
            return false;
        }
        int blankIndex = -1;
        int inversions = 0;
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i] == 0) {
                blankIndex = i;
            }
        }
        if (blankIndex < 0) {
            return false;
        }
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i] == 0) {
                continue;
            }
            for (int j = i + 1; j < tiles.length; j++) {
                if (tiles[j] != 0 && tiles[i] > tiles[j]) {
                    inversions++;
                }
            }
        }
        if (width % 2 == 1) {
            // 奇数宽度：只看逆序数奇偶。
            return inversions % 2 == 0;
        }
        // 偶数宽度：逆序数 + 空位自底向上行号（1-based）为奇数。
        int rows = tiles.length / width;
        int rowFromBottom = rows - blankIndex / width;
        return (inversions + rowFromBottom) % 2 == 1;
    }

    /** 二维盘面版本，按列数取宽度。 */
    public static boolean isSolvable(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0] == null) {
            return false;
        }
        int size = grid[0].length;
        int[] flat = new int[grid.length * size];
        for (int r = 0; r < grid.length; r++) {
            if (grid[r] == null || grid[r].length != size) {
                return false;
            }
            System.arraycopy(grid[r], 0, flat, r * size, size);
        }
        return isSolvable(flat, size);
    }
}
