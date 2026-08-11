package com.soundfix;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * 游戏内设置（Cloth Config AutoConfig）。
 * 生成 config/soundfix.json，配置数据由 AutoConfig 持久化。
 * 依赖 Cloth Config，缺失时本类不会被加载，各读取点均有 try-catch 兜底。
 */
@Config(name = "soundfix")
public class SoundFixConfig implements ConfigData {
    /**
     * 枪械音效隔离：
     * 开启：枪械音效将不受到此mod的声音修复行为影响；
     * 关闭：枪械将受到和刀一致的声音打断。
     * 默认开启。
     */
    @ConfigEntry.Gui.Tooltip
    public boolean gunSoundIsolation = true;
}
