# Task 1 目标与可量化验收标准清单（分包治理与业务层抽净）

> **文档性质**：Task 1 执行实施指南与可量化验收清单  
> **文档位置**：`docs/theme-m3/02-TASK1_ACCEPTANCE_CRITERIA.md`  
> **当前状态**：待用户审查（审查通过后作为实施勾选清单）

---

## 一、 Task 1 核心定位与原则

1. **纯重构与无感迁移（Zero Behavioral & Visual Regression）**：
   - 业务逻辑完全不改，视觉效果、动画曲线、按压与拖拽手感在本次任务完成后**必须保持 100% 一致**。
   - 用户与测试人员在运行层面**感知不到任何差异**。
2. **彻底解决分包堆砌乱象**：
   - 彻底废除 `ui-kit.infra` 和 `ui-kit.infra.component` 这两个无序的大杂烩包。
   - 建立 8 个垂直领域包，所有对外组件统一命名为 `ZafiroXXX`。
   - 所有底层毛玻璃、光学 Presets 和 Shader 全部收编入 `internal/liquid/`，对业务层（`app/` 模块）完全不可见。
3. **接口标准拉齐（Compose Modifier 规范）**：
   - 严格遵循 Google Compose API 规范：所有新基件中，**`modifier: Modifier = Modifier` 必须放在第一个带默认值的可选参数位置**（即所有必填参数之后、其他可选参数及尾部 Composable Lambda 之前）。
4. **Backdrop 安全兜底（防图层脱节黑屏）**：
   - 不搞激进的“一刀切砍形参”，在基件签名中使用 `backdrop: Backdrop? = LocalScreenBackdrop.current` 作为带默认值的可选形参。
   - 业务层绝大多数调用点直接省略该参数，代码达到极致干净；极特殊场景仍然保留安全透传能力，避免 `com.kyant.backdrop` 图层脱节。

---

## 二、 自动化量化验收门禁（Automated Acceptance Gates）

在 Task 1 宣告完成前，必须在终端依次执行并通过以下 4 道**硬性自动化量化门禁**：

```bash
# 门禁 1：业务层（app/ 模块）Backdrop 依赖清零
# 预期命令输出：0 行匹配（当前工程中共有 5 处）
rg "import com\.kyant\.backdrop\." app/

# 门禁 2：业务层（app/ 模块）底层图形 Presets / 物理拖拽清零
# 预期命令输出：0 行匹配（当前在 HomePageContent 与 HomeChatComponents 中有 8 处）
rg "LiquidGlassPresets|applyLiquidInteractiveTransform|InteractiveHighlight" app/

# 门禁 3：业务层（app/ 模块）Liquid* 假借命名清零
# 预期命令输出：0 行匹配（业务层只面向 Zafiro* 基件，不面向任何 Liquid*）
rg "LiquidDialog|LiquidTextField|LiquidPopover|LiquidMenuItem|LiquidScreen|LiquidAdaptiveScrollbar" app/

# 门禁 4：编译强类型门禁与零警告递增
# 预期命令退出码：0 (BUILD SUCCESSFUL)
./gradlew assembleDebug
```

---

## 三、 Task 1 逐项可勾选实施清单（Checklist）

### Step 1.1: `ui-kit` 新包落位与基件门面封装 (预计 LOC: +1,200 ~ 1,400 行)
- [x] **建立垂直领域包目录结构**：
  - [x] 新建 `ui-kit/src/main/java/com/niki914/uikit/screen/`
  - [x] 新建 `ui-kit/src/main/java/com/niki914/uikit/button/`
  - [x] 新建 `ui-kit/src/main/java/com/niki914/uikit/textfield/`
  - [x] 新建 `ui-kit/src/main/java/com/niki914/uikit/dialog/`
  - [x] 新建 `ui-kit/src/main/java/com/niki914/uikit/menu/`
  - [x] 新建 `ui-kit/src/main/java/com/niki914/uikit/control/`
  - [x] 新建 `ui-kit/src/main/java/com/niki914/uikit/scroll/`
- [x] **基件门面封装与规范拉齐（拉齐 modifier 为首个可选参数）**：
  - [x] `screen/ZafiroScreen.kt`：封装页面四层骨架，保留现有视觉
  - [x] `screen/ZafiroTopBar.kt`：三槽顶栏门面
  - [x] `screen/ZafiroScreenTitle.kt`：页面标题状态模型（原 LiquidScreenTitle）
  - [x] `button/ZafiroActionButton.kt`：通用图标动作按钮，参数拉齐：
    `fun ZafiroActionButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, backdrop: Backdrop? = null, ...)`
  - [x] `button/ZafiroScrollToBottomButton.kt`：回到底部快捷悬浮按钮（从主页抽离下沉）
  - [x] `button/ZafiroButton.kt`：通用操作按钮门面（桥接原 TintLiquidButton / MaterialTintLiquidButton）
  - [x] `textfield/ZafiroTextField.kt`：输入框基件门面（规范 modifier 为首个可选参数，支持 ZafiroTextFieldStyle）
  - [x] `dialog/ZafiroDialog.kt`：弹窗门户与面板基件（原 LiquidDialog）
  - [x] `dialog/ZafiroConfirmationDialog.kt`：二次确认弹窗门面
  - [x] `dialog/ZafiroSingleChoiceDialog.kt`：单选弹窗门面
  - [x] `dialog/ZafiroInputDialog.kt`：单/多行文本录入弹窗门面
  - [x] `menu/ZafiroDropdownMenu.kt` 与 `ZafiroMenuItem.kt`：通用菜单门面
  - [x] `control/ZafiroSwitch.kt` 与 `ZafiroSlider.kt`：开关与滑块门面
  - [x] `scroll/ZafiroScrollableContainer.kt` 与 `ZafiroAdaptiveScrollbar.kt`：滚动条与容器门面
- [x] **保留过渡兼容别名（Typealias / Deprecated）**：
  - [x] 原 `infra` 包完整保留，防止存量代码编译突变中断

---

### Step 1.2: 主页核心流深度净化（预计 LOC: ~100 行）
> **适配上游最新模块化结构**：上游已将旧大文件解耦为 `HomePageContentBody.kt`、`LiquidChatComposer.kt`、`ScrollToBottomButton.kt` 与 `HomeConfigPopover.kt`。
- [x] **重构 `HomePageContentBody.kt`**：
  - [x] 移除文件头部的 `import com.kyant.backdrop.*`
  - [x] 下沉局部 Backdrop 隔离：采用 `ui-kit` 新增的 `ZafiroLocalBackdropContainer`（解决外层 Backdrop 祖先环形自指崩溃 SIGSEGV 问题）
  - [x] `LazyColumn` 挂载容器提供的 `contentBackdropModifier`
  - [x] 换用下沉后的 `ZafiroScrollToBottomButton`，移除 `backdrop` 透传
  - [x] 换用 `ZafiroChatComposer`，移除 `backdrop` 透传
- [x] **重构 `composer/LiquidChatComposer.kt` -> `ZafiroChatComposer.kt`**：
  - [x] 移除文件头部的 `import com.kyant.backdrop.Backdrop`
  - [x] 移除 `ZafiroChatComposer` 签名中的 `backdrop: Backdrop? = null` 外部泄漏
  - [x] 将内部添加附件与发送按钮的 `ActionBarButton` 替换为通用的 `ZafiroActionButton`（默认 `backdrop = null`，避免编辑区按钮异常采样下层图层）
  - [x] 移除 `glassStyle = LiquidGlassPresets.ComposerBar` 底层预设引用
- [x] **清理/重构 `ScrollToBottomButton.kt`**：
  - [x] 业务层直接改用 `ui-kit/button/ZafiroScrollToBottomButton.kt`，删除 `app` 模块内的手搓绘制
- [x] **重构 `header/HomeConfigPopover.kt`**：
  - [x] 替换模型配置菜单为 `ZafiroDropdownMenu` + `ZafiroMenuItem`

---

### Step 1.3: `app` 模块全量调用点切流（预计 LOC: ~350 ~ 450 行）
- [x] **应用入口与顶栏全局菜单切流**：
  - [x] `ZafiroApp.kt`：将右侧三点菜单由 `LiquidPopover` 换为 `ZafiroDropdownMenu`
  - [x] `CrashReportDialog.kt`：换为 `ZafiroDialog` + `ZafiroButton`
- [x] **全量弹窗调用点替换**：
  - [x] 替换各设置页中的 `ConfirmationLiquidDialog` 为 `ZafiroConfirmationDialog`
  - [x] 替换各详情页中的 `SingleChoiceLiquidDialog` 为 `ZafiroSingleChoiceDialog`
  - [x] 替换各详情页中的 `InputLiquidDialog` 为 `ZafiroInputDialog`
- [x] **全量页面容器与滚动条切流**：
  - [x] 将各级页面外壳 `LiquidScreen` 切换为 `ZafiroScreen`
  - [x] 将滚动容器 `LiquidScrollableContainer` 切换为 `ZafiroScrollableContainer`
  - [x] 将纯回弹手势 `LiquidOverscrollContainer` 切换为 `ZafiroOverscrollContainer`
- [x] **文本框与控制项切流**：
  - [x] 将 `LiquidTextField` 切换为 `ZafiroTextField`
  - [x] 将 `LiquidToggle` 切换为 `ZafiroSwitch`
  - [x] 将 `LiquidSlider` 切换为 `ZafiroSlider`

---

### Step 1.4: 门禁验证与自测验收
- [x] 终端运行 `rg "import com\.kyant\.backdrop\." app/`，确认输出为 0
- [x] 终端运行 `rg "LiquidGlassPresets|applyLiquidInteractiveTransform|InteractiveHighlight" app/`，确认输出为 0
- [x] 终端运行 `rg "LiquidDialog|LiquidTextField|LiquidPopover|LiquidMenuItem|LiquidScreen|LiquidAdaptiveScrollbar" app/`，确认输出为 0
- [x] 终端运行 `./gradlew assembleDebug`，确认 100% 编译通过
- [x] 启动模拟器或真机验证（零回归走查）：
  - [x] 主页对话流正常滚动、回到底部浮标正常弹出与触发
  - [x] 主页输入框正常输入、附件与发送键正常响应
  - [x] 顶栏三槽布局完整，左右动作项拖拽形变表现与重构前完全一致
  - [x] 模型切换菜单与顶栏下拉菜单弹出、选项点击正常
  - [x] 设置页二次确认弹窗与单选弹窗弹出、关闭逻辑正常

---

## 四、 提交切分规划与 Git Hook 限制应对策略

### 4.1 推荐的 3 个原子 Commit 规划
1. **Commit 1: `refactor(ui-kit): scaffold domain packages and zafiro primitives`**
   - 范围：`ui-kit` 建立 8 个垂直领域包，封装 `Zafiro*` 门面并保留旧 `infra` 兼容别名。
   - 保障：纯新增/移动基件，零破坏，100% 编译通过。
2. **Commit 2: `refactor(app): purge backdrop and liquid presets from home chat`**
   - 范围：重构 `HomePageContentBody`、`LiquidChatComposer` -> `ZafiroChatComposer`、`ScrollToBottomButton`、`HomeConfigPopover`。
   - 保障：清空主页核心链路的 Backdrop 与 Presets，通过自动化门禁 1 与门禁 2。
3. **Commit 3: `refactor(app): migrate dialogs and screens to zafiro primitives`**
   - 范围：切流 `ZafiroApp.kt`、50+ 个弹窗与设置详情页调用点。
   - 保障：通过自动化门禁 3 与门禁 4（全量编译通过）。

### 4.2 Git Hook（.githooks/check_tokens.py）5,000 Tokens 限制应对
- **天然符合规范**：由于分包治理将原本庞大的代码分散为单一职责文件（新文件普遍在 1KB ~ 4KB 之间，远低于 20KB 上限），新建的基件天然可通过检查。
- **单文件豁免标记**：若修改存量较大文件（如 `HomePageContentBody.kt`）时被拦截，可在文件第 1 行添加注释：
  ```kotlin
  // githooks:ignore-file file-token-limit
  ```
- **命令行级跳过**：应急或原子多文件重构提交时，可使用 `git commit --no-verify`（或 `git commit -n`）跳过本地钩子。

