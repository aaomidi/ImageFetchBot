package com.bo0tzz.imagebot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import pro.zackpollard.telegrambot.api.TelegramBot;
import pro.zackpollard.telegrambot.api.chat.Chat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.*;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

/**
 * Created by bo0tzz
 */
public class ImageBot {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final File CONFIG_FILE = new File("config.json");
    public static TelegramBot bot;
    private Timer timer;
    private BotConfig config;

    public static void main(String[] args) {
        new ImageBot().run();
    }

    public void run() {
        config = BotConfig.loadConfig(CONFIG_FILE);

        if (config == null) {
            System.out.println("There was an error loading config.. Shutting down");
            System.exit(-127);
            return;
        }

        if ("enter_key_here".equals(config.botKey())) {
            System.out.println("Please configure config.json before using this bot!");
            System.exit(1);
            return;
        }

        bot = TelegramBot.login(config.botKey());
        bot.getEventsManager().register(new ImageCommandListener(this));
        bot.startUpdates(false);

        if (config.adminChatId() == null) {
            System.out.println("Admin chat is not setup. Execute /admin with the bot id to set the admin chat.");
        }

        timer = new Timer();
        LocalDateTime todayMidnight = LocalDateTime.of(LocalDate.now(ZoneId.of("UTC")), LocalTime.MIDNIGHT);

        timer.scheduleAtFixedRate(new ResetUsesTask(this), todayMidnight.toEpochSecond(ZoneOffset.UTC), TimeUnit.DAYS.toMillis(1));
    }

    public String[] getKeys() {
        try {
            String[] keys = Files.lines(new File("g_keys").toPath())
                    .filter((predicate) -> predicate.equals("") ? false : true)
                    .toArray(String[]::new);
            return keys;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public BotConfig getConfig() {
        return config;
    }

    public String getGiphyKey() {
        try {
            return FileUtils.readFileToString(new File("giphyKey"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public Chat getAdminChat() {
        return config.adminChatId() == null ? null : bot.getChat(config.adminChatId());
    }

    public BotUser getOrCreate(long userId) {
        return config.users().stream()
                .filter((user) -> user.id() == userId)
                .findFirst()
                .orElseGet(() -> {
                    BotUser user = new BotUser(userId);
                    config.users().add(user);
                    return user;
                });
    }

    public BotUser getOrNull(long userId) {
        return config.users().stream().filter((user) -> user.id() == userId).findFirst().orElse(null);
    }

    public boolean canUse(long userId) {
        return config.users().stream()
                .filter((user) -> user.id() == userId)
                .anyMatch(BotUser::canUse);
    }

    public boolean isLimited(long userId) {
        BotUser user = getOrNull(userId);
        return config.maxUses() != -1 && (user == null || user.uses() >= config.maxUses());
    }
}
