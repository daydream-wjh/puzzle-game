package com.wjh.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * 承载一帧画面的面板：只做「把 {@link BoardView} 交给 {@link BoardRenderer}」这一件事。
 * <p>
 * 真实窗口（{@link GameJFrame}）和离屏截图（{@code tools/Snapshot}）用的是同一个面板类，
 * 差别只在于一个是显示出来，一个是 {@code setSize()} 之后 {@code paint(g)} 到图片上。
 */
public class GamePanel extends JPanel {

    private BoardView view;
    private Tiles tiles;

    public GamePanel(BoardView view, Tiles tiles) {
        this.view = view;
        this.tiles = tiles;
        setOpaque(true);
        setBackground(BoardRenderer.backgroundColor());
        setFocusable(true);
    }

    /** 换一份状态（切难度 / 重开一局时用）。 */
    public void setView(BoardView view) {
        this.view = view;
        repaint();
    }

    public BoardView getView() {
        return view;
    }

    /** 换素材来源（切难度时切片方式不同）。 */
    public void setTiles(Tiles tiles) {
        this.tiles = tiles;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(BoardRenderer.backgroundColor().getRGB()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (view != null && tiles != null) {
            BoardRenderer.paint(g2, view, tiles);
        }
        g2.dispose();
    }
}
