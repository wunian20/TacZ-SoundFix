# TacZ SoundFix

修复 TaCZ 模组的检视（inspect）音效问题：打断检视时音效不停止、连续检视音效叠加等。

## 版本分支

| 分支 | 游戏版本 | 加载器 |
|---|---|---|
| `main` | 1.20.1 | Forge 47+ |
| `neoforge/1.21.1` | 1.21.1 | NeoForge 21.1+ |

## 功能

- 检视中**切武器** → 检视音效立即停止（只停检视音效，不误伤新武器的切出/射击音效）
- 检视中**攻击/开镜** → 检视音效立即停止（仅检视后 8 秒内的点击生效，不影响正常战斗）
- **连续按检视键** → 音效不叠加
- 冲锋枪开火音效被 NBT 变化误停 → 只比较物品类型不比较 NBT
- **枪械音效隔离**（游戏内设置，默认开启）：枪械音效不受此 mod 的声音修复影响，切走/切回不打断枪声；近战（刀）音效仍执行停止逻辑

## 原理

不修改 TaCZ 源文件，全部通过反射操作 TaCZ 的 `SoundPlayManager`：

- 切武器/攻击/检视时，反射快照 `TRACKED_GUN_SOUNDS` 追踪池，只停止"上一 tick 就存在的旧音效"，保留本 tick 新播放的音效（新武器的 draw 切出音效）
- 枪械音效隔离开启时，切走前是枪会把当时追踪池中的音效实例标记为"枪的音效"，后续停止逻辑跳过这些实例，保证枪声在切走、切回时都不被打断
- 枪/刀判定：`IGun → TimelessAPI.getCommonGunIndex → CommonGunIndex.getType`，type 含近战特征词（melee/lrtactical/knife/blade/sword）判为刀

## 依赖

- `main`：Minecraft 1.20.1 + Forge 47+
- `neoforge/1.21.1`：Minecraft 1.21.1 + NeoForge 21.1+（对应 [TaCZ 1.21.1 NeoForge 移植版](https://modrinth.com/mod/tacz-1.21.1)）
- TaCZ 1.1+（可选，不装也能进游戏但无效果）
- **Cloth Config API**（可选，仅游戏内设置界面需要；不装时功能按默认值生效，只是没有设置界面）
  - `main`（Forge 1.20.1）：Cloth Config 11.x（`cloth-config-forge`）
  - `neoforge/1.21.1`（NeoForge 1.21.1）：Cloth Config 15.x（`cloth-config-neoforge`）

## 构建

`main` 用 JDK 17 toolchain，`neoforge/1.21.1` 用 JDK 21；Gradle 运行 JVM 在 `gradle.properties` 的 `org.gradle.java.home` 中配置：

```bash
./gradlew build
```

产物与版本：

| 分支 | 版本 | 产物 |
|---|---|---|
| `main` | v1.0.2 | `build/libs/TacZ SoundFix1.20.1-Forge-1.0.2.jar` |
| `neoforge/1.21.1` | v1.0.2 | `build/libs/TacZ SoundFix1.21.1-NeoForge-1.0.2.jar` |

文件名遵循 `TacZ SoundFix{游戏版本}-{加载器平台}-{版本号}.jar` 规范，版本号在 `gradle.properties` 的 `mod_version` 与 mod 元数据（`mods.toml` / `neoforge.mods.toml`）中同步维护。

## 测试

- 已测试 DeltaForce Melee Pack，安装此 mod 后无检视音效叠加 bug
- 针对 TaCZ 1.20.1 与 TaCZ 1.21.1 NeoForge 移植版（1.1.8-hotfix-r6）验证：
  - 切出（draw）音效不被误停（含快速连续切换）
  - 枪械音效隔离：开枪切走再切回，枪声完整不打断
  - 检视中切武器 / 攻击 / 开镜，检视音效立即停止
  - 连续检视不叠加
- 欢迎反馈 bug（提 issue 即可）

## 鸣谢

- TaCZ 全体开发成员
- [Unofficial TaCZ 1.21.1 NeoForge Port](https://modrinth.com/mod/tacz-1.21.1)（MUKSC）
- lrtactical 模组
- DeltaForce Melee Pack 刀包作者
