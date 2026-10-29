package com.wjh.ui;

import javax.swing.JFrame;
import java.awt.Dimension;

/**
 * 登录窗体：套一个 640×760 的窗口，逻辑全在 {@link LoginPanel} 里。
 * 登录成功 → 进游戏；点注册 → 去注册窗口。
 */
public class LoginJFrame extends JFrame {

    private final LoginPanel panel = new LoginPanel();

    public LoginJFrame() {
        initJFrame();
        panel.setOnLoginSuccess(new Runnable() {
            @Override
            public void run() {
                LoginJFrame.this.setVisible(false);
                new GameJFrame();
            }
        });
        panel.setOnRegister(new Runnable() {
            @Override
            public void run() {
                LoginJFrame.this.setVisible(false);
                new RegisterJFrame(new java.util.ArrayList<>(panel.getUsers()));
            }
        });
        this.setVisible(true);
        fitToContent();
    }

    private void initJFrame() {
        this.setTitle("拼图 登录");
        this.setLayout(null);
        this.setContentPane(panel);
        this.setSize(LoginPanel.WIDTH, LoginPanel.HEIGHT);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /** 内容区正好等于面板尺寸，免得窗口边框把卡片挤歪。 */
    private void fitToContent() {
        Dimension content = getContentPane().getSize();
        setSize(getWidth() + (LoginPanel.WIDTH - content.width),
                getHeight() + (LoginPanel.HEIGHT - content.height));
        setLocationRelativeTo(null);
    }
}
