package com.soundfix;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SoundFixMod.MOD_ID)
public class SoundFixMod {
    public static final String MOD_ID = "soundfix";

    public SoundFixMod() {
        // 注册配置数据持久化（依赖 Cloth Config；未安装时静默跳过，mod 仍可运行）
        try {
            AutoConfig.register(SoundFixConfig.class, GsonConfigSerializer::new);
        } catch (Throwable ignored) {}
        // 手动注册 Mod 列表"配置"按钮（NeoForge 21.1 的 IConfigScreenFactory 扩展点；
        // Cloth Config 15.0.140 的自动注册与新版 NeoForge 不兼容，故手动注册）
        try {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> ModConfigScreen::createScreen);
        } catch (Throwable ignored) {}
        NeoForge.EVENT_BUS.register(new ClientTickHandler());
    }
}
