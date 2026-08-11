package com.soundfix;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Mod 列表"配置"按钮的界面。
 * 通过 ConfigScreenHandler.ConfigScreenFactory 扩展点注册（Forge 1.20.1 标准 API），
 * 界面由 Cloth Config 的 ConfigBuilder 构建，数据由 AutoConfig 持久化到 config/soundfix.json。
 */
public class ModConfigScreen {
    public static Screen createScreen(Screen parent) {
        try {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("TacZ SoundFix 设置"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            builder.getOrCreateCategory(Component.literal("通用"))
                    .addEntry(entryBuilder.startBooleanToggle(
                            Component.literal("枪械音效隔离"),
                            readIsolation())
                            .setDefaultValue(true)
                            .setTooltip(
                                    Component.literal("开启：枪械音效将不受到此mod的声音修复行为影响"),
                                    Component.literal("关闭：枪械将受到和刀一致的声音打断"))
                            .setSaveConsumer(ModConfigScreen::writeIsolation)
                            .build());
            return builder.build();
        } catch (Throwable t) {
            // Cloth 异常时回退到原界面，避免黑屏
            return parent;
        }
    }

    private static boolean readIsolation() {
        try {
            return AutoConfig.getConfigHolder(SoundFixConfig.class).getConfig().gunSoundIsolation;
        } catch (Throwable t) {
            return true;
        }
    }

    private static void writeIsolation(boolean value) {
        try {
            var holder = AutoConfig.getConfigHolder(SoundFixConfig.class);
            holder.getConfig().gunSoundIsolation = value;
            holder.save();
        } catch (Throwable ignored) {}
    }
}
