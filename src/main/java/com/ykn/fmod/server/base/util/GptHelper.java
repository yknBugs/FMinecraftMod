/**
 * Copyright (c) ykn, Xenapte
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.data.GptData;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GptHelper implements Runnable {

    private GptData gptData;
    private CommandContext<ServerCommandSource> context;

    public GptHelper(GptData gptData, CommandContext<ServerCommandSource> context) {
        this.gptData = gptData;
        this.context = context;
    }

    public GptData getGptData() {
        return gptData;
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) gptData.getCachedRequestUrl().openConnection();
            connection.setConnectTimeout(Util.serverConfig.getGptServerTimeout());
            connection.setReadTimeout(Util.serverConfig.getGptServerTimeout());
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            final String accessTokens = Util.serverConfig.getGptAccessTokens();
            if (!accessTokens.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + accessTokens);
            }
            connection.setDoOutput(true);

            Gson gson = new Gson();
            final String jsonRequest = gptData.getPostMessageJson();
            connection.getOutputStream().write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                final String responseJson = responseBuilder.toString();
                final String response = gson.fromJson(responseJson, ChatResponse.class).getMessageContent().trim();
                final String responseModel = gptData.getCachedGptModel();
                final Text formattedText = MarkdownToTextConverter.parseMarkdownToText(response);
                gptData.receiveMessage(response, formattedText, responseJson);
                context.getSource().getServer().execute(() -> {
                    context.getSource().sendFeedback(() -> Text.literal("<").append(responseModel.isBlank() ? "GPT" : responseModel).append("> ").append(formattedText), false);
                    if (context.getSource().getPlayer() != null) {
                        LoggerFactory.getLogger(Util.LOGGERNAME).info("<" + (responseModel.isBlank() ? "GPT" : responseModel) + "> " + response);
                    }
                });
            } else {
                gptData.cancel();
                context.getSource().getServer().execute(() -> {
                    context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.gpt.httperror", responseCode).formatted(Formatting.RED), false);
                });
                LoggerFactory.getLogger(Util.LOGGERNAME).info("FMinecraftMod: GPT server response code: " + responseCode);
                // BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                // StringBuilder responseBuilder = new StringBuilder();
                // String line;
                // while ((line = reader.readLine()) != null) {
                //     responseBuilder.append(line);
                // }
                // LoggerFactory.getLogger(Util.LOGGERNAME).info("FMinecraftMod: GPT server response: " + responseBuilder.toString());
            }
        } catch (SocketTimeoutException e) {
            gptData.cancel();
            context.getSource().getServer().execute(() -> {
                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.gpt.timeout").formatted(Formatting.RED), false);
            });
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Connect to the GPT server timeout", e);
        } catch (Exception e) {
            gptData.cancel();
            context.getSource().getServer().execute(() -> {
                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.gpt.error").formatted(Formatting.RED), false);
            });
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Exception while connecting to the GPT server", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @SuppressWarnings("unused")
    private static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static class ChatResponse {
        private Choice[] choices;

        public String getMessageContent() {
            return choices.length > 0 ? choices[0].message.content : "";
        }

        private static class Choice {
            private ChatMessage message;
        }
    }
}
