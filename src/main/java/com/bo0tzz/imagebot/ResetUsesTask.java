package com.bo0tzz.imagebot;

import java.util.TimerTask;

/**
 * @author Mazen Kotb
 */
public class ResetUsesTask extends TimerTask {
    private final ImageBot bot;

    public ResetUsesTask(ImageBot bot) {
        this.bot = bot;
    }

    @Override
    public void run() {
        BotConfig config = bot.getConfig();
        int lastDayUse = config.users().stream().mapToInt(BotUser::uses).sum();

        config.setTotalUses(bot.getConfig().totalUses() + lastDayUse);
        config.users().forEach(BotUser::resetUses);

        config.save(ImageBot.CONFIG_FILE);
    }
}
