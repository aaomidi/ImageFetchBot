package com.bo0tzz.imagebot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mazen Kotb
 */
public class BotConfig {
    private String botKey = "enter_key_here";
    private boolean restrictAdmins = false;
    private String adminChatId = null;
    private int maxUses = -1;
    private int totalUses = 0;
    private Set<BotUser> users = new HashSet<>();

    public static BotConfig loadConfig(File file) {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }

                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            BotConfig config = new BotConfig();

            if (!config.save(file)) {
                return null;
            }

            return config;
        }

        try {
            return ImageBot.GSON.fromJson(new FileReader(file), BotConfig.class);
        } catch (FileNotFoundException e) {
            // what.
            e.printStackTrace();
            return null;
        }
    }

    public boolean save(File file) {
        try {
            Files.write(
                    file.toPath(),
                    Collections.singleton(ImageBot.GSON.toJson(this)),
                    ImageBot.DEFAULT_CHARSET
            );

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Set<BotUser> users() {
        return users;
    }

    public int maxUses() {
        return maxUses;
    }

    public boolean restrictAdmins() {
        return restrictAdmins;
    }

    public String botKey() {
        return botKey;
    }

    public String adminChatId() {
        return adminChatId;
    }

    public void setAdminChatId(String adminChatId) {
        this.adminChatId = adminChatId;
    }

    public int totalUses() {
        return totalUses;
    }

    public void setTotalUses(int totalUses) {
        this.totalUses = totalUses;
    }
}
