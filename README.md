# puzzlegame

> Java Swing 拼图小游戏：4×4 滑块拼图，带注册登录、验证码、步数统计和查看原图

## 项目简介

一个用 Java Swing 手写的桌面拼图游戏，功能完整度超出「拼图」本身——它带了一套简易的用户系统（注册 / 登录 / 验证码），登录成功后才进入游戏。

玩法是很经典的 **4×4 数字华容道（滑块拼图）**：一张图被切成 16 块打乱（其中一块是空位），用方向键把滑块推来推去，直到还原成原图。游戏过程中统计步数，随时可以按住 `A` 偷看原图，拼好后弹出「胜利」标志。

窗口不含任何图片素材以外的资源，也没有音频、没有联网（用户数据存在一个 txt 里，由 `UserStore` 统一管理路径）。代码总量约 940 行，4 个窗体类 + 1 个实体类 + 1 个 `UserStore`，很适合当作 Swing 的入门参考。

## 内容概览

### 玩法与操作

| 操作 | 效果 | 代码位置 |
| --- | --- | --- |
| `←` `↑` `→` `↓` | 把空白块相邻的滑块移进空白位（即「空格反向移动」），边框约束在 4×4 网格内，越界不生效 | `GameJFrame.keyReleased`，`code==37/38/39/40` |
| 按住 `A` | 临时显示完整原图 `img/all.png`（覆盖在拼图区上），松开后恢复 | `keyPressed`（`code==65`）+ `keyReleased` 里再调 `initImage()` |
| `W` | **一键通关**：直接把 `data` 置为胜利布局并重绘（调试用的作弊键，代码里已加 `// TODO: 发布前删除调试后门` 注释，**后门本身保留未删**） | `keyReleased`，`code==87` |
| 菜单「功能 → 重新游戏」 | `step` 归零、重新洗牌 | `actionPerformed` → `replayItem` |
| 菜单「功能 → 重新登录」 | 隐藏游戏窗口、回到登录窗口 | `actionPerformed` → `reLoginItem` |
| 菜单「功能 → 关闭游戏」 | `System.exit(0)` | `actionPerformed` → `closeItem` |
| 菜单「关于我们 → 公众号」 | 弹一个模态 `JDialog`，显示 `img/wy.png`，标题 `about my girl` | `actionPerformed` → `accountItem` |

### 关卡 / 计分逻辑

没有关卡概念，只有**一局**：每次「重新游戏」或重新登录后重新洗牌。

- **开局**：`initData()` 生成 `[0..15]` 的数组，用**标准 Fisher–Yates**（从后往前，第 `i` 位与 `[0, i]` 里的随机一位交换，每种排列等概率）洗牌，然后做**可解性校验**：忽略 `0` 统计逆序数 `inv`，空位所在行从底部数 1-based 记作 `rowFromBottom`，可解 ⇔ `(inv + rowFromBottom) % 2 == 1`；不满足就重洗，所以开局不会是死局。洗好的 16 个数写进 `int[4][4] data`，值为 `0` 的那一格就是**空位**，同时记录它的坐标 `x, y`。
- **移动**：按方向键时，把空位与相邻格的值互换，并同步更新 `x` / `y`。所以四个方向键的实际语义是「把空格往这个方向挪」，这和大部分滑块拼图的手感一致（按 `←` 是把右边的块推进空格）。
- **计分**：只有一个 `int step`，每成功移动一次 `step++`，每次重绘时在 `(50,30)` 位置画一个 `JLabel("步数:" + step)`。**没有时间限制、没有排行榜、没有分数换算**，`step` 就是唯一指标。
- **胜负判定**：`victory()` 把 `data` 和常量 `win`（`1..15` 顺序排列 + 右下角 `0`）逐格比对。为真时在 `(203,283)` 叠加 `img/win.png`，同时 `keyReleased` 开头就 `return`，锁死移动。
- **重绘**：`initImage()` 每帧仍先 `removeAll()` 再重建 `JLabel`（全量刷新），但 **16 张 `ImageIcon` 已在构造时预加载进 `tileIcons[]`**（`all.png` / `bg.png` / `win.png` 也各只读一次盘），移动时不再反复解码 PNG；`num == 0` 的空位**显式 `continue` 跳过绘制**，不再依赖「`img/CS (0).png` 不存在 → `ImageIcon` 加载成 null 图标」这个副作用。

### 登录 / 注册流程

启动入口是 `App.main` → `new LoginJFrame()`（`GameJFrame` 那行被注释掉了，所以必须走登录）。

```
LoginJFrame（488×430）
 ├─ 用户名 / 密码 / 验证码 三个输入框
 ├─ 验证码：5 位随机字符（a-z + A-Z + 0-9），生成后固定显示在 (320,200) 的 JLabel 上
 ├─ 「登录」：先比对验证码 → 再用 User.equals（用户名 + 密码同时相等）在 allusers 里找
 │     ├─ 命中 → 弹「欢迎 xxx」→ new GameJFrame()
 │     └─ 未命中 → 弹「用户名或密码错误」
 └─ 「注册」：隐藏当前窗口 → new RegisterJFrame(allusers)
```

`RegisterJFrame`（488×430）有用户名 / 密码 / 再次输入密码三个框和「提交」「重置」两个按钮，提交时由 `checkInput()` 做**三项校验**：① 三项都非空（用户名 / 密码 / **确认密码**都判）；② 两个密码框内容真的相等（`password.getText().equals(repassword.getText())`）；③ 用户名查重（`containsname(username.getText().trim())`）。任一项不过就弹窗提示并中止，全过后把新用户加进 `allusers`，再用 `UserStore.save(...)` 整体覆写数据文件，然后回到登录窗口。

用户文件由 **`UserStore`（`src/com/wjh/ui/UserStore.java`）统一管理**，不再有任何硬编码路径：

- 解析顺序：`-Dpuzzlegame.data=<文件或目录>` → 环境变量 `PUZZLEGAME_DATA=<文件或目录>` → 当前工作目录下的 `data/userinfo.txt`（存在时优先）→ 工程根（同时含 `img/` 与 `src/` 的那一层）的 `data/userinfo.txt` → 都不存在时退回第 3 项并**自动创建**；
- 配置成目录（或以 `/`、`\` 结尾）时会自动补上 `userinfo.txt`；
- 首次运行自动创建 `data/userinfo.txt`，初始内容优先从仓库里遗留的 `src/com/wjh/ui/userinfo.txt` **迁移**（只读不改原文件），没有遗留文件才写入默认账号 `username=admin&password=123456`；
- 读盘失败不会让登录窗口起不来，回退成默认账号并在终端打日志。

数据文件一行一个用户，就是这个游戏的「数据库」：

```
username=admin&password=123456
username=test&password=123456
username=wy&password=wy
```

格式由 `User.toString()` 决定（`"username=" + username + "&password=" + password`），读取时按 `&` 切分、再按 `=` 取值（值里带 `=` 也能原样保留）——正好和隔壁 `java` 仓库 `month12` 聊天室的协议是同一套写法，应该是同期练习的延续。

> 上面这几行是仓库里遗留的 `src/com/wjh/ui/userinfo.txt` 的占位内容；游戏自己读写的 `data/userinfo.txt` 已被 `.gitignore` 忽略，属于运行时数据。

### 素材结构

`img/` 下 20 个 PNG，一共 5 类（16 张切片 + 4 张整图素材）：

| 文件 | 尺寸 | 用途 |
| --- | --- | --- |
| `CS (1).png` ~ `CS (16).png` | 每张 105×105 | **拼图切片**，16 张拼成 4×4；文件名里的数字就是 `data` 里的值（`"CS (" + num + ").png"`） |
| `all.png` | 420×420 | 完整原图 = 4×4 张切片（420 = 105 × 4），按 `A` 时铺在拼图区 |
| `bg.png` | 580×560 | 游戏窗口的木质边框背景，绘制在拼图之上（`(30,40)`，缩放到 508×560） |
| `win.png` | 220×88 | 通关时叠加的「胜利」字样 |
| `wy.png` | 720×540 | 「关于我们 → 公众号」弹窗里的图 |

坐标是硬编码的：拼图区左上角 `(83,134)`，每格 `105×105`，所以第 `i` 行 `j` 列的位置是 `(j*105+83, i*105+134)`；窗口 `603×680`，`setLayout(null)` 绝对定位。

`lib/` 下两个 jar：`hutool-all-5.8.23.jar`、`commons-io-2.15.0.jar`。**现在两个都没被引用了**——旧版读写 `userinfo.txt` 用的是 hutool 的 `FileUtil.writeLines`，`UserStore` 改成纯 `java.nio.file` 实现（`Files.readAllLines` / `Files.write`）后，`src` 里已经没有任何 `cn.hutool` / `org.apache.commons` 引用（已实测 `javac` 不带 `-cp` 也能全量编译通过）。

## 技术栈与运行方式

- **语言 / 版本**：Java + Swing（`javax.swing`、`java.awt`）。`.idea/misc.xml` 声明 `languageLevel="JDK_21"`、`project-jdk-name="21"`，`puzzlegame.iml` 用的是 `inheritedJdk`。代码本身没有用到 14+ 的语法（没有 `switch` 箭头、没有 `var`、没有 `getFirst()` 这类 SequencedCollection API），**JDK 8 及以上都能编译**——实测 `javac --release 8` 与 `javac --release 11` 均 0 错误，用 JDK 21 也 0 错误 0 警告。
- **GUI 框架**：纯 Swing（`JFrame` / `JLabel` / `JButton` / `JTextField` / `JDialog` / `JMenuBar`），没有 JavaFX，没有第三方 UI 库。
- **第三方依赖**：`lib/hutool-all-5.8.23.jar`、`lib/commons-io-2.15.0.jar`——**两个都已不再被代码引用**（用户文件读写改成了 JDK 自带的 `java.nio.file`）。
- **构建工具**：没有。不是 Maven / Gradle 工程，只能靠 IDEA 模块（`puzzlegame.iml` 里已把 `lib/*.jar` 挂成 library）或手写 `javac`。
- **图片加载方式**：`new ImageIcon("img/xxx.png")`——**相对路径**，所以工作目录必须是仓库根目录（用户数据文件不在此列，见下面的 `UserStore` 说明）。

命令行编译运行（在仓库根目录）：

```bash
# 编译（现在没有任何第三方依赖，-cp 可带可不带；带上也无害）
javac -encoding UTF-8 -d out $(find src -name "*.java")
# 旧写法仍可用：javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name "*.java")

# 运行：必须在仓库根目录，否则 img/ 找不到
java -cp "out:lib/*" com.wjh.ui.App
```

macOS / Linux 用 `:`，Windows 用 `;` 分隔 classpath。

用 IDEA：`File → Open` 选仓库根目录，`puzzlegame.iml` 已配好 `src` 和 `lib`，右键 `App.java` 运行即可。

> **数据文件不用再手改路径了**：旧版 `LoginJFrame` / `RegisterJFrame` 里各有一处写死的 `D:\desktop\java\puzzlegame\src\com\wjh\ui\userinfo.txt`，在 macOS / Linux 上启动即 `IORuntimeException`。现在统一走 `UserStore`：默认数据文件是**工作目录下的 `data/userinfo.txt`**，**首次运行自动创建**（初始内容为遗留文件迁移来的账号，否则默认 `admin/123456`），也可以显式指定：
>
> ```bash
> java -Dpuzzlegame.data=/tmp/puzzle -cp out com.wjh.ui.App   # 目录或文件都行
> PUZZLEGAME_DATA=/tmp/puzzle/userinfo.txt java -cp out com.wjh.ui.App
> ```

## 目录结构

```
puzzlegame/
├── README.md
├── puzzlegame.iml               # IDEA 模块：src 为源码根，lib/*.jar 为依赖（jar 实际已不用）
├── .gitignore                   # 忽略 out/、bin/、.DS_Store、data/（没忽略 .idea/）
├── .idea/                       # 4 个文件，已入库（含 workspace.xml）
├── img/                         # 20 张 PNG
│   ├── CS (1).png ... CS (16).png   # 16 张 105×105 切片，无 CS (0).png（空位现在显式跳过绘制）
│   ├── all.png                     # 420×420 原图（按 A 查看）
│   ├── bg.png                      # 580×560 边框背景
│   ├── win.png                     # 220×88 胜利标志
│   └── wy.png                      # 720×540「关于我们」弹窗图
├── data/                        # 运行时自动创建（已被 .gitignore 忽略）
│   └── userinfo.txt                # 用户数据（明文）：username=x&password=y
├── lib/
│   ├── commons-io-2.15.0.jar        # 未被引用
│   └── hutool-all-5.8.23.jar        # 未被引用（旧版用它读写 userinfo.txt）
└── src/com/wjh/
    ├── ui/                          # 全部业务代码
    │   ├── App.java                 # 入口：new LoginJFrame()
    │   ├── LoginJFrame.java         # 登录窗体（170 行）+ 验证码生成
    │   ├── RegisterJFrame.java      # 注册窗体（164 行）
    │   ├── GameJFrame.java          # 游戏主窗体（324 行）：洗牌+可解性校验、键盘、菜单、判胜、图标预加载
    │   ├── User.java                # 实体类：username/password + equals/hashCode
    │   ├── UserStore.java           # 数据文件路径解析 + 读写（204 行），无硬编码路径
    │   └── userinfo.txt             # 遗留数据（明文占位，仅作为首次建库的迁移源）
    └── test/                        # 两个 Swing 事件练习
        ├── Test1.java               # 一个 603×680 的窗口 + 一个按钮
        └── MyActionLinstener.java   # ActionListener 实现，点击打印「按钮被点击」
```

## 学习路线 / 说明

这个仓库的学习价值集中在三个「从零手写 GUI」的常见环节，按依赖顺序读代码会比较顺：

1. **窗体搭建**（`LoginJFrame.initJFrame` / `GameJFrame.initJFrame`）：`setSize`、`setLocationRelativeTo(null)` 居中、`setAlwaysOnTop(true)`、`setLayout(null)` + `setBounds` 绝对定位。这是 Swing 教学里最典型的一套起手式，代价是窗口不能缩放。
2. **事件监听三种写法**（`month` 之外的三处对照着看）：
   - 鼠标：`LoginJFrame implements MouseListener`，用 `e.getSource() == login` 区分按钮；
   - 键盘：`GameJFrame implements KeyListener`，用 `e.getKeyCode()` 区分方向键；
   - 动作：`MyActionLinstener implements ActionListener`（`test/` 目录的独立小 demo），以及 `GameJFrame` 里用同一个 `this` 处理四个菜单项。
3. **面板重绘**（`GameJFrame.initImage`）：`removeAll()` → 逐个 `add(JLabel)` → `repaint()`。图片已改成构造时预加载（不再每帧读盘），但这套「每帧全量重建 `JLabel`」的做法能跑而效率一般，正好可以当反面教材改成「只更新变化的两个 `JLabel`」。
4. **组件 + 业务的最小组合**（`LoginJFrame` / `RegisterJFrame` + `User` + `UserStore`）：把「表单校验 → 实体对象 → 落盘 → 回读比对」串了一遍，是把 Swing 和集合/IO 结合起来的第一步；`UserStore` 那 200 行还顺便演示了「配置优先级 + 自动建目录 + 首次建库 + 失败回退」这套惯用写法。

如果要继续做，比较自然的方向：改成 3×3/5×5 可选难度（把 `data` 和 `win` 的维度参数化）、加计时器、加图片选择（用 `all.png` 现切 9/16/25 块）、把 `userinfo.txt` 换成本地 SQLite、把绝对布局换成 `GridLayout`。

## 已知问题

> 保留历史问题原文；已修掉的条目在末尾标 **（已修复）** 并说明新做法，未标记的仍然存在。汇总见文末「修复记录」。

1. **用户文件用了写死的 Windows 绝对路径（最严重）**：`LoginJFrame.java:37` 和 `RegisterJFrame.java:91` 都是
   `"D:\\desktop\\java\\puzzlegame\\src\\com\\wjh\\ui\\userinfo.txt"`。
   在 macOS / Linux 上这个路径不存在，`App` 启动即抛 `IORuntimeException: File not exist`，**游戏完全进不去**；在 Windows 上换个人 clone 也一样。应改成相对路径 `src/com/wjh/ui/userinfo.txt`（与 `GameJFrame` 里 `img/...` 的写法保持一致），或放到 `resources/` 下用 `getResource` 读。**（已修复）** 两处硬编码已全部删除，改由新增的 `UserStore` 解析数据文件路径：`-Dpuzzlegame.data` → 环境变量 `PUZZLEGAME_DATA` → `<cwd>/data/userinfo.txt` → 工程根 `data/userinfo.txt`，首次运行自动建目录建文件（默认账号 `admin/123456`，若有遗留 `src/com/wjh/ui/userinfo.txt` 则先迁移其中账号）。macOS / Linux / Windows 三平台通用，不需要再改任何路径。
2. **注册页的「两次密码一致」校验失效**：`RegisterJFrame.java:84` 写的是
   `password.getText().equals(reset.getText())`——`reset` 是「重置」**按钮**，它的 `getText()` 是 `"重置"`。所以这个条件只有在密码恰好等于「重置」时才成立，真正的「两次输入不一致」永远不会被拦下，用户能注册成功但密码是第一次输入的那个。**（已修复）** 改为真正比较两个输入框：`password.getText().equals(repassword.getText())`，不一致时弹「两次密码不一致」并中止。
3. **注册页的空值校验写重复了**：`RegisterJFrame.java:82` 的
   `username.getText().isEmpty() || password.getText().isEmpty() || password.getText().isEmpty()`
   第三个条件应该是 `repassword.getText().isEmpty()`，现在「确认密码」留空也能通过。**（已修复）** 三项分别判 `name` / `psd` / `repsd`（`repassword.getText()`），任一为空就弹「用户名或密码不能为空」。
4. **用户名重复校验失效**：`RegisterJFrame.java:86` 调用 `containsname(username.getName())`。`JTextField.getName()` 取的是**组件名**（没 `setName` 过就是 `null`），不是输入的内容，应该用 `getText()`。结果 `containsname(null)` 恒为 `false`，同名用户可以无限注册。**（已修复）** 改为 `containsname(username.getText().trim())`，同名时弹「用户名已存在」。
5. **`img/CS (0).png` 不存在**：`data` 的取值范围是 `0..15`，`initImage()` 统一拼 `"CS (" + num + ".png"`，但 `img/` 里只有 `CS (1)` ~ `CS (16)`。值为 `0`（也就是空位）的那一格会得到一个 `null` 图标的 `JLabel`——界面上表现为「空格子」，恰好符合预期，但这是**靠副作用实现的**，换个 `ImageIcon` 的加载行为（比如加了 `setIcon` 前的空判断）就会露馅。建议显式判断 `num == 0` 时跳过绘制，或者补一张透明的 `CS (0).png`。**（已修复，采用前者）** `initImage()` 里 `if (num == 0) continue;` 显式跳过空位绘制，不再依赖不存在的 `CS (0).png` 造成的 null 图标；`img/` 也没有为此新增文件。
6. **洗牌不保证有解**：`initData()` 只是随机交换 16 次（还不是标准的 Fisher–Yates，分布有偏），**没有检查逆序数的奇偶性**。4×4 滑块拼图只有一半的初始局面是可还原的，所以大约有 50% 的概率一开局就是死局，玩家怎么推都拼不出来（除了按 `W` 作弊）。正确做法是：先算 `data` 的逆序数（忽略 0），判断 `逆序数 + 空位所在行号` 的奇偶性，不满足就重新洗。**（已修复）** 洗牌改为标准 Fisher–Yates（每种排列等概率），并加 `isSolvable()` 校验：`(逆序数 + 空位自底向上行号) % 2 == 1` 为可解，不可解就重洗。判据用 2×4 八数码全状态空间穷举（40320 个排列，0 不一致）、4×4 十万局统计与 IDA* 复核验证过。
7. **`W` 键是后门**：直接 `data = win` 一键通关，还会把 `step` 停在原值。发布版应删除，或改成需要输入的调试开关。**（保留未改）** 代码逻辑保持不变，只在分支上加了 `// TODO: 发布前删除调试后门` 注释；按现状：按 `W` 立刻变成胜利布局，`step` 停在原值，随后 `victory()` 为真、方向键被锁死。
8. **`.idea/` 和 `puzzlegame.iml` 已入库**：`.gitignore` 里没有 `.idea/` 和 `*.iml`，所以 `.idea/workspace.xml`（IDE 的个人工作区状态，129 行）也被提交了。这类文件每次开 IDEA 都会变，会让 diff 一直变脏；`workspace.xml` 里通常还会带本机路径和断点信息。（未修复）
9. **`userinfo.txt` 明文存密码**（已处理）：格式是 `username=<用户名>&password=<明文密码>`，没有任何加密。仓库里原先存的几条记录和开发者本人的真实密码习惯高度相似，已完成两件事：① 记录已全部替换为占位数据（`admin` / `test` / `123456`）；② 如果那个密码在别处还在用，请去改掉——**git 历史里的旧内容不会因为这次替换而消失**。该文件是游戏运行时的「数据库」，必须随仓库存在，所以没有加进 `.gitignore`；如果以后你想让它只在本地存在，用 `git rm --cached src/com/wjh/ui/userinfo.txt` 取消跟踪即可。**（部分修复）** 现在运行时数据库是 `data/userinfo.txt`（`UserStore` 生成），`.gitignore` 已新增 `data/`，运行时数据不再入库；仓库里的 `src/com/wjh/ui/userinfo.txt` 只剩占位账号、且只作为首次建库的迁移源。明文存储这件事本身没变。
10. **没有构建脚本**：不是 Maven / Gradle 工程，`lib/` 里的 jar 只能靠 IDEA 的 `.iml` 挂载，命令行必须手写 `-cp "lib/*"`，在 macOS 上还要注意 classpath 分隔符是 `:` 而不是 Windows 的 `;`。**（部分缓解）** 仍然没有构建脚本，但代码已不依赖任何第三方 jar（`UserStore` 用 JDK 自带 `java.nio.file`），`javac` 不带 `-cp` 即可全量编译通过；`-cp "lib/*"` 写成习惯也无害。
11. **`commons-io-2.15.0.jar` 未被使用**：`src` 里没有 `org.apache.commons` 的引用，白白多占 500 KB。**（范围扩大）** 现在 `hutool-all-5.8.23.jar` 也没人用了：`FileUtil.writeLines` 换成 `UserStore.save()` 的 `Files.write` 后，`src` 里 `cn.hutool` / `org.apache.commons` 双零引用，两个 jar 都可以删。
12. **重绘效率低**：每次按键都 `removeAll()` 再新建 16 个 `JLabel` + 16 次 `new ImageIcon(...)`（每次都从磁盘读 PNG），移动手感会有可感知的延迟。把 16 个 `ImageIcon` 在 `initData` 阶段缓存进数组即可，或者干脆只移动两个格子的图标。**（一半修复）** 16 张切片在构造时预加载进 `tileIcons[]`（`all.png` / `bg.png` / `win.png` 也各只读一次盘），不再每帧解码 PNG；但「每帧 `removeAll()` + 重建 16 个 `JLabel`」仍在，要彻底顺滑还得改成只移动两个格子。
13. **`test/` 目录是练习残留**：`Test1` + `MyActionLinstener` 是一个独立的事件监听 demo，和游戏无关，可以移到 `examples/` 或删除。另外类名拼写是 `MyActionLinstener`（少一个 `i`，应为 `Listener`）。（未修复）
14. **`App` 里 `new GameJFrame()` 被注释**：想直接调试游戏（跳过登录）时得手动改代码；建议加一个 `--skip-login` 之类的开关或在 README 里说明。（未修复）
15. **窗体关闭行为不一致**：`LoginJFrame` / `RegisterJFrame` 用 `EXIT_ON_CLOSE`（常量 `3`），`GameJFrame` 用的是数字 `3`，`Test1` 用的是 `WindowConstants.EXIT_ON_CLOSE`——同一个意思三种写法，`GameJFrame` 里的魔法数字 `setDefaultCloseOperation(3)` 可读性最差。（未修复）

## 修复记录

| 文件 | 修了什么 | 现在的做法 |
| --- | --- | --- |
| `src/com/wjh/ui/UserStore.java`（新增） | 两处硬编码 `D:\desktop\...\userinfo.txt`，非 Windows 直接 `IORuntimeException` | 集中解析数据文件：`-Dpuzzlegame.data` → `PUZZLEGAME_DATA` → `<cwd>/data/userinfo.txt` → 工程根 `data/userinfo.txt`；首次运行自动建目录建文件，默认账号 `admin/123456`，有遗留文件则迁移账号；读写失败回退默认账号而不是崩 |
| `src/com/wjh/ui/GameJFrame.java` | 洗牌是随机交换 16 次（分布有偏、且 50% 概率开局死局） | 标准 Fisher–Yates + `isSolvable()` 校验（`(逆序数 + 空位自底向上行号) % 2 == 1`），不可解重洗 |
| `src/com/wjh/ui/GameJFrame.java` | 16 张 `ImageIcon` 每次重绘都重新读盘 | 构造时预加载进 `tileIcons[]`，`all` / `bg` / `win` 也各只读一次 |
| `src/com/wjh/ui/GameJFrame.java` | 空位靠不存在的 `CS (0).png` 生成 null 图标 | `num == 0` 显式 `continue` 跳过绘制 |
| `src/com/wjh/ui/RegisterJFrame.java` | 用户名查重用了 `getName()`（组件名，恒 null） | 改为 `getText().trim()` |
| `src/com/wjh/ui/RegisterJFrame.java` | 空值校验把 `password` 判了两次，确认密码留空也能过 | 第三个条件改为 `repassword.getText().isEmpty()` |
| `src/com/wjh/ui/RegisterJFrame.java` | 两次密码比较的是「重置」按钮的文本 | 改为 `password.getText().equals(repassword.getText())` |
| `.gitignore` | 运行时数据未忽略 | 新增 `data/` |
| `src/com/wjh/ui/GameJFrame.java` | `W` 键一键通关后门 | **保留未删**，只加 `// TODO: 发布前删除调试后门` 注释 |
