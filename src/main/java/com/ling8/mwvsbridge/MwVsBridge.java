package com.ling8.mwvsbridge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(MwVsBridge.MOD_ID)
public final class MwVsBridge {
    public static final String MOD_ID = "mianbaos_modernwarfare_vs2_compat";

    public MwVsBridge() {
        MinecraftForge.EVENT_BUS.register(new ServerEvents());
    }
}
