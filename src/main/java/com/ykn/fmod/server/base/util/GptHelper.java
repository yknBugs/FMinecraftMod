package com.ykn.fmod.server.base.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class GptHelper implements Runnable {

    private String text;
    private URL url;
    private String response;
    private CommandContext<ServerCommandSource> context;

    public GptHelper(String text, CommandContext<ServerCommandSource> context) {
        this.text = text;
        this.context = context;
    }

    public String getResponse() {
        return response;
    }

    public void setURL(String url) throws Exception {
        this.url = new URI(url).toURL();
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(Util.serverConfig.getGptServerTimeout());
            connection.setReadTimeout(Util.serverConfig.getGptServerTimeout());
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            String accessTokens = Util.serverConfig.getGptAccessTokens();
            if (!accessTokens.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + accessTokens);
            }
            connection.setDoOutput(true);

            Gson gson = new Gson();
            String jsonRequest = gson.toJson(new ChatRequest(text));
            connection.getOutputStream().write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                response = gson.fromJson(responseBuilder.toString(), ChatResponse.class).getMessageContent();
                Text formattedText = MarkdownToTextConverter.parseMarkdownToText(response);
                context.getSource().getServer().execute(() -> {
                    context.getSource().sendFeedback(() -> formattedText, false);
                });
                if (context.getSource().getPlayer() != null) {
                    LoggerFactory.getLogger(Util.LOGGERNAME).info(response);
                }
            } else {
                context.getSource().getServer().execute(() -> {
                    context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.gpt.httperror", responseCode), false);
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
            context.getSource().getServer().execute(() -> {
                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.gpt.timeout"), false);
            });
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Connect to the GPT server timeout", e);
        } catch (Exception e) {
            context.getSource().getServer().execute(() -> {
                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.gpt.error"), false);
            });
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Exception while connecting to the GPT server", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @SuppressWarnings("unused")
    private static class ChatRequest {
        private final ChatMessage[] messages;
        private final double temperature = Util.serverConfig.getGptTemperature();
        // private final int top_k = 40;
        // private final double top_p = 0.95;
        // private final double min_p = 0.05;
        // private final int max_tokens = 4096;
        // private final double frequency_penalty = 0.0;
        // private final double presence_penalty = 0.0;
        private final String model = Util.serverConfig.getGptModel();

        public ChatRequest(String content) {
            this.messages = new ChatMessage[]{new ChatMessage("user", content)};
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
