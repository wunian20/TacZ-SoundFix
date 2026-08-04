# TacZ SoundFix

修复 TaCZ 模组的检视（inspect）音效问题：打断检视时音效不停止、连续检视音效叠加等。

## 版本分支

| 分支 | 游戏版本 | 加载器 | 状态 |
|---|---|---|---|
| `main` | 1.20.1 | Forge 47+ | 原版 |
| `neoforge/1.21.1` | 1.21.1 | NeoForge 21.1+ | 移植版（当前） |

## 功能

- 检视中**切武器** → 检视音效立即停止（只停检视音效，不误伤新武器的切出/射击音效）
- 检视中**攻击/开镜** → 检视音效立即停止（仅检视后 8 秒内的点击生效，不影响正常战斗）
- **连续按检视键** → 音效不叠加
- 冲锋枪开火音效被 NBT 变化误停 → 只比较物品类型不比较 NBT

## 原理

不修改 TaCZ 源文件，全部通过反射操作 TaCZ 的 `SoundPlayManager`：

- **1.20.1（`main` 分支，Forge）**：直接反射调用 `stopAndClearTrackedSounds()`。
- **1.21.1（`neoforge/1.21.1` 分支，NeoForge）**：TaCZ 1.1.8 重构了声音系统，`stopAndClearTrackedSounds()` 全停会误伤新武器的切出（draw）音效，因此改为：
  1. 按检视键时（HIGH 优先级，TaCZ 处理前）全停旧音效，防止叠加；
  2. 反射快照 `TRACKED_GUN_SOUNDS` 追踪池，把检视后新入池的音效识别为"检视音效候选"；
  3. 切武器 / 攻击 / 开镜时**只对候选音效调用 `setStop()`**，完全不触碰其他音效。

## 依赖

- `main`：Minecraft 1.20.1 + Forge 47+
- `neoforge/1.21.1`：Minecraft 1.21.1 + NeoForge 21.1+（对应 [TaCZ 1.21.1 NeoForge 移植版](https://modrinth.com/mod/tacz-1.21.1)）
- TaCZ 1.1+（可选，不装也能进游戏但无效果）

## 构建

需要 JDK 21（`neoforge/1.21.1` 分支），JDK 路径在 `gradle.properties` 的 `org.gradle.java.home` 中配置：

```bash
./gradlew build
```

产物：`build/libs/TacZ SoundFix-1.0.0.jar`，放入游戏 `mods/` 目录即可。

## 测试

- 已测试 DeltaForce Melee Pack，安装此 mod 后无检视音效叠加 bug
- `neoforge/1.21.1` 分支针对 TaCZ 1.21.1 NeoForge 移植版（1.1.8-hotfix-r6）验证：
  - 切出（draw）音效不被误停（含快速连续切换）
  - 检视中切武器 / 攻击 / 开镜，检视音效立即停止
  - 连续检视不叠加
- 欢迎反馈 bug（提 issue 即可）

## 鸣谢

- TaCZ 全体开发成员
- [Unofficial TaCZ 1.21.1 NeoForge Port](https://modrinth.com/mod/tacz-1.21.1)（MUKSC）
- lrtactical 模组
- DeltaForce Melee Pack 刀包作者
