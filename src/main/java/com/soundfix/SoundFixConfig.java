package com.soundfix;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * 游戏内设置（Cloth Config AutoConfig）。
 * 生成 config/soundfix.toml，并在 Mod 列表的"配置"按钮打开设置界面。
 * 依赖 Cloth Config，缺失时本类不会被加载，各读取点均有 try-catch 兜底。
 */
@Config(name = "soundfix")
public class SoundFixConfig implements ConfigData {
    /**
     * 枪械音效隔离：开启后，切走武器时若上一把是枪械，不停止其音效（枪声等保留播完）；
     * 近战武器（刀）仍执行停止逻辑。默认开启。
     */
    @ConfigEntry.Gui.Tooltip
    public boolean gunSoundIsolation = true;
}
