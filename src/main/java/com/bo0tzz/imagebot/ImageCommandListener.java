package com.bo0tzz.imagebot;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pro.zackpollard.telegrambot.api.chat.Chat;
import pro.zackpollard.telegrambot.api.chat.ChatMemberStatus;
import pro.zackpollard.telegrambot.api.chat.inline.send.InlineQueryResponse;
import pro.zackpollard.telegrambot.api.chat.inline.send.content.InputMessageContent;
import pro.zackpollard.telegrambot.api.chat.inline.send.content.InputTextMessageContent;
import pro.zackpollard.telegrambot.api.chat.inline.send.results.InlineQueryResult;
import pro.zackpollard.telegrambot.api.chat.inline.send.results.InlineQueryResultArticle;
import pro.zackpollard.telegrambot.api.chat.inline.send.results.InlineQueryResultPhoto;
import pro.zackpollard.telegrambot.api.chat.message.content.TextContent;
import pro.zackpollard.telegrambot.api.chat.message.content.type.MessageEntityType;
import pro.zackpollard.telegrambot.api.chat.message.send.*;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.inline.InlineQueryReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.CommandMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.menu.InlineMenu;
import pro.zackpollard.telegrambot.api.user.User;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by bo0tzz
 */
public class ImageCommandListener implements Listener {
    private ImageBot bot;
    private final String[] keys;
    private int lastKey = 0;
    private final String giphyAPI;

    public ImageCommandListener(ImageBot bot) {
        this.bot = bot;
        this.keys = bot.getKeys();
        this.giphyAPI = "http://api.giphy.com/v1/gifs/translate?api_key=" + bot.getGiphyKey() + "&s=";
    }

    @Override
    public void onInlineQueryReceived(InlineQueryReceivedEvent event) {
        String query = event.getQuery().getQuery();
        List<InlineQueryResult> responses = new ArrayList<>();
        HttpResponse<JsonNode> response = null;

        if (!bot.canUse(event.getQuery().getSender().getId())) {
            String emoji = "\uD83D\uDE45\uD83C\uDFFC\u200D";
            InlineQueryResultArticle result = InlineQueryResultArticle.builder()
                    .title("You do not have permissions to use this bot")
                    .description("Please request permissions using /requestpermissions")
                    .inputMessageContent(InputTextMessageContent.builder()
                            .messageText("❌" + emoji + "❌" + emoji + "️❌ NO PERMISSION ❌" + emoji + "❌" + emoji + "❌")
                            .build())
                    .build();

            event.getQuery().answer(ImageBot.bot, InlineQueryResponse.builder().results(result).build());
            return;
        }

        if (bot.isLimited(event.getQuery().getSender().getId())) {
            InlineQueryResultArticle result = InlineQueryResultArticle.builder()
                    .title("You have reached your max amount of uses for the day!")
                    .description("Please try again later")
                    .inputMessageContent(InputTextMessageContent.builder()
                            .messageText("I'm a bad boy and reached my max uses :(")
                            .build())
                    .build();

            event.getQuery().answer(ImageBot.bot, InlineQueryResponse.builder().results(result).build());
            return;
        }

        try {
            response = Unirest.get(getUrl() + query.replace(" ", "+"))
                    .asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        BotUser user = bot.getOrCreate(event.getQuery().getSender().getId());

        user.incrementUses();

        JSONArray array = null;

        if (response.getBody().getObject().has("items")) {
            array = response.getBody().getObject().getJSONArray("items");
        } else if (response.getBody().getObject().has("error")) {
            InlineQueryResultArticle result = InlineQueryResultArticle.builder()
                    .title("The bot has reached its request limit for today!")
                    .description("Unfortunately, Google limits the amount of requests this bot can make every day. " +
                            "Please contact the admins if you want to help get rid of this limit!")
                    .inputMessageContent(InputTextMessageContent.builder()
                        .messageText("Unfortunately this bot has reached the daily limit on how many images it can fetch for you." +
                                "Please be patient, the limit should reset within 24 hours.\n" +
                                "If you want to help get rid of these limits, please contact the admins!")
                        .parseMode(ParseMode.MARKDOWN)
                        .build())
                    .build();
            event.getQuery().answer(ImageBot.bot, InlineQueryResponse.builder().results(result).build());
            return;
        }

        if (array == null) return;

        for (int i = 0; i < array.length(); i++) {
            JSONObject image = array.getJSONObject(i);
            URL url = null;
            URL thumb = null;
            try {
                url = new URL(image.getString("link"));
                thumb = new URL(image.getJSONObject("image").getString("thumbnailLink"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            //getting width and height of image, so Telegram will show the Response properly
            int width = 0;
            int height = 0;
            
            width = image.getJSONObject("image").getInt("width");
            height = image.getJSONObject("image").getInt("height");
            
            InlineQueryResultPhoto result = InlineQueryResultPhoto.builder().photoUrl(url).photoWidth(width).photoHeight(height).thumbUrl(thumb).build();
            responses.add(result);
        }

        event.getQuery().answer(ImageBot.bot, InlineQueryResponse.builder().results(responses).build());
        bot.getConfig().save(ImageBot.CONFIG_FILE);
    }

    public void onCommandMessageReceived(CommandMessageReceivedEvent event) {
        if (event.getCommand().equals("get")) {
            if (event.getArgsString().equals("")) {
                event.getChat().sendMessage("Give me");
                return;
            }

            if (!bot.canUse(event.getMessage().getSender().getId()) && !isAdminChat(event.getChat())) {
                event.getChat().sendMessage("You do not have permissions to use this bot. Please request permissions using /requestpermissions");
                return;
            }

            if (bot.isLimited(event.getMessage().getSender().getId()) && !isAdminChat(event.getChat())) {
                event.getChat().sendMessage("You have reached your max amount of uses for the day! Please try again later");
                return;
            }

            event.getChat().sendMessage(SendableChatAction.builder().chatAction(ChatAction.UPLOADING_PHOTO).build());

            BotUser user = bot.getOrNull(event.getMessage().getSender().getId());
            HttpResponse<JsonNode> response = null;

            try {
                response = Unirest.get(getUrl() + event.getArgsString().replace(" ", "+"))
                        .asJson();
            } catch (UnirestException e) {
                e.printStackTrace();
            }

            if (user != null) {
                user.incrementUses();
            } else {
                bot.getConfig().setTotalUses(bot.getConfig().totalUses() + 1);
            }

            if (response.getBody().getObject().has("error")) {
                event.getChat().sendMessage("The Google API returned the following error - " + response.getBody().getObject().getJSONObject("error").getString("message"));
                System.out.println("Google API returned error: " + response.getBody());
                return;
            }

            JSONArray array;

            if (response.getBody().getObject().has("items")) {
                array = response.getBody().getObject().getJSONArray("items");
            } else {
                event.getChat().sendMessage("No images found!");
                return;
            }

            if (array.length() == 0) {
                event.getChat().sendMessage("No images found!");
                return;
            }

            JSONObject image = array.getJSONObject(ThreadLocalRandom.current().nextInt(array.length()));
            URL url;

            try {
                url = new URL(image.getString("link"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
                event.getChat().sendMessage("Something went wrong while getting the image!");
                return;
            }

            System.out.println("Uploading photo: " + url);

            event.getChat().sendMessage(SendablePhotoMessage.builder()
                    .photo(new InputFile(url))
                    .replyTo(event.getMessage())
                    .build());

            System.out.println("Photo uploaded: " + url);
        } else if (event.getCommand().equals("getgif")) {
            if (!bot.canUse(event.getMessage().getSender().getId()) && !isAdminChat(event.getChat())) {
                event.getChat().sendMessage("You do not have permissions to use this bot. Please request permissions using /requestpermissions");
                return;
            }

            event.getChat().sendMessage(SendableChatAction.builder().chatAction(ChatAction.UPLOAD_DOCUMENT).build());

            URI request;

            try {
                request = new URI(giphyAPI + event.getArgsString().replace(" ", "+"));
            } catch (URISyntaxException e) {
                event.getChat().sendMessage("Request contained illegal characters!");
                return;
            }

            HttpResponse<JsonNode> response = null;

            try {
                response = Unirest.get(request.toString())
                        .asJson();
            } catch (UnirestException e) {
                e.printStackTrace();
            }

            URL url = null;

            try {
                JSONObject image = response.getBody().getObject().getJSONObject("data").getJSONObject("images").getJSONObject("original");
                url = new URL(image.getString("url"));
            } catch (MalformedURLException|JSONException e) {
                System.out.println("Error on response: " + response.getBody());
                e.printStackTrace();
            }

            if (url == null) {
                event.getChat().sendMessage("No pictures found!");
                return;
            }

            System.out.println("Uploading gif: " + url);

            event.getChat().sendMessage(SendableDocumentMessage.builder()
                .document(new InputFile(url))
                .replyTo(event.getMessage())
                .build());
        }

        // ADMIN COMMANDS

        if ("admin".equalsIgnoreCase(event.getCommand())) {
            if (bot.getConfig().botKey().equals(event.getArgsString())) {
                if (event.getChat().getChatMember(ImageBot.bot.getBotID()).getStatus() == ChatMemberStatus.ADMINISTRATOR) {
                    ImageBot.bot.deleteMessage(event.getMessage());
                }

                bot.getConfig().setAdminChatId(event.getChat().getId());
                bot.getConfig().save(ImageBot.CONFIG_FILE);
                event.getChat().sendMessage("Successfully set this chat as the admin chat for this bot.");
            }

            return;
        }

        if ("requestpermissions".equals(event.getCommand())) {
            User sender = event.getMessage().getSender();
            BotUser user = bot.getOrCreate(sender.getId());
            Chat adminChat = bot.getAdminChat();

            if (adminChat == null) {
                event.getChat().sendMessage("This bot has not been completely setup. Please inform the bot owner to configure this bot");
                return;
            }

            if (user.sentRequest()) {
                event.getChat().sendMessage("You have already sent a request for permissions!");
                return;
            }

            String allow = "✅ Allow Access";
            String deny = "❌ Deny Access";
            String permissionsGiven = "You have been given permissions to use this bot";
            String permissionsRevoked = "Your permissions to use this bot are now revoked";

            InlineMenu menu = InlineMenu.builder(ImageBot.bot)
                    .message(SendableTextMessage.builder().textBuilder().bold(sender.getUsername()).plain(" has requested access to this bot.").buildText())
                    .forWhom(adminChat)
                    .newRow()
                    .toggleButton(allow)
                        .toggleCallback((b, permitted) -> {
                            user.setCanUse(permitted);
                            event.getChat().sendMessage(permitted ? permissionsGiven : permissionsRevoked);
                            bot.getConfig().save(ImageBot.CONFIG_FILE);
                            return permitted ? deny : allow;
                        })
                    .buildMenu();
            menu.start();

            user.setSentRequest(true);
            event.getChat().sendMessage("A permissions request has been sent to the admins");
            return;
        }
    }

    private boolean isAdminChat(Chat chat) {
        return chat.getId().equals(bot.getConfig().adminChatId());
    }

    private String getUrl() {
        int chosenKey = ++lastKey;

        if(chosenKey >= keys.length) {
            chosenKey = 0;
            lastKey = 0;
        }

        return "https://www.googleapis.com/customsearch/v1?key=" + keys[chosenKey] + "&cx=016322137100648159445:e9nsxf_q_-m&searchType=image&q=";
    }
}
