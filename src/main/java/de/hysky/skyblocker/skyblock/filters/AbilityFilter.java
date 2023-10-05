package de.hysky.skyblocker.skyblock.filters;

import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.utils.chat.ChatFilterResult;

public class AbilityFilter extends SimpleChatFilter {
    public AbilityFilter() {
        super("^(?:This ability is on cooldown for " + NUMBER + "s\\.|No more charges, next one in " + NUMBER + "s!)$");
    }

    @Override
    protected ChatFilterResult state() {
        return SkyblockerConfigManager.get().messages.hideAbility;
    }
}
