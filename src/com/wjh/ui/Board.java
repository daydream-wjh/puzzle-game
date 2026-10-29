package com.wjh.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * 滑块拼图的数据模型：盘面、空位坐标、移动、撤销栈和胜负判定。
 * <p>
 * 和界面完全解耦，单元测试与离屏截图都直接构造它，不需要窗口。
 * 移动方向直接复用 AWT 的方向键键码（{@link #UP} 等），省掉一层映射。
 */
public class Board {

    /** 上（空位往上挪，即把上面的方块推下来）。 */
    public static final int UP = 38;
    /** 下。 */
    public static final int DOWN = 40;
    /** 左。 */
    public static final int LEFT = 37;
    /** 右。 */
    public static final int RIGHT = 39;

    /** 撤销栈上限：只保留最近 50 步。 */
    public static final int MAX_HISTORY = 50;

    private final Difficulty difficulty;
    private final int size;
    private final int[][] data;
    /** 记录走过的方向，撤销时反向再走一步。 */
    private final Deque<Integer> history = new ArrayDeque<>();
    private int blankRow;
    private int blankCol;

    public Board(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.size = difficulty.getSize();
        this.data = new int[size][size];
        reset();
    }

    /** 恢复成目标局面（1,2,...,n²-1，0 在右下角），并清空历史。 */
    public void reset() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                data[r][c] = r * size + c + 1;
            }
        }
        data[size - 1][size - 1] = 0;
        blankRow = size - 1;
        blankCol = size - 1;
        history.clear();
    }

    /** 直接铺一个局面（截图 / 测试用），非法输入抛 IllegalArgumentException。 */
    public void setData(int[][] layout) {
        if (layout == null || layout.length != size) {
            throw new IllegalArgumentException("盘面行数必须是 " + size);
        }
        boolean foundBlank = false;
        for (int r = 0; r < size; r++) {
            if (layout[r] == null || layout[r].length != size) {
                throw new IllegalArgumentException("盘面列数必须是 " + size);
            }
            for (int c = 0; c < size; c++) {
                int v = layout[r][c];
                if (v < 0 || v >= difficulty.getCellCount()) {
                    throw new IllegalArgumentException("非法编号：" + v);
                }
                if (v == 0) {
                    blankRow = r;
                    blankCol = c;
                    foundBlank = true;
                }
                data[r][c] = v;
            }
        }
        if (!foundBlank) {
            throw new IllegalArgumentException("盘面必须有一个空位 0");
        }
        history.clear();
    }

    /** 复制一份盘面，给渲染器和外部读。 */
    public int[][] snapshot() {
        int[][] copy = new int[size][size];
        for (int r = 0; r < size; r++) {
            copy[r] = data[r].clone();
        }
        return copy;
    }

    public int get(int row, int col) {
        return data[row][col];
    }

    public int getSize() {
        return size;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getBlankRow() {
        return blankRow;
    }

    public int getBlankCol() {
        return blankCol;
    }

    /** 指定方向能不能走。 */
    public boolean canMove(int direction) {
        switch (direction) {
            case UP:
                return blankRow > 0;
            case DOWN:
                return blankRow < size - 1;
            case LEFT:
                return blankCol > 0;
            case RIGHT:
                return blankCol < size - 1;
            default:
                return false;
        }
    }

    /** 空位朝 direction 挪一格（等价于把那个方块推进空位）。走成了返回 true。 */
    public boolean move(int direction) {
        if (!canMove(direction)) {
            return false;
        }
        int nr = blankRow;
        int nc = blankCol;
        switch (direction) {
            case UP:
                nr--;
                break;
            case DOWN:
                nr++;
                break;
            case LEFT:
                nc--;
                break;
            case RIGHT:
                nc++;
                break;
            default:
                return false;
        }
        data[blankRow][blankCol] = data[nr][nc];
        data[nr][nc] = 0;
        blankRow = nr;
        blankCol = nc;
        history.push(direction);
        while (history.size() > MAX_HISTORY) {
            history.removeLast();
        }
        return true;
    }

    /** 撤销一步，成功返回 true。 */
    public boolean undo() {
        Integer last = history.poll();
        if (last == null) {
            return false;
        }
        // 反方向再走一步就回到上一步的盘面。
        move(opposite(last));
        // move() 会把它压回栈，这里去掉，否则撤销会把自己吃掉。
        history.poll();
        return true;
    }

    public int getHistorySize() {
        return history.size();
    }

    /** 目标局面。 */
    public static int[][] solvedLayout(int size) {
        int[][] layout = new int[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                layout[r][c] = r * size + c + 1;
            }
        }
        layout[size - 1][size - 1] = 0;
        return layout;
    }

    /** 是否已经拼好。 */
    public boolean solved() {
        int expect = 1;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                boolean lastCell = r == size - 1 && c == size - 1;
                int want = lastCell ? 0 : expect;
                if (data[r][c] != want) {
                    return false;
                }
                if (!lastCell) {
                    expect++;
                }
            }
        }
        return true;
    }

    /**
     * 洗牌：Fisher–Yates 打乱全部编号，再用 {@link Solvability} 校验可解性，
     * 不可解就重洗（宽度奇偶不同，判据由 Solvability 内部适配）。
     */
    public void shuffle(Random random) {
        int total = difficulty.getCellCount();
        int[] flat = new int[total];
        for (int i = 0; i < total; i++) {
            flat[i] = i;
        }
        do {
            shuffleArray(flat, random);
        } while (!Solvability.isSolvable(flat, size) || isSolvedFlat(flat));
        int[][] layout = new int[size][size];
        for (int i = 0; i < total; i++) {
            layout[i / size][i % size] = flat[i];
        }
        setData(layout);
    }

    /** 从已解局面出发随机走若干步，得到一个「一定可达、看起来自然」的中间盘面（截图用）。 */
    public void scramble(int moves, Random random) {
        reset();
        int[] dirs = {UP, DOWN, LEFT, RIGHT};
        int done = 0;
        int guard = 0;
        while (done < moves && guard < moves * 20) {
            guard++;
            int dir = dirs[random.nextInt(dirs.length)];
            if (move(dir)) {
                done++;
            }
        }
        // 打乱只是布景，不算玩家走过的步数。
        history.clear();
    }

    /** Fisher–Yates：从后往前，每一步和 [0, i] 里随机一位交换，每种排列等概率。 */
    public static void shuffleArray(int[] arr, Random rd) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rd.nextInt(i + 1);
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }

    private static boolean isSolvedFlat(int[] flat) {
        for (int i = 0; i < flat.length - 1; i++) {
            if (flat[i] != i + 1) {
                return false;
            }
        }
        return flat.length > 0 && flat[flat.length - 1] == 0;
    }

    public static int opposite(int direction) {
        switch (direction) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            default:
                return direction;
        }
    }
}
