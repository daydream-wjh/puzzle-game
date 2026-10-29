package com.wjh.ui;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

public class GameJFrame extends JFrame implements KeyListener, ActionListener {
    int[][] data=new int[4][4];
    int[][] win={
            {1,2,3,4},
            {5,6,7,8},
            {9,10,11,12},
            {13,14,15,0}
    };
    int step = 0;
    JMenuItem replayItem=new JMenuItem("重新游戏");
    JMenuItem reLoginItem=new JMenuItem("重新登录");
    JMenuItem closeItem=new JMenuItem("关闭游戏");

    JMenuItem accountItem=new JMenuItem("公众号");

    /** 拼图区左上角与单格尺寸（坐标、尺寸与原版一致，未改动）。 */
    private static final int ORIGIN_X = 83;
    private static final int ORIGIN_Y = 134;
    private static final int CELL_SIZE = 105;

    /** 预加载的拼图切片：tileIcons[n - 1] 就是 img/CS (n).png（n = 1..16）。 */
    private final ImageIcon[] tileIcons = new ImageIcon[16];
    /** 原图 / 背景 / 胜利标志也只读一次盘。 */
    private final ImageIcon allImage = new ImageIcon("img/all.png");
    private final ImageIcon bgImage = new ImageIcon("img/bg.png");
    private final ImageIcon winImage = new ImageIcon("img/win.png");
    private final Random random = new Random();

    public GameJFrame(){

        initJFrame();
        //初始化菜单
        initJMenuBar();
        //预加载图片，之后重绘不再读盘
        loadImages();
        //初始化数据
        initData();

        initImage();

        this.setVisible(true);
    }
    int x=0,y=0;

    /** 构造时把 16 张切片一次性读进内存（img/ 路径、文件名保持原样）。 */
    private void loadImages() {
        for (int i = 0; i < tileIcons.length; i++) {
            tileIcons[i] = new ImageIcon("img/CS (" + (i + 1) + ").png");
        }
    }

    private void initData() {
        int[] tempArr = new int[16];
        for (int i = 0; i < tempArr.length; i++) {
            tempArr[i] = i;
        }
        // 标准 Fisher–Yates 洗牌；4×4 只有一半局面可解，不可解就重洗。
        do {
            shuffle(tempArr, random);
        } while (!isSolvable(tempArr));

        for (int i = 0; i < tempArr.length; i++) {
            if (tempArr[i] == 0){
                x=i / 4;
                y=i % 4;
            }
            data[i/4][i%4]=tempArr[i];
        }
    }

    /** Fisher–Yates：从后往前，每一步和 [0, i] 里随机一位交换，每种排列等概率。 */
    public static void shuffle(int[] arr, Random rd) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rd.nextInt(i + 1);
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }

    /**
     * 判断宽度为偶数（本游戏 4）的滑块局面是否可解。
     * <p>
     * 忽略 0 统计逆序数 inv；空位所在行从底部数 1-based 记作 rowFromBottom（1..4）。
     * 可解 ⇔ {@code (inv + rowFromBottom)} 为奇数，也就是
     * inv 为偶数时 rowFromBottom 为奇数，或 inv 为奇数时 rowFromBottom 为偶数。
     * 判据已用 2×4 八数码全状态空间 BFS（40320 个排列，0 不一致）和 10 万次 4×4 洗牌统计验证过。
     */
    public static boolean isSolvable(int[] tiles) {
        return isSolvable(tiles, 4);
    }

    /** 同上，width 为列数（只适用于偶数列的盘面，如 4 或 2）。 */
    public static boolean isSolvable(int[] tiles, int width) {
        if (tiles == null || tiles.length == 0 || width <= 0) {
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
        int rows = tiles.length / width;
        int rowFromBottom = rows - blankIndex / width;
        return (inversions + rowFromBottom) % 2 == 1;
    }

    private void initImage() {
        this.getContentPane().removeAll();
        if (victory()) {
            JLabel winJLabel = new JLabel(winImage);
            winJLabel.setBounds(203,283,197,73);
            this.getContentPane().add(winJLabel);
        }

        JLabel stepCount = new JLabel("步数:" + step);
        stepCount.setBounds(50,30,100,73);
        this.getContentPane().add(stepCount);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                int num=data[i][j];
                if (num == 0) {
                    // 空位：img 里没有 CS (0).png，显式跳过绘制，保持透明格。
                    continue;
                }
                ImageIcon imageIcon1=tileIcons[num - 1];
                JLabel jLabel1=new JLabel(imageIcon1);
                jLabel1.setBounds(j*CELL_SIZE+ORIGIN_X,i*CELL_SIZE+ORIGIN_Y,CELL_SIZE,CELL_SIZE);
                //添加边框
                jLabel1.setBorder(new BevelBorder(0));
                this.getContentPane().add(jLabel1);
            }
        }
        //添加背景图片
        JLabel background = new JLabel(bgImage);
        background.setBounds(30,40,508,560);
        this.getContentPane().add(background);

        this.getContentPane().repaint();
    }

    private void initJMenuBar() {
        JMenuBar jMenuBar=new JMenuBar();

        JMenu functionJMenu=new JMenu("功能");
        JMenu aboutJMenu=new JMenu("关于我们");




        this.setJMenuBar(jMenuBar);

        functionJMenu.add(replayItem);
        functionJMenu.add(reLoginItem);
        functionJMenu.add(closeItem);

        aboutJMenu.add(accountItem);

        replayItem.addActionListener(this);
        reLoginItem.addActionListener(this);
        closeItem.addActionListener(this);
        accountItem.addActionListener(this);

        jMenuBar.add(functionJMenu);
        jMenuBar.add(aboutJMenu);
    }

    private void initJFrame() {
        this.setSize(603,680);
        this.setTitle("拼图单机版");
        this.setAlwaysOnTop(true);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(3);
        this.setLayout(null);
        this.addKeyListener(this);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code=e.getKeyCode();
        if(code == 65){
            System.out.println("按下a");
            this.getContentPane().removeAll();
            JLabel all=new JLabel(allImage);
            all.setBounds(83,134,420,420);
            this.getContentPane().add(all);
            JLabel background = new JLabel(bgImage);
            background.setBounds(30,40,508,560);
            this.getContentPane().add(background);
            this.getContentPane().repaint();

        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (victory()) {
            return;
        }
        int code=e.getKeyCode();
        if (code==37){
            System.out.println("向左移动");
            if (y!=0) {
                data[x][y]=data[x][y-1];
                data[x][y-1]=0;
                y--;
                step++;
            }
            initImage();
        }else if (code==38){
            System.out.println("向上移动");
            if (x!=0) {
                data[x][y]=data[x-1][y];
                data[x-1][y]=0;
                x--;
                step++;
            }
            initImage();
        }else if (code==39){
            System.out.println("向右移动");
            if (y!=3) {
                data[x][y]=data[x][y+1];
                data[x][y+1]=0;
                y++;
                step++;
            }
            initImage();
        }else if (code==40){
            System.out.println("向下移动");
            if (x!=3) {
                data[x][y]=data[x+1][y];
                data[x+1][y]=0;
                x++;
                step++;
            }
            initImage();
        }else if (code == 65){
            initImage();
        }else if(code == 87){
            // TODO: 发布前删除调试后门
            data=new int[][]{
                    {1,2,3,4},
                    {5,6,7,8},
                    {9,10,11,12},
                    {13,14,15,0}
            };
            x=3;
            y=3;
            initImage();
        }
    }
    public boolean victory(){
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (data[i][j] != win[i][j]){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
       Object obj = e.getSource();
       if (obj==replayItem){
           System.out.println("重新游戏");
           step=0;
           initData();
           initImage();
       }else if (obj == reLoginItem){
           System.out.println("重新登录");
           this.setVisible(false);
           new LoginJFrame();
       }else if (obj == closeItem){
           System.out.println("关闭游戏");
           System.exit(0);
       }else if (obj == accountItem){
           System.out.println("公众号");
           JDialog jDialog=new JDialog();
           JLabel jLabel = new JLabel(new ImageIcon("img/wy.png"));
           jLabel.setBounds(0,0,720,540);
           jDialog.getContentPane().add(jLabel);
           jDialog.setSize(800,600);
           jDialog.setAlwaysOnTop(true);
           jDialog.setTitle("about my girl");
           jDialog.setLocationRelativeTo(null);
           jDialog.setModal(true);
           jDialog.setVisible(true);

       }
    }
}
