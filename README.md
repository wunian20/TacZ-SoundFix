# TacZ SoundFix

修复 TaCZ 模组的检视音效问题。

**功能**
- 切武器、攻击、开镜打断检视时，音效立即停止
- 连续按检视键音效不叠加
- 冲锋枪 NBT 变化误停音效修复

**原理**
通过反射调用 `SoundPlayManager.stopAndClearTrackedSounds()`，不修改 TaCZ 源文件。

**依赖**
- Minecraft 1.20.1 + Forge 47+
- TaCZ 1.1+（可选，不装也能进游戏但无效果）

**测试**
- 已测试 DeltaForce Melee Pack-0.2.0，安装此 mod 后无检视音效叠加 bug
- 欢迎反馈 bug

**鸣谢**
- TaCZ 全体开发成员
- lrtactical 模组
- DeltaForce Melee Pack 刀包作者
