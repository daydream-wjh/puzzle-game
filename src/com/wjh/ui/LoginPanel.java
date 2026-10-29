package com.wjh.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 登录页：整页自己画（背景是 {@code img/bg.png} 压暗后的纹理 + 一张米色卡片），
 * 里面的输入框 / 按钮还是真 Swing 组件。
 * <p>
 * 从 {@link LoginJFrame} 里抽出来单独成面板，一是让窗体只剩「套壳 + 跳转」，
 * 二是离屏截图工具可以直接实例化它渲染成 PNG（headless 下 JFrame 建不出来，JPanel 可以）。
 * 登录校验规则和原来完全一致：先比验证码，再用 {@link User#equals} 查账号。
 */
public class LoginPanel extends JPanel implements ActionListener {

    /** 面板宽。 */
    public static final int WIDTH = 640;
    /** 面板高。 */
    public static final int HEIGHT = 760;

    private static final int CARD_X = 70;
    private static final int CARD_Y = 132;
    private static final int CARD_W = 500;
    private static final int CARD_H = 506;

    private static final Color CREAM = new Color(0xFBF1DE);
    private static final Color INK = new Color(0x2A2118);
    private static final Color MUTED_INK = new Color(0x8B7A64);
    private static final Color GOLD = new Color(0xE8BE7C);
    private static final Color GOLD_DEEP = new Color(0xC08A2E);

    private final ArrayList<User> allusers = new ArrayList<>();
    private final JTextField usernamet = new JTextField();
    private final JPasswordField passwordt = new JPasswordField();
    private final JTextField codet = new JTextField();
    private final JLabel rightCode = new CaptchaLabel();
    private final JButton login = new JButton("登录");
    private final JButton register = new JButton("注册");

    private BufferedImage backdrop;
    private Runnable onLoginSuccess = new Runnable() {
        @Override
        public void run() {
        }
    };
    private Runnable onRegister = new Runnable() {
        @Override
        public void run() {
        }
    };

    public LoginPanel() {
        setLayout(null);
        setOpaque(true);
        setBackground(new Color(0x1A1512));
        initView();
        loadUsers();
        rightCode.setText(newCode());
    }

    /** 登录成功（验证码 + 账号都对）之后要做什么。 */
    public void setOnLoginSuccess(Runnable action) {
        this.onLoginSuccess = action == null ? this.onLoginSuccess : action;
    }

    /** 点「注册」之后要做什么。 */
    public void setOnRegister(Runnable action) {
        this.onRegister = action == null ? this.onRegister : action;
    }

    /** 从 {@link UserStore} 读全部账号。 */
    private void loadUsers() {
        allusers.clear();
        for (String line : UserStore.load()) {
            String[] split = line.split("&");
            if (split.length < 2) {
                continue;
            }
            String username = valueOf(split[0]);
            String password = valueOf(split[1]);
            if (username == null || password == null) {
                continue;
            }
            allusers.add(new User(username, password));
        }
    }

    /** 取 "key=value" 里等号后面的部分，值里带等号也能原样保留。 */
    private static String valueOf(String pair) {
        int eq = pair.indexOf('=');
        if (eq < 0) {
            return null;
        }
        return pair.substring(eq + 1).trim();
    }

    private void initView() {
        JLabel usernameLabel = new JLabel("用户名");
        JLabel passwordLabel = new JLabel("密码");
        JLabel codeLabel = new JLabel("验证码");
        Font labelFont = Fonts.plain(14);
        Color labelColor = new Color(0x6B5A46);
        usernameLabel.setFont(labelFont);
        usernameLabel.setForeground(labelColor);
        passwordLabel.setFont(labelFont);
        passwordLabel.setForeground(labelColor);
        codeLabel.setFont(labelFont);
        codeLabel.setForeground(labelColor);

        usernameLabel.setBounds(110, CARD_Y + 118, 80, 20);
        passwordLabel.setBounds(110, CARD_Y + 196, 80, 20);
        codeLabel.setBounds(110, CARD_Y + 274, 80, 20);

        styleField(usernamet);
        styleField(passwordt);
        styleField(codet);
        usernamet.setBounds(110, CARD_Y + 140, 420, 42);
        passwordt.setBounds(110, CARD_Y + 218, 420, 42);
        codet.setBounds(110, CARD_Y + 296, 240, 42);
        rightCode.setBounds(366, CARD_Y + 296, 164, 42);

        stylePrimary(login);
        styleSecondary(register);
        login.setBounds(110, CARD_Y + 356, 200, 44);
        register.setBounds(330, CARD_Y + 356, 200, 44);
        login.addActionListener(this);
        register.addActionListener(this);

        add(usernameLabel);
        add(passwordLabel);
        add(codeLabel);
        add(usernamet);
        add(passwordt);
        add(codet);
        add(rightCode);
        add(login);
        add(register);
    }

    private static void styleField(JTextField field) {
        field.setFont(Fonts.plain(16));
        field.setForeground(INK);
        field.setBackground(new Color(0xFFFDF6));
        field.setCaretColor(INK);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDCC8A6), 1, true),
                BorderFactory.createEmptyBorder(0, 10, 0, 8)));
    }

    private static void stylePrimary(JButton button) {
        button.setFont(Fonts.bold(16));
        button.setForeground(INK);
        button.setBackground(GOLD);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createLineBorder(GOLD_DEEP, 1, true));
    }

    private static void styleSecondary(JButton button) {
        button.setFont(Fonts.bold(16));
        button.setForeground(new Color(0x6B5A46));
        button.setBackground(new Color(0xF3E4C8));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createLineBorder(new Color(0xD9C4A0), 1, true));
    }

    /** 5 位随机验证码：a-z + A-Z + 0-9。 */
    private String newCode() {
        char[] chars = new char[26 * 2 + 10];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            chars[i] = (char) ('a' + i);
        }
        for (int i = 26; i < 26 + 26; i++) {
            chars[i] = (char) ('A' + i - 26);
        }
        for (int i = 52; i < 10 + 52; i++) {
            chars[i] = (char) ('0' + i - 52);
        }
        Random rd = new Random();
        for (int i = 0; i < 5; i++) {
            sb.append(chars[rd.nextInt(26 * 2 + 10)]);
        }
        return sb.toString();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == login) {
            if (!rightCode.getText().equals(codet.getText())) {
                showDialog("验证码错误");
                return;
            }
            User user = new User(usernamet.getText(), new String(passwordt.getPassword()));
            if (allusers.contains(user)) {
                showDialog("欢迎" + usernamet.getText());
                onLoginSuccess.run();
            } else {
                showDialog("用户名或密码错误");
            }
        } else if (source == register) {
            onRegister.run();
        }
    }

    private void showDialog(String str) {
        JDialog jDialog = new JDialog(new JFrame(), "", true);
        JLabel label = new JLabel(str);
        jDialog.setSize(300, 150);
        jDialog.setLocationRelativeTo(null);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        jDialog.add(label);
        jDialog.setAlwaysOnTop(true);
        jDialog.setVisible(true);
    }

    // ------------------------------------------------------------------ 给窗体 / 截图工具用的存取

    /** 面板上的账号列表（注册窗口要用）。 */
    public List<User> getUsers() {
        return allusers;
    }

    /** 当前输入的用户名。 */
    public String getUsernameText() {
        return usernamet.getText();
    }

    /** 预填一组输入（截图 / 自测用，不影响正常交互）。 */
    public void prefill(String username, String password, String code) {
        usernamet.setText(username);
        passwordt.setText(password);
        codet.setText(code);
        rightCode.setText(code);
    }

    // ------------------------------------------------------------------ 绘制

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (backdrop == null) {
            backdrop = Assets.read("bg.png");
        }
        if (backdrop != null) {
            // 等比裁切铺满，背景只是纹理，不抢卡片的视线。
            double scale = Math.max(WIDTH / (double) backdrop.getWidth(), HEIGHT / (double) backdrop.getHeight());
            int w = (int) Math.round(backdrop.getWidth() * scale);
            int h = (int) Math.round(backdrop.getHeight() * scale);
            g2.drawImage(backdrop, (WIDTH - w) / 2, (HEIGHT - h) / 2, w, h, null);
        }
        g2.setPaint(new GradientPaint(0, 0, new Color(22, 18, 15, 205), 0, HEIGHT, new Color(46, 36, 27, 226)));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // 卡片投影
        for (int i = 5; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 12 + i * 4));
            g2.fill(new RoundRectangle2D.Float(CARD_X - i, CARD_Y - i + 8, CARD_W + i * 2, CARD_H + i * 2, 30, 30));
        }
        g2.setColor(CREAM);
        g2.fill(new RoundRectangle2D.Float(CARD_X, CARD_Y, CARD_W, CARD_H, 28, 28));
        g2.setColor(new Color(0xE6D2AE));
        g2.draw(new RoundRectangle2D.Float(CARD_X + 0.5f, CARD_Y + 0.5f, CARD_W - 1, CARD_H - 1, 28, 28));

        UiText.text(g2, "欢迎回来", 110, CARD_Y + 66, Fonts.bold(34), INK);
        UiText.text(g2, "拼图游戏 · 长沙夜景 · 3 × 3 / 4 × 4 / 5 × 5", 110, CARD_Y + 94,
                Fonts.plain(13), MUTED_INK);
        g2.setColor(new Color(0xE2C79B));
        g2.fill(new RoundRectangle2D.Float(110, CARD_Y + 106, CARD_W - 80, 2, 2, 2));

        UiText.text(g2, "默认账号 admin / 123456 · 首次运行自动生成 data/userinfo.txt",
                110, CARD_Y + 442, Fonts.plain(12), MUTED_INK);
        UiText.text(g2, "素材：img/   ·   纪录：~/.puzzle-game/records.properties",
                110, CARD_Y + 464, Fonts.plain(12), new Color(0xA5947C));

        g2.dispose();
    }

    /** 带底色的验证码框（真 JLabel 子类，截图里同样是组件自己画的）。 */
    private static class CaptchaLabel extends JLabel {

        CaptchaLabel() {
            super("", SwingConstants.CENTER);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0xF1E1C2));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
            g2.setColor(new Color(0xDCC8A6));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
            g2.setColor(new Color(192, 138, 46, 90));
            g2.drawLine(8, getHeight() - 12, getWidth() - 10, 12);
            g2.setColor(GOLD_DEEP);
            UiText.center(g2, getText(), getWidth() / 2, getHeight() / 2 + 8, Fonts.bold(22), GOLD_DEEP);
            g2.dispose();
        }
    }
}
