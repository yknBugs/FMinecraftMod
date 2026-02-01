/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.async;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.data.GptData;
import com.ykn.fmod.server.base.util.MarkdownToTextConverter;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Executes a GPT request asynchronously and delivers the result back to the
 * original command sender. This executor prepares the HTTP POST request using
 * data provided by a {@link GptData} instance, sends it to the configured GPT
 * server, parses the response and converts markdown to Minecraft Text for
 * feedback.
 *
 * <p>Execution is performed on a background thread (see
 * {@link AsyncTaskExecutor}) and once finished the result handling occurs in
 * {@link #taskAfterCompletion()} which sends feedback to the originating
 * {@link net.minecraft.server.command.ServerCommandSource}.</p>
 *
 * @see AsyncTaskExecutor
 * @see GptData
 */
public class GptCommandExecutor extends AsyncTaskExecutor {

    private final GptData gptData;
    private final CommandContext<CommandSourceStack> context;

    private volatile Component feedbackText;
    private volatile String loggedResponse;

    public GptCommandExecutor(GptData gptData, CommandContext<CommandSourceStack> context) {
        this.gptData = gptData;
        this.context = context;
        this.feedbackText = null;
        this.loggedResponse = null;
    }

    public GptData getGptData() {
        return gptData;
    }

    @Override
    public void executeAsyncTask() {
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
                final Component formattedText = MarkdownToTextConverter.parseMarkdownToText(response);
                gptData.receiveMessage(response, formattedText, responseJson);
                this.feedbackText = Component.literal("<").append(responseModel.isBlank() ? "GPT" : responseModel).append("> ").append(formattedText);
                this.loggedResponse = "<" + (responseModel.isBlank() ? "GPT" : responseModel) + "> " + response;
                this.markAsyncFinished();
            } else {
                gptData.cancel();
                this.feedbackText = Util.parseTranslatableText("fmod.command.gpt.httperror", responseCode).withStyle(ChatFormatting.RED);
                this.markAsyncFinished();
                LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: GPT server response code: " + responseCode);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: GPT server response: " + responseBuilder.toString());
            }
        } catch (SocketTimeoutException e) {
            gptData.cancel();
            this.feedbackText = Util.parseTranslatableText("fmod.command.gpt.timeout").withStyle(ChatFormatting.RED);
            this.markAsyncFinished();
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Connect to the GPT server timeout", e);
        } catch (ConnectException e) {
            gptData.cancel();
            this.feedbackText = Util.parseTranslatableText("fmod.command.gpt.connecterror").withStyle(ChatFormatting.RED);
            this.markAsyncFinished();
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Cannot connect to the GPT server", e);
        } catch (Exception e) {
            gptData.cancel();
            this.feedbackText = Util.parseTranslatableText("fmod.command.gpt.error").withStyle(ChatFormatting.RED);
            this.markAsyncFinished();
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Exception while connecting to the GPT server", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void taskAfterCompletion() {
        if (context.getSource().isPlayer()) {
            if (this.loggedResponse != null) {
                LoggerFactory.getLogger(Util.LOGGERNAME).info(this.loggedResponse);
            }
            if (context.getSource().getPlayer() == null || context.getSource().getPlayer().hasDisconnected()) {
                LoggerFactory.getLogger(Util.LOGGERNAME).info("FMinecraftMod: GPT command executed but the player has disconnected.");
                return;
            }
        }
        if (this.feedbackText == null) {
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.gpt.emptyerror").withStyle(ChatFormatting.RED), false);
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: GPT command executed but no feedback text was set.");
            return;
        }
        context.getSource().sendSuccess(() -> this.feedbackText, false);
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
