package com.soundfix;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(SoundFixMod.MOD_ID)
public class SoundFixMod {
    public static final String MOD_ID = "soundfix";

    public SoundFixMod() {
        // 注册配置数据持久化（依赖 Cloth Config；未安装时静默跳过，mod 仍可运行）
        try {
            AutoConfig.register(SoundFixConfig.class, GsonConfigSerializer::new);
        } catch (Throwable ignored) {}
        // 手动注册 Mod 列表"配置"按钮（Forge 1.20.1 的 ConfigScreenHandler.ConfigScreenFactory 扩展点）
        try {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(ModConfigScreen::createScreen));
        } catch (Throwable ignored) {}
        MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
    }
}
