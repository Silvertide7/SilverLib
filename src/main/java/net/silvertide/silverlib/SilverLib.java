package net.silvertide.silverlib;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SilverLib.MOD_ID)
public class SilverLib {
    public static final String MOD_ID = "silver_lib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SilverLib() {
        LOGGER.info("SilverLib loaded");
    }
}
