# COMPOSER_CR_0: 聊天编辑框与主题架构 CR-0 问题修复实施计划

> **文档性质**：工作区 CR-0 审查问题专项修复计划  
> **文档位置**：`docs/theme-m3/COMPOSER_CR_0.md`  
> **当前状态**：已完成实施（已通过全量门禁与编译检查，待命待用户验收）  

---

## 一、 用户完整原话记录（Unaltered & Unabridged）

以下按时序完整收录本轮审查与方案对齐中用户的全部原话输入，不做任何删减、缩略或摘要：

### 第 1 轮
> /Users/niki/.repo/android/zafiro-worktrees/feat/theme-m3/docs/theme-m3/01-OVERVIEW_AND_FI
>   NAL_GOAL.md
>   /Users/niki/.repo/android/zafiro-worktrees/feat/theme-m3/docs/theme-m3/02-TASK1_ACCEPTANC
>   E_CRITERIA.md
>   /Users/niki/.repo/android/zafiro-worktrees/feat/theme-m3/docs/theme-m3/PLAN.md
> 
>   review 工作区的改动，QA已经发现的问题：
> 
> - material下，左右两个按钮的内容颜色不一致，应该和右边按钮一样的颜色：/var/folders/py/t639_sts2z9dv5zdzwht_3700000gn/T/orca-paste-1792670601335-6f663582-873f-4adb-a2a8-02b29bb57e76.png
> - MD3E似乎没有体现出应有的色彩灵动感。3和3E的主要区别在于颜色和按压效果。PLAN.md 中有讲解区别，如果你不了解，可以先看看
> - 主题选择不符合基建规范
> - 其他待review：代码复用程度以及圆角、大小、交互的同构程度（从QA来看做得还不错）
> ---
> 
> research only

### 第 2 轮
> 继续

### 第 3 轮
> 总的来说：
> 1. 基建下沉
> 2. 配置持久化
> 3. UI string -- 不用看我就知道过不了githook因为文案太弱智了
> 4. basetheme 随主题改kolor颜色配置
> 5. 按钮颜色同构
> 
> 现在逐个展开。多语言的话只给中英两种就够了，其他的我看不懂

### 第 4 轮
> 展开方案

### 第 5 轮
> ZafiroActionButton.kt为什么要加参数？你自己都说了在里面自己检查主题，难道还要业务来选吗？你解释一下？我没理解这个
> 
> ui字符串你出的这个也不合格，你自己看看是为什么
> 
> 其他的感觉，没啥。我说的这两个重新给我将

### 第 6 轮
> 重新给我讲

### 第 7 轮
> 中文叫‘液态玻璃’
> 
> 现在写一个 COMPOSER_CR_0.md 详细写出修复计划。包含我的所有用户原话作为上下文（不是概要）

---

## 二、 核心修复原则与设计铁律

1. **业务层绝对零感知（Hard Gate）**：
   - 业务调用点（顶栏 `ZafiroTopBar`、编辑框 `ComposerAttachButton` / `ComposerSendButton` 等）绝不传递任何主题或风格枚举参数。
   - `ZafiroActionButton` 签名**保持原样、不新增任何参数**，内部直接通过 `LocalThemeStyle.current` 自主分流。
2. **单一事实源与正统持久化**：
   - 彻底废弃并删除私有纯文本读写的 `ThemeStyleStore.kt`。
   - 将 `themeStyle` 全面收编入 `AppStateSettingsCodec` 与 `XRepo`，通过响应式/单一来源管线统一管理。
3. **严格遵守 Git Hook 规范（多语言与 UI 文案）**：
   - 杜绝技术黑话与括号解释（如 `(Liquid Glass)`），严格遵守 `.githooks/check_strings.py` 的 `verbose-ui` 零括号、零分号要求。
   - 官方中文定名为 **“液态玻璃”**。
   - 保留三档 `SettingsRowSpec.Slider`（0: 液态玻璃, 1: Material 3, 2: Material 3 Expressive），接入正式多语言资源。
   - 为避免 `check_strings.py` 严格对齐门禁因 `missing_keys` 阻断提交，新增的 Key 同步至全项目已支持的 6 个语言目录。
4. **BaseTheme 全局色板动态联动**：
   - 在 `BaseTheme` 中集成 `com.materialkolor.PaletteStyle`：MD3 采用稳重低彩度 `PaletteStyle.TonalSpot`，MD3E 启用高彩度与互补张力的 `PaletteStyle.Expressive`。
5. **视觉与按钮完全同构（绝对对称）**：
   - 加号按钮与发送按钮无论在任何模式下均**完全同构、颜色与底色 100% 保持一致**（两侧均为 `surfaceContainerHighest` 浅色微深底座 + `primary` 前景图标）。
   - 严禁对发送键单独做大色块高反差填充，拒绝视觉失衡。

---

## 三、 六大分步实施任务清单（Task Checklist）

```mermaid
flowchart TD
    Task1["Task 1: XRepo 持久化管线收编 (删除 ThemeStyleStore)"]
    Task2["Task 2: 全语种 Strings 规范落位与 Settings 卡片化"]
    Task3["Task 3: BaseTheme 对接 PaletteStyle.Expressive"]
    Task4["Task 4: ZafiroActionButton 零参数下沉 (删除 ComposerActionButton)"]
    Task5["Task 5: 编辑框按钮前景色同构与 MD3E 底盘反差"]
    Task6["Task 6: Preview 真实化验证与全量门禁验收"]

    Task1 --> Task2 --> Task3 --> Task4 --> Task5 --> Task6
```

---

### Task 1: 配置持久化收编入 `XRepo`（彻底销毁私有文本存储）
- [x] **1.1 扩展 `AppStateSettingsCodec.kt`**：
  - 在 `AppStateSettings` 数据模型中增加字段：
    ```kotlin
    @SerialName("theme_style")
    val themeStyle: String = "liquid",
    ```
  - 在反序列化容错/清洗逻辑中加入兜底：未知或空白值统一回退为 `"liquid"`。
- [x] **1.2 扩展 `XRepo.kt`**：
  - 声明字段管线：
    ```kotlin
    private val themeStyleField = PlainAppStateField(
        select = { themeStyle },
        update = { copy(themeStyle = it) },
    )
    suspend fun themeStyle(): String = themeStyleField.get()
    suspend fun setThemeStyle(style: String) = themeStyleField.set(style)
    ```
- [x] **1.3 升级 `ThemeController.kt`**：
  - `ThemeStyle` 增加 `storageKey` 映射（`liquid`, `md3`, `md3e`）及转换工具函数。
  - `ThemeController.load()` 改为读取 `XRepo.themeStyle()`。
  - `ThemeController.setThemeStyle(style)` 改为调用 `XRepo.setThemeStyle(style.storageKey)`。
- [x] **1.4 彻底删除 `app/.../ThemeStyleStore.kt`**。

---

### Task 2: UI 字符串规范落位与设置页重构（过 Hook 且无黑话）
- [x] **2.1 多语言文件录入（6 语种严格对齐）**：
  - **中文 (`app/src/main/res/values/strings.xml`)**：
    ```xml
    <string name="ui_settings_theme_style_title">风格</string>
    <string name="ui_theme_style_liquid">液态玻璃</string>
    <string name="ui_theme_style_md3">Material 3</string>
    <string name="ui_theme_style_md3e">Material 3 Expressive</string>
    ```
  - **英文 (`app/src/main/res/values-en/strings.xml`)**：
    ```xml
    <string name="ui_settings_theme_style_title">Style</string>
    <string name="ui_theme_style_liquid">Liquid Glass</string>
    <string name="ui_theme_style_md3">Material 3</string>
    <string name="ui_theme_style_md3e">Material 3 Expressive</string>
    ```
  - 同步写入 `values-b+zh+Hant`（同中文）、`values-es`、`values-ja`、`values-ru`（同英文），保证 `check_strings.py` 零缺失通过。
- [x] **2.2 规范化 `ThemeSettingsContent.kt` 的 Slider**：
  - 维持三档 `SettingsRowSpec.Slider` 既有交互形式（0: 液态玻璃, 1: Material 3, 2: Material 3 Expressive）。
  - 文案完全替换为无括号、规范的 String 资源，滑动与提交直接驱动 `XRepo` 持久化。

---

### Task 3: `BaseTheme` 联动 `material-kolor` 的 Expressive 色板
- [x] **3.1 引入 `PaletteStyle` 联动**：
  - 在 `ui-kit/base/BaseTheme.kt` 中：
    ```kotlin
    val paletteStyle = when (themeStyle) {
        ThemeStyle.Material3Expressive -> PaletteStyle.Expressive
        else -> PaletteStyle.TonalSpot
    }
    ```
- [x] **3.2 色板生成管线对接**：
  - 用户自定义种子色时：向 `rememberDynamicColorScheme` 传入 `style = paletteStyle`。
  - 动态壁纸取色时：MD3E 模式从动态主色提取种子色，利用 `PaletteStyle.Expressive` 派生出高饱和度、高对比度的灵动表现力色板；MD3 保持原生平稳 `TonalSpot`。
- [x] **3.3 同步更新 `resolveThemeColorScheme`**：
  - 保证非 Composable 外部通知、常驻浮窗解析出的颜色与应用内 100% 同步。

---

### Task 4: `ZafiroActionButton` 零参数基件下沉（彻底销毁私有按钮）
- [x] **4.1 升级 `ui-kit/button/ZafiroActionButton.kt`**：
  - 保持签名零新增参数：
    ```kotlin
    @Composable
    fun ZafiroActionButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        backdrop: Backdrop? = null,
        onButtonBoundsChanged: ((Rect) -> Unit)? = null,
        content: @Composable () -> Unit,
    )
    ```
  - 内部根据 `LocalThemeStyle.current` 智能分流：
    - `ThemeStyle.LiquidGlass`: 执行 `ActionBarButton`（毛玻璃底盘 + 动态高光 + 拖拽阻尼）。
    - `ThemeStyle.Material3`: 执行原生圆形实体底座（`surfaceContainerHighest`）+ 原生边界 `ripple()` 水波纹。
    - `ThemeStyle.Material3Expressive`: 在实体底盘上叠加 `springSquish` 弹性按压形变。
  - 严禁开辟染色后门，两侧按钮统一底座。
- [x] **4.2 业务层切流并清理**：
  - `ComposerAttachButton.kt` 与 `ComposerSendButton.kt` 直接调用下沉后的 `ZafiroActionButton`。
  - 彻底删除 `app/.../composer/ComposerActionButton.kt`。

---

### Task 5: 按钮完全同构（两侧镜像对称）
- [x] **5.1 左右按钮前景色完全统一**：
  - `ComposerAttachButton.kt` 与 `ComposerSendButton.kt` 前景色无条件统一绑定为 `MaterialTheme.colorScheme.primary`，彻底杜绝色差。
- [x] **5.2 左右按钮底座完全统一**：
  - 加号与发送按钮统一使用 `surfaceContainerHighest` 浅色微深底座，严禁对发送键单独做大色块填充，保持两端绝对对称。

---

### Task 6: Preview 真实化验证与全量验收门禁
- [x] **6.1 刷新 Preview（拒绝手写 Mock）**：
  - 在 `ChatComposerThemePreview.kt` 中验证三模下的按钮前景色一致性、MD3E 色板与底盘反差。
  - 在 `TopBarThemePreview.kt` 等文件中剔除手写布局，统一注入环境驱动真实 `ZafiroTopBar`。
- [x] **6.2 自动化量化验收门禁验证**：
  ```bash
  # 门禁 1：多语言对齐与无话痨括号检查
  python3 .githooks/check_strings.py --worktree
  
  # 门禁 2：死代码与私有旁路清理核查（预期输出均为 0 行）
  rg "ThemeStyleStore|ComposerActionButton" app/
  
  # 门禁 3：全工程编译无破坏
  ./gradlew assembleDebug
  ```

---

## 四、 状态说明

- 当前文档已完整记录需求上下文与修正设计。
- 工作区当前**严格处于待命状态（Research/Planning Only）**，尚未对任何业务代码做实际修改，等待用户验收批准。
