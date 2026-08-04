package com.soundfix;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SoundFixMod.MOD_ID)
public class SoundFixMod {
    public static final String MOD_ID = "soundfix";

    public SoundFixMod() {
        NeoForge.EVENT_BUS.register(new ClientTickHandler());
    }
}
