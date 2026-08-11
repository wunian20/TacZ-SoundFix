package com.soundfix;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SoundFixMod.MOD_ID)
public class SoundFixMod {
    public static final String MOD_ID = "soundfix";

    public SoundFixMod() {
        // 注册游戏内配置（依赖 Cloth Config；未安装时静默跳过，mod 仍可运行）
        try {
            AutoConfig.register(SoundFixConfig.class, GsonConfigSerializer::new);
        } catch (Throwable ignored) {}
        NeoForge.EVENT_BUS.register(new ClientTickHandler());
    }
}
