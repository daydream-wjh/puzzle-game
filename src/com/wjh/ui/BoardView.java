package com.wjh.ui;

/**
 * 渲染一帧画面需要的全部状态。
 * <p>
 * 界面和离屏截图（{@code tools/Snapshot}）共用同一个数据载体，
 * 所以 README 里的截图和真正跑起来的游戏永远是同一套绘制代码画出来的。
 */
public class BoardView {

    /** 难度档位。 */
    public Difficulty difficulty = Difficulty.CLASSIC;
    /** 盘面，0 是空位。 */
    public int[][] data = Board.solvedLayout(4);
    /** 已走步数。 */
    public int steps;
    /** 已用时间（毫秒）。 */
    public long elapsedMs;
    /** 是否正在按住 A 看原图。 */
    public boolean showingOriginal;
    /** 是否已经拼好。 */
    public boolean won;
    /** 是否还能撤销。 */
    public boolean canUndo;
    /** 当前难度的最佳纪录（可能为空纪录）。 */
    public Records.Best best = Records.Best.EMPTY;
    /** 这一局是否刷新了最少步数纪录。 */
    public boolean newRecordSteps;
    /** 这一局是否刷新了最快用时纪录。 */
    public boolean newRecordTime;

    /** 用一局游戏的真实状态填充视图。 */
    public static BoardView of(Board board, int steps, long elapsedMs) {
        BoardView view = new BoardView();
        view.difficulty = board.getDifficulty();
        view.data = board.snapshot();
        view.steps = steps;
        view.elapsedMs = elapsedMs;
        view.canUndo = board.getHistorySize() > 0;
        return view;
    }

    /** 胜利提示里的一行字：步数 + 用时。 */
    public String resultText() {
        return steps + " 步 · 用时 " + BoardRenderer.formatTime(elapsedMs);
    }

    /** 胜利提示里的纪录判断。 */
    public String recordText() {
        if (newRecordSteps && newRecordTime) {
            return "新纪录：最少步数 + 最快用时";
        }
        if (newRecordSteps) {
            return "新纪录：最少步数";
        }
        if (newRecordTime) {
            return "新纪录：最快用时";
        }
        return "未破纪录，继续加油";
    }
}
