package me.xmrvizzy.skyblocker.config.modmenu;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import me.xmrvizzy.skyblocker.config.SkyblockerConfig;

@Environment(EnvType.CLIENT)
public class ModMenuEntry implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> AutoConfig.getConfigScreen(SkyblockerConfig.class, screen).get();
    }
}