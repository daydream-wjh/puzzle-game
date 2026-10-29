import com.wjh.ui.Board;
import com.wjh.ui.BoardRenderer;
import com.wjh.ui.BoardView;
import com.wjh.ui.Difficulty;
import com.wjh.ui.GamePanel;
import com.wjh.ui.LoginPanel;
import com.wjh.ui.Records;
import com.wjh.ui.Tiles;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

/**
 * README 截图生成器：把 {@link BoardRenderer} 画出来的画面离屏渲染成 PNG。
 * <p>
 * 关键点是不去「手画示意图」，而是复用真实界面用的那个 {@link GamePanel}：
 * 面板 {@code setSize(880,880)} 后直接 {@code paint(g)} 到 {@link BufferedImage} 上，
 * 所以截图和玩家看到的界面永远一致。渲染时按 2 倍超采样再缩回 1 倍，文字边缘更干净。
 * <p>
 * 运行（仓库根目录）：{@code java -cp /tmp/pg Snapshot}
 */
public final class Snapshot {

    /** 超采样倍数：2 倍渲染 → 缩回 1 倍，抗锯齿更细腻。 */
    private static final int SUPERSAMPLE = 2;

    private Snapshot() {
    }

    public static void main(String[] args) throws IOException {
        // 这台机器可能没有图形会话，强制离屏；JPanel 不依赖窗口也能画。
        System.setProperty("java.awt.headless", "true");
        Files.createDirectories(Paths.get("docs"));

        // 01 登录页
        LoginPanel login = new LoginPanel();
        login.prefill("admin", "123456", "K7Pa2");
        save(login, LoginPanel.WIDTH, LoginPanel.HEIGHT, "docs/01-login.png");

        // 02 4×4 进行中
        saveGame(midGame(Difficulty.CLASSIC, 7L, 70, 37, 84_000L,
                Records.Best.of(148, 95)), "docs/02-game-4x4.png");

        // 03 5×5 进行中
        saveGame(midGame(Difficulty.HARD, 11L, 120, 88, 192_000L,
                Records.Best.of(214, 292)), "docs/03-game-5x5.png");

        // 04 胜利（4×4，破两项纪录）
        saveGame(winView(), "docs/04-win.png");

        // 05 查看原图（4×4 进行中，按住 A 的叠加层）
        BoardView original = midGame(Difficulty.CLASSIC, 7L, 70, 37, 84_000L,
                Records.Best.of(148, 95));
        original.showingOriginal = true;
        saveGame(original, "docs/05-original.png");

        // 06 3×3 进行中
        saveGame(midGame(Difficulty.EASY, 3L, 12, 9, 18_000L,
                Records.Best.of(21, 26)), "docs/06-game-3x3.png");

        System.out.println("完成：docs/ 下共 6 张截图。");
    }

    /** 拼一帧「进行中」的画面：随机游走得到一定可达的盘面，空位尽量落在中间更像在玩。 */
    private static BoardView midGame(Difficulty difficulty, long seed, int moves,
                                     int steps, long elapsedMs, Records.Best best) {
        Board board = new Board(difficulty);
        Random random = new Random(seed);
        int attempt = 0;
        while (true) {
            board.scramble(moves, random);
            int size = difficulty.getSize();
            boolean interior = board.getBlankRow() > 0 && board.getBlankRow() < size - 1
                    && board.getBlankCol() > 0 && board.getBlankCol() < size - 1;
            if (interior || ++attempt > 20) {
                break;
            }
        }
        BoardView view = BoardView.of(board, steps, elapsedMs);
        view.best = best;
        view.canUndo = true;
        return view;
    }

    /** 胜利那一帧：盘面已解、破了两项纪录。 */
    private static BoardView winView() {
        BoardView view = new BoardView();
        view.difficulty = Difficulty.CLASSIC;
        view.data = Board.solvedLayout(view.difficulty.getSize());
        view.steps = 142;
        view.elapsedMs = 151_000L;   // 02:31
        view.won = true;
        view.canUndo = false;
        view.best = Records.Best.of(142, 151);
        view.newRecordSteps = true;
        view.newRecordTime = true;
        return view;
    }

    private static void saveGame(BoardView view, String path) throws IOException {
        GamePanel panel = new GamePanel(view, new Tiles(view.difficulty));
        save(panel, BoardRenderer.WIDTH, BoardRenderer.HEIGHT, path);
    }

    /** 把组件画进图片：2 倍超采样，再缩回逻辑尺寸写 PNG。 */
    private static void save(JComponent component, int width, int height, String path) throws IOException {
        component.setSize(width, height);
        component.doLayout();

        BufferedImage big = new BufferedImage(width * SUPERSAMPLE, height * SUPERSAMPLE,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = big.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.scale(SUPERSAMPLE, SUPERSAMPLE);
        component.paint(g);
        g.dispose();

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D shrink = out.createGraphics();
        shrink.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        shrink.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        shrink.drawImage(big, 0, 0, width, height, null);
        shrink.dispose();

        File file = new File(path);
        ImageIO.write(out, "png", file);
        System.out.printf("%-26s %4d×%-4d  %6.1f KB%n", path, out.getWidth(), out.getHeight(),
                file.length() / 1024.0);
    }
}
