import com.wjh.ui.Solvability;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 可解性判据的穷举验证（无第三方依赖，直接 {@code java SolvabilityTest} 跑）。
 * <p>
 * 做法：从目标局面出发做 BFS，得到「真正能走到的局面集合」；再把该尺寸的**全部排列**枚举一遍，
 * 用 {@link Solvability#isSolvable(int[], int)} 判断，两边逐项比对。
 * <ul>
 *     <li>3×3：全排列 9! = 362880，宽度 3（奇数）→ 验证「逆序数为偶数」；</li>
 *     <li>2×4：全排列 8! = 40320，宽度 4（偶数）→ 顺带验证偶数宽度那套「逆序数 + 空位行号」判据。</li>
 * </ul>
 * 另外对 5×5（宽度 5，奇数）做随机游走抽样：从已解局面走 200 步，判据必须恒为可解；
 * 再把两个非空块对调，判据必须恒为不可解。
 * 有不一致就以非 0 退出码结束。
 */
public final class SolvabilityTest {

    private SolvabilityTest() {
    }

    public static void main(String[] args) {
        long begin = System.currentTimeMillis();
        int failures = 0;

        failures += verify(3, 3);
        failures += verify(2, 4);
        failures += walkSample(5, 5);

        long cost = System.currentTimeMillis() - begin;
        System.out.println();
        System.out.println("总耗时：" + cost + " ms");
        if (failures == 0) {
            System.out.println("结论：全部一致，判据通过。");
        } else {
            System.out.println("结论：发现 " + failures + " 处不一致！");
            System.exit(1);
        }
    }

    /** 对一个 rows×cols 盘面做全状态空间比对，返回不一致的数量。 */
    private static int verify(int rows, int cols) {
        int cells = rows * cols;
        Set<Long> reachable = bfs(rows, cols);
        int total = 0;
        int judgedSolvable = 0;
        int mismatch = 0;

        int[] perm = new int[cells];
        for (int i = 0; i < cells; i++) {
            perm[i] = i;
        }
        do {
            total++;
            boolean judged = Solvability.isSolvable(perm, cols);
            boolean actual = reachable.contains(encode(perm));
            if (judged) {
                judgedSolvable++;
            }
            if (judged != actual) {
                mismatch++;
                if (mismatch <= 5) {
                    System.out.println("  不一致：" + java.util.Arrays.toString(perm)
                            + " 判据=" + judged + " 可达=" + actual);
                }
            }
        } while (nextPermutation(perm));

        System.out.println("=== " + rows + "×" + cols + "（宽度 " + cols
                + (cols % 2 == 1 ? "，奇数）" : "，偶数）") + " ===");
        System.out.println("全排列数：      " + total);
        System.out.println("BFS 可达局面：  " + reachable.size());
        System.out.println("判据判为可解：  " + judgedSolvable);
        System.out.println("不一致：        " + mismatch);
        System.out.println();
        return mismatch;
    }

    /** 5×5 抽样：随机游走得到的局面必须判可解，交换两个块后必须判不可解。 */
    private static int walkSample(int rows, int cols) {
        int cells = rows * cols;
        Random random = new Random(20241029L);
        int walks = 2000;
        int badWalk = 0;
        int badSwap = 0;
        for (int i = 0; i < walks; i++) {
            int[] perm = solved(cells);
            int blank = cells - 1;
            for (int step = 0; step < 200; step++) {
                int[] neighbors = neighbors(blank, rows, cols);
                int target = neighbors[random.nextInt(neighbors.length)];
                perm[blank] = perm[target];
                perm[target] = 0;
                blank = target;
            }
            if (!Solvability.isSolvable(perm, cols)) {
                badWalk++;
            }
            // 交换两个非空块：必然跨集合。
            int a = -1;
            int b = -1;
            for (int k = 0; k < cells && b < 0; k++) {
                if (perm[k] == 0) {
                    continue;
                }
                if (a < 0) {
                    a = k;
                } else {
                    b = k;
                }
            }
            int tmp = perm[a];
            perm[a] = perm[b];
            perm[b] = tmp;
            if (Solvability.isSolvable(perm, cols)) {
                badSwap++;
            }
        }
        System.out.println("=== " + rows + "×" + cols + "（宽度 " + cols + "，奇数）随机游走抽样 ===");
        System.out.println("游走样本数：    " + walks + "（每样本 200 步）");
        System.out.println("误判不可解：    " + badWalk);
        System.out.println("交换后误判可解：" + badSwap);
        System.out.println();
        return badWalk + badSwap;
    }

    /** 从目标局面 BFS，返回所有可达局面的编码。 */
    private static Set<Long> bfs(int rows, int cols) {
        int cells = rows * cols;
        int[] start = solved(cells);
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        seen.add(encode(start));
        queue.add(start);
        while (!queue.isEmpty()) {
            int[] state = queue.poll();
            int blank = 0;
            for (int i = 0; i < cells; i++) {
                if (state[i] == 0) {
                    blank = i;
                    break;
                }
            }
            for (int target : neighbors(blank, rows, cols)) {
                int[] next = state.clone();
                next[blank] = next[target];
                next[target] = 0;
                if (seen.add(encode(next))) {
                    queue.add(next);
                }
            }
        }
        return seen;
    }

    private static int[] neighbors(int blank, int rows, int cols) {
        int row = blank / cols;
        int col = blank % cols;
        List<Integer> list = new ArrayList<>(4);
        if (row > 0) {
            list.add(blank - cols);
        }
        if (row < rows - 1) {
            list.add(blank + cols);
        }
        if (col > 0) {
            list.add(blank - 1);
        }
        if (col < cols - 1) {
            list.add(blank + 1);
        }
        int[] result = new int[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    private static int[] solved(int cells) {
        int[] state = new int[cells];
        for (int i = 0; i < cells - 1; i++) {
            state[i] = i + 1;
        }
        state[cells - 1] = 0;
        return state;
    }

    /** 每个格子 4 bit，最大 25 格 = 100 bit，所以用两个 long 拼不划算 —— 这里只用于 ≤ 16 格。 */
    private static long encode(int[] state) {
        long code = 0;
        for (int value : state) {
            code = (code << 4) | value;
        }
        return code;
    }

    /** 下一个字典序排列，返回 false 表示已经枚举完。 */
    private static boolean nextPermutation(int[] arr) {
        int i = arr.length - 2;
        while (i >= 0 && arr[i] >= arr[i + 1]) {
            i--;
        }
        if (i < 0) {
            return false;
        }
        int j = arr.length - 1;
        while (arr[j] <= arr[i]) {
            j--;
        }
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
        for (int l = i + 1, r = arr.length - 1; l < r; l++, r--) {
            tmp = arr[l];
            arr[l] = arr[r];
            arr[r] = tmp;
        }
        return true;
    }
}
