package com.wjh.ui;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

/**
 * 拼图主窗体：键盘 / 鼠标输入 + 计时 + 纪录 + 撤销，画面全部交给 {@link BoardRenderer}。
 * <p>
 * 快捷键：
 * <ul>
 *     <li>方向键：把空位朝这个方向挪（等价于把那个方块推进空位）；</li>
 *     <li>鼠标点空格旁边的方块：同样是移动它；</li>
 *     <li>{@code Ctrl+Z} 或状态栏「撤销」：回退一步（最多 50 步）；</li>
 *     <li>{@code R} 或状态栏「重开」：同一难度重新洗牌；</li>
 *     <li>{@code 1 / 2 / 3}：切 3×3 / 4×4 / 5×5；</li>
 *     <li>按住 {@code A}：叠加原图，松开恢复；</li>
 *     <li>{@code W}：一键通关（调试用后门）。</li>
 * </ul>
 */
public class GameJFrame extends JFrame implements KeyListener, ActionListener {

    /** 计时器刷新间隔（毫秒）。 */
    private static final int TICK_MS = 200;

    private final Random random = new Random();
    private final BoardView view = new BoardView();
    private final GamePanel panel;
    private final Timer clock;

    private Difficulty difficulty;
    private Board board;
    private Tiles tiles;
    private Records.Best best = Records.Best.EMPTY;

    /** 当前步数（撤销会同步减回去）。 */
    private int steps;
    /** 第一次移动的时刻，-1 表示还没开始计时。 */
    private long startedAt = -1;
    /** 胜利时冻结的用时。 */
    private long frozenMillis;
    /** 胜利时是否刷新了纪录，只用于战绩卡片。 */
    private boolean newRecordSteps;
    private boolean newRecordTime;

    // 菜单项
    private final JMenuItem replayItem = new JMenuItem("重新游戏");
    private final JMenuItem reLoginItem = new JMenuItem("重新登录");
    private final JMenuItem closeItem = new JMenuItem("关闭游戏");
    private final JMenuItem accountItem = new JMenuItem("公众号");
    private final JRadioButtonMenuItem easyItem = new JRadioButtonMenuItem("3 × 3 入门");
    private final JRadioButtonMenuItem classicItem = new JRadioButtonMenuItem("4 × 4 经典");
    private final JRadioButtonMenuItem hardItem = new JRadioButtonMenuItem("5 × 5 困难");

    public GameJFrame() {
        this(Difficulty.CLASSIC);
    }

    public GameJFrame(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.board = new Board(difficulty);
        this.tiles = new Tiles(difficulty);
        this.panel = new GamePanel(view, tiles);
        this.clock = new Timer(TICK_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                view.elapsedMs = elapsed();
                panel.repaint();
            }
        });

        initJFrame();
        initJMenuBar();
        newRound();

        this.setVisible(true);
        fitToContent();
        this.toFront();
        this.requestFocus();
    }

    // ------------------------------------------------------------------ 初始化

    private void initJFrame() {
        this.setTitle("拼图单机版");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.setLayout(null);
        this.setContentPane(panel);
        this.setSize(BoardRenderer.WIDTH, BoardRenderer.HEIGHT);
        this.setLocationRelativeTo(null);
        this.addKeyListener(this);
        panel.addKeyListener(this);
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    /** 把窗口调到「内容区正好 880×880」，这样绘制坐标和窗口像素一一对应。 */
    private void fitToContent() {
        Dimension content = getContentPane().getSize();
        setSize(getWidth() + (BoardRenderer.WIDTH - content.width),
                getHeight() + (BoardRenderer.HEIGHT - content.height));
        setLocationRelativeTo(null);
    }

    private void initJMenuBar() {
        JMenuBar jMenuBar = new JMenuBar();
        JMenu functionJMenu = new JMenu("功能");
        JMenu levelJMenu = new JMenu("难度");
        JMenu aboutJMenu = new JMenu("关于我们");

        functionJMenu.add(replayItem);
        functionJMenu.add(reLoginItem);
        functionJMenu.add(closeItem);

        ButtonGroup group = new ButtonGroup();
        group.add(easyItem);
        group.add(classicItem);
        group.add(hardItem);
        levelJMenu.add(easyItem);
        levelJMenu.add(classicItem);
        levelJMenu.add(hardItem);

        aboutJMenu.add(accountItem);

        replayItem.addActionListener(this);
        reLoginItem.addActionListener(this);
        closeItem.addActionListener(this);
        accountItem.addActionListener(this);
        easyItem.addActionListener(this);
        classicItem.addActionListener(this);
        hardItem.addActionListener(this);

        jMenuBar.add(functionJMenu);
        jMenuBar.add(levelJMenu);
        jMenuBar.add(aboutJMenu);
        this.setJMenuBar(jMenuBar);
        syncLevelMenu();
    }

    private void syncLevelMenu() {
        easyItem.setSelected(difficulty == Difficulty.EASY);
        classicItem.setSelected(difficulty == Difficulty.CLASSIC);
        hardItem.setSelected(difficulty == Difficulty.HARD);
    }

    // ------------------------------------------------------------------ 一局的生命周期

    /** 重新洗牌，步数与计时归零，纪录重新读盘。 */
    private void newRound() {
        clock.stop();
        startedAt = -1;
        frozenMillis = 0;
        steps = 0;
        newRecordSteps = false;
        newRecordTime = false;
        // 胜利遮罩 / 原图叠加层都是上一局的状态，重开必须清掉。
        view.won = false;
        view.showingOriginal = false;
        board.reset();
        board.shuffle(random);
        best = Records.load(difficulty);
        refresh();
    }

    /** 把模型状态同步进渲染视图并重绘。 */
    private void refresh() {
        view.difficulty = difficulty;
        view.data = board.snapshot();
        view.steps = steps;
        view.canUndo = board.getHistorySize() > 0;
        view.best = best;
        view.newRecordSteps = newRecordSteps;
        view.newRecordTime = newRecordTime;
        view.elapsedMs = elapsed();
        syncLevelMenu();
        panel.repaint();
    }

    private void switchDifficulty(Difficulty target) {
        if (target == difficulty) {
            return;
        }
        difficulty = target;
        board = new Board(target);
        tiles = new Tiles(target);
        panel.setTiles(tiles);
        view.difficulty = target;
        newRound();
    }

    private void startClock() {
        if (startedAt < 0 && !view.won) {
            startedAt = System.currentTimeMillis();
            clock.start();
        }
    }

    private long elapsed() {
        if (view.won) {
            return frozenMillis;
        }
        if (startedAt < 0) {
            return 0;
        }
        return System.currentTimeMillis() - startedAt;
    }

    /** 胜利：停表、结算纪录、弹战绩卡片。 */
    private void finish() {
        if (view.won) {
            return;
        }
        view.won = true;
        view.showingOriginal = false;
        frozenMillis = startedAt < 0 ? 0 : System.currentTimeMillis() - startedAt;
        clock.stop();
        int seconds = (int) Math.round(frozenMillis / 1000.0);
        Records.Result result = Records.submit(difficulty, steps, seconds);
        newRecordSteps = result.isNewSteps();
        newRecordTime = result.isNewTime();
        best = result.getAfter();
        view.elapsedMs = frozenMillis;
        refresh();
    }

    // ------------------------------------------------------------------ 移动 / 撤销

    /** 走一步：成功返回 true，并处理计时、胜利判定。 */
    private boolean tryMove(int direction) {
        if (view.won) {
            return false;
        }
        if (!board.move(direction)) {
            return false;
        }
        steps++;
        startClock();
        if (board.solved()) {
            finish();
        } else {
            refresh();
        }
        return true;
    }

    /** 撤销一步，步数同步回退。 */
    private void undo() {
        if (view.won) {
            return;
        }
        if (!board.undo()) {
            return;
        }
        if (steps > 0) {
            steps--;
        }
        refresh();
    }

    // ------------------------------------------------------------------ 输入

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // 按住 A 看原图：只在按下的那一刻切换，避免重复事件刷屏。
        if (e.getKeyCode() == KeyEvent.VK_A && !view.won) {
            view.showingOriginal = true;
            panel.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_A) {
            view.showingOriginal = false;
            panel.repaint();
            return;
        }
        if ((e.isControlDown() || e.isMetaDown()) && code == KeyEvent.VK_Z) {
            undo();
            return;
        }
        if (view.won && code != KeyEvent.VK_W && code != KeyEvent.VK_R
                && code != KeyEvent.VK_1 && code != KeyEvent.VK_2 && code != KeyEvent.VK_3) {
            return;
        }
        switch (code) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_DOWN:
                tryMove(code);
                break;
            case KeyEvent.VK_R:
                newRound();
                break;
            case KeyEvent.VK_1:
                switchDifficulty(Difficulty.EASY);
                break;
            case KeyEvent.VK_2:
                switchDifficulty(Difficulty.CLASSIC);
                break;
            case KeyEvent.VK_3:
                switchDifficulty(Difficulty.HARD);
                break;
            case KeyEvent.VK_W:
                // TODO: 发布前删除调试后门
                debugSolve();
                break;
            default:
                break;
        }
    }

    /** 调试后门：直接把盘面摆成胜利状态，不结算纪录。 */
    private void debugSolve() {
        long used = elapsed();
        board.setData(Board.solvedLayout(difficulty.getSize()));
        view.won = true;
        frozenMillis = used;
        clock.stop();
        newRecordSteps = false;
        newRecordTime = false;
        refresh();
    }

    /** 鼠标：点状态栏按钮，或点空格旁边的方块。 */
    private void handleClick(int x, int y) {
        if (BoardRenderer.undoButton().contains(x, y)) {
            undo();
            return;
        }
        if (BoardRenderer.restartButton().contains(x, y)) {
            newRound();
            return;
        }
        if (view.won) {
            return;
        }
        int[] cell = BoardRenderer.cellAt(difficulty.getSize(), x, y);
        if (cell == null) {
            return;
        }
        int row = cell[0];
        int col = cell[1];
        int blankRow = board.getBlankRow();
        int blankCol = board.getBlankCol();
        if (row == blankRow && col == blankCol) {
            return;
        }
        // 点到的方块和空位相邻 → 把空位朝它的方向挪一格。
        int direction;
        if (row == blankRow && col == blankCol - 1) {
            direction = Board.LEFT;
        } else if (row == blankRow && col == blankCol + 1) {
            direction = Board.RIGHT;
        } else if (col == blankCol && row == blankRow - 1) {
            direction = Board.UP;
        } else if (col == blankCol && row == blankRow + 1) {
            direction = Board.DOWN;
        } else {
            return;
        }
        tryMove(direction);
    }

    // ------------------------------------------------------------------ 菜单

    @Override
    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource();
        if (obj == replayItem) {
            newRound();
        } else if (obj == reLoginItem) {
            this.dispose();
            new LoginJFrame();
        } else if (obj == closeItem) {
            System.exit(0);
        } else if (obj == accountItem) {
            JDialog jDialog = new JDialog();
            JLabel jLabel = new JLabel(new javax.swing.ImageIcon("img/wy.png"));
            jLabel.setBounds(0, 0, 720, 540);
            jDialog.getContentPane().add(jLabel);
            jDialog.setSize(800, 600);
            jDialog.setAlwaysOnTop(true);
            jDialog.setTitle("about my girl");
            jDialog.setLocationRelativeTo(null);
            jDialog.setModal(true);
            jDialog.setVisible(true);
        } else if (obj == easyItem) {
            switchDifficulty(Difficulty.EASY);
        } else if (obj == classicItem) {
            switchDifficulty(Difficulty.CLASSIC);
        } else if (obj == hardItem) {
            switchDifficulty(Difficulty.HARD);
        }
    }

    // ------------------------------------------------------------------ 兼容旧接口

    /** 是否已经拼好（保留给旧调用方）。 */
    public boolean victory() {
        return view.won || board.solved();
    }

    /** 旧的 4×4 判据入口，转交给 {@link Solvability}。 */
    public static boolean isSolvable(int[] tiles) {
        return Solvability.isSolvable(tiles, 4);
    }

    /** 旧的带宽度判据入口，转交给 {@link Solvability}（奇数宽度也支持了）。 */
    public static boolean isSolvable(int[] tiles, int width) {
        return Solvability.isSolvable(tiles, width);
    }

    /** 旧的洗牌入口，转交给 {@link Board#shuffleArray}。 */
    public static void shuffle(int[] arr, Random rd) {
        Board.shuffleArray(arr, rd);
    }
}
