/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.async;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.data.GptData;
import com.ykn.fmod.server.base.util.MarkdownToTextConverter;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

    private static final Gson gson = new Gson();

    private final GptData gptData;
    private final CommandContext<ServerCommandSource> context;

    private volatile Text feedbackText;
    private volatile String loggedResponse;

    public GptCommandExecutor(GptData gptData, CommandContext<ServerCommandSource> context) {
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
            connection.setConnectTimeout(Util.getServerConfig().getGptServerTimeout());
            connection.setReadTimeout(Util.getServerConfig().getGptServerTimeout());
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            final String accessTokens = Util.getServerConfig().getGptAccessTokens();
            if (!accessTokens.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + accessTokens);
            }
            connection.setDoOutput(true);

            final String jsonRequest = gptData.getPostMessageJson();
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder responseBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                        responseBuilder.append("\n");
                    }
                }
                final String responseJson = responseBuilder.toString().strip();
                final String response = gson.fromJson(responseJson, ChatResponse.class).getMessageContent().strip();
                final String responseModel = gptData.getCachedGptModel();
                final Text formattedText = MarkdownToTextConverter.parseMarkdownToText(response);
                gptData.receiveMessage(response, formattedText, responseJson);
                this.feedbackText = Text.literal("<").append(responseModel.isBlank() ? "GPT" : responseModel).append("> ").append(formattedText);
                this.loggedResponse = "<" + (responseModel.isBlank() ? "GPT" : responseModel) + "> " + response;
                this.markAsyncFinished();
            } else {
                gptData.cancel();
                this.feedbackText = Util.parseTranslatableText("fmod.command.gpt.httperror", responseCode).formatted(Formatting.RED);
                this.markAsyncFinished();
                Util.LOGGER.warn("FMinecraftMod: GPT server response code: " + responseCode);
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    StringBuilder responseBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                            responseBuilder.append("\n");
                        }
                    }
                    Util.LOGGER.warn("FMinecraftMod: GPT server response: " + responseBuilder.toString().strip());
                }
            }
        } catch (SocketTimeoutException e) {
            gptData.cancel();
            this.feedbackText = Util.parseTranslatableText("fmod.command.gpt.timeout").formatted(Formatting.RED);
            this.markAsyncFinished();
            Util.LOGGER.warn("FMinecraftMod: Connect to the GPT server timeout", e);
        } catch (ConnectException e) {
            gptData.cancel();
            this.feedbackText = Util.parseTranslatableText("fmod.command.gpt.connecterror").formatted(Formatting.RED);
            this.markAsyncFinished();
            Util.LOGGER.warn("FMinecraftMod: Cannot connect to the GPT server", e);
        } catch (FileNotFoundException e) {
            gptData.cancel();
            this.feedbackText = Util.parseTranslatableText("fmod.command.gpt.fileerror").formatted(Formatting.RED);
            this.markAsyncFinished();
            Util.LOGGER.warn("FMinecraftMod: The GPT server did not return a valid response", e);
        } catch (Exception e) {
            gptData.cancel();
            this.feedbackText = Util.parseTranslatableText("fmod.command.gpt.error").formatted(Formatting.RED);
            this.markAsyncFinished();
            Util.LOGGER.error("FMinecraftMod: Exception while connecting to the GPT server", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void taskAfterCompletion() {
        if (context.getSource().isExecutedByPlayer()) {
            if (this.loggedResponse != null) {
                Util.LOGGER.info(this.loggedResponse);
            }
            if (context.getSource().getPlayer() == null || context.getSource().getPlayer().isDisconnected()) {
                Util.LOGGER.info("FMinecraftMod: GPT command executed but the player has disconnected.");
                return;
            }
        }
        if (this.feedbackText == null) {
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.gpt.emptyerror").formatted(Formatting.RED), false);
            Util.LOGGER.error("FMinecraftMod: GPT command executed but no feedback text was set.");
            return;
        }
        context.getSource().sendFeedback(() -> this.feedbackText, false);
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
