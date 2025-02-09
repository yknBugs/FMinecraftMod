package com.ykn.fmod.server.base.data;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.text.Text;

/**
 * Most functions in this class are thread-safe.
 */
public class GptData {

    protected final transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected List<String> postMessages;
    protected List<Text> responseTexts;
    protected List<String> reponseMessages;
    protected List<String> responseJson;
    protected List<URL> requestUrls;
    protected List<String> gptModels;
    protected List<Double> responseTemperature;
    protected boolean hasReceivedResponse;

    protected String cachedPostMessage;
    protected URL cachedRequestUrl;
    protected String cachedGptModel;
    protected double cachedResponseTemperature;

    public GptData() {
        postMessages = new ArrayList<>();
        responseTexts = new ArrayList<>();
        reponseMessages = new ArrayList<>();
        responseJson = new ArrayList<>();
        requestUrls = new ArrayList<>();
        gptModels = new ArrayList<>();
        responseTemperature = new ArrayList<>();
        hasReceivedResponse = true;

        cachedPostMessage = "";
        cachedRequestUrl = null;
        cachedGptModel = "";
        cachedResponseTemperature = 0.8;
    }

    /**
     * Receives a message and stores it along with its associated data if a response has not been received yet.
     *
     * @param message The message to be received.
     * @param text The text object associated with the message.
     * @param responseJson The JSON response associated with the message.
     * @return true if the message was successfully received and stored, false if a response has already been received.
     */
    public boolean receiveMessage(String message, Text text, String responseJson) {
        boolean result = false;
        this.lock.writeLock().lock();
        try {
            if (this.hasReceivedResponse == false) {
                this.postMessages.add(this.cachedPostMessage);
                this.requestUrls.add(this.cachedRequestUrl);
                this.gptModels.add(this.cachedGptModel);
                this.responseTemperature.add(this.cachedResponseTemperature);
                this.reponseMessages.add(message);
                this.responseTexts.add(text);
                this.responseJson.add(responseJson);
                this.hasReceivedResponse = true;
                result = true;
            }
        } finally {
            this.lock.writeLock().unlock();
        }
        return result;
    }

    /**
     * Converts the current instance of ChatRequest to a JSON string.
     *
     * @return A JSON representation of the ChatRequest object.
     */
    public String getPostMessageJson() {
        Gson gson = new Gson();
        this.lock.readLock().lock();
        try {
            return gson.toJson(new ChatRequest(this));
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Clears the history of GPT-related data if a response has been received.
     * This method will clear the following collections:
     * - postMessages
     * - requestUrls
     * - gptModels
     * - responseTemperature
     * - reponseMessages
     * - responseTexts
     * - responseJson
     *
     * @return true if the history was cleared, false if no response has been received.
     */
    public boolean clearHistory() {
        boolean result = false;
        this.lock.writeLock().lock();
        try {
            if (this.hasReceivedResponse) {
                this.postMessages.clear();
                this.requestUrls.clear();
                this.gptModels.clear();
                this.responseTemperature.clear();
                this.reponseMessages.clear();
                this.responseTexts.clear();
                this.responseJson.clear();
                result = true;
            }
        } finally {
            this.lock.writeLock().unlock();
        }
        return result;
    }

    /**
     * Returns the number of post messages in the history.
     *
     * @return the size of the post messages history
     */
    public int getHistorySize() {
        this.lock.readLock().lock();
        try {
            return this.postMessages.size();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Stores the message, URL, GPT model, and response temperature for a future response.
     * @param message The message to be sent.
     * @param url The URL endpoint to which the message will be sent.
     * @param gptModel The GPT model to be used for generating the response.
     * @param responseTemperature The temperature setting for the response generation, affecting randomness.
     * @return true if the message was successfully stored, false if a response has already been received.
     */
    public boolean reply(String message, URL url, String gptModel, double responseTemperature) {
        boolean result = false;
        this.lock.writeLock().lock();
        try {
            if (this.hasReceivedResponse) {
                this.cachedPostMessage = message;
                this.cachedRequestUrl = url;
                this.cachedGptModel = gptModel;
                this.cachedResponseTemperature = responseTemperature;
                this.hasReceivedResponse = false;
                result = true;
            } 
        } finally {
            this.lock.writeLock().unlock();
        }
        return result;
    }

    /**
     * Initiates a new conversation by clearing the history and sending a new message.
     *
     * @param message The message to be sent in the new conversation.
     * @param url The URL endpoint to which the message will be sent.
     * @param gptModel The GPT model to be used for generating the response.
     * @param responseTemperature The temperature setting for the response generation, affecting randomness.
     * @return true if the history was successfully cleared and the message was sent, false otherwise.
     */
    public boolean newConversation(String message, URL url, String gptModel, double responseTemperature) {
        boolean result = this.clearHistory();
        if (result) {
            result = this.reply(message, url, gptModel, responseTemperature);
        }
        return result;
    }

    /**
     * Regenerates the last message in the history if certain conditions are met.
     * 
     * @return {@code true} if the regeneration was successful and the last message
     *         was removed from the history; {@code false} otherwise.
     */
    public boolean regenerate() {
        if (this.getHistorySize() == 0) {
            return false;
        }
        if (this.getHasReceivedResponse() == false) {
            return false;
        }
        int lastMessageIndex = this.getHistorySize() - 1;
        boolean result = this.reply(this.postMessages.get(lastMessageIndex), this.requestUrls.get(lastMessageIndex), this.gptModels.get(lastMessageIndex), this.responseTemperature.get(lastMessageIndex));
        if (result) {
            this.lock.writeLock().lock();
            try {
                this.postMessages.remove(lastMessageIndex);
                this.requestUrls.remove(lastMessageIndex);
                this.gptModels.remove(lastMessageIndex);
                this.responseTemperature.remove(lastMessageIndex);
                this.reponseMessages.remove(lastMessageIndex);
                this.responseTexts.remove(lastMessageIndex);
                this.responseJson.remove(lastMessageIndex);
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        return result;
    }

    /**
     * Regenerates the last message in the history by sending a reply using the specified parameters.
     * If the history is empty or no response has been received, the method returns false.
     * If the reply is successful, the last message and its associated data are removed from the history.
     *
     * @param url The URL to send the reply to.
     * @param gptModel The GPT model to use for generating the reply.
     * @param responseTemperature The temperature setting for the GPT model response.
     * @return true if the reply was successful and the last message was removed; false otherwise.
     */
    public boolean regenerate(URL url, String gptModel, double responseTemperature) {
        if (this.getHistorySize() == 0) {
            return false;
        }
        if (this.getHasReceivedResponse() == false) {
            return false;
        }
        int lastMessageIndex = this.getHistorySize() - 1;
        boolean result = this.reply(this.postMessages.get(lastMessageIndex), url, gptModel, responseTemperature);
        if (result) {
            this.lock.writeLock().lock();
            try {
                this.postMessages.remove(lastMessageIndex);
                this.requestUrls.remove(lastMessageIndex);
                this.gptModels.remove(lastMessageIndex);
                this.responseTemperature.remove(lastMessageIndex);
                this.reponseMessages.remove(lastMessageIndex);
                this.responseTexts.remove(lastMessageIndex);
                this.responseJson.remove(lastMessageIndex);
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        return result;
    }

    /**
     * Edits the history at the specified index with the provided message, URL, GPT model, and response temperature.
     * It will remove all history entries after the specified index.
     * 
     * @param index The index of the history entry to edit.
     * @param message The new message to replace the existing one.
     * @param url The new URL to replace the existing one.
     * @param gptModel The new GPT model to replace the existing one.
     * @param responseTemperature The new response temperature to replace the existing one.
     * @return true if the history was successfully edited, false otherwise.
     */
    public boolean editHistory(int index, String message, URL url, String gptModel, double responseTemperature) {
        if (index < 0 || index >= this.getHistorySize()) {
            return false;
        }
        if (this.getHasReceivedResponse() == false) {
            return false;
        }
        boolean result = this.reply(message, url, gptModel, responseTemperature);
        this.lock.writeLock().lock();
        try {
            while (result && this.postMessages.size() > index) {
                this.postMessages.remove(index);
                this.requestUrls.remove(index);
                this.gptModels.remove(index);
                this.responseTemperature.remove(index);
                this.reponseMessages.remove(index);
                this.responseTexts.remove(index);
                this.responseJson.remove(index);
            }
        } finally {
            this.lock.writeLock().unlock();
        }
        return result;
    }

    public String getPostMessages(int index) {
        this.lock.readLock().lock();
        try {
            return this.postMessages.get(index);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public Text getResponseTexts(int index) {
        this.lock.readLock().lock();
        try {
            return this.responseTexts.get(index);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public String getResponseJson(int index) {
        this.lock.readLock().lock();
        try {
            return this.responseJson.get(index);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public String getReponseMessages(int index) {
        this.lock.readLock().lock();
        try {
            return this.reponseMessages.get(index);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public URL getRequestUrls(int index) {
        this.lock.readLock().lock();
        try {
            return this.requestUrls.get(index);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public String getGptModels(int index) {
        this.lock.readLock().lock();
        try {
            return this.gptModels.get(index);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public Double getResponseTemperature(int index) {
        this.lock.readLock().lock();
        try {
            return this.responseTemperature.get(index);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public boolean getHasReceivedResponse() {
        this.lock.readLock().lock();
        try {
            return this.hasReceivedResponse;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public String getCachedPostMessage() {
        this.lock.readLock().lock();
        try {
            return this.cachedPostMessage;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public URL getCachedRequestUrl() {
        this.lock.readLock().lock();
        try {
            return this.cachedRequestUrl;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public String getCachedGptModel() {
        this.lock.readLock().lock();
        try {
            return this.cachedGptModel;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public double getCachedResponseTemperature() {
        this.lock.readLock().lock();
        try {
            return this.cachedResponseTemperature;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Retrieves a list of chat messages, alternating between user and assistant messages.
     * 
     * This method constructs a list of `ChatMessage` objects by iterating through the 
     * `postMessages` and `reponseMessages` lists. For each entry in these lists, it adds 
     * a `ChatMessage` for the user and a corresponding `ChatMessage` for the assistant.
     * Finally, it adds a `ChatMessage` for the cached post message from the user.
     * 
     * @return An array of `ChatMessage` objects representing the conversation history.
     */
    private ChatMessage[] getChatMessageList() {
        ArrayList<ChatMessage> messages = new ArrayList<>();
        String systemPrompt = Util.serverConfig.getGptSystemPrompt();
        if (systemPrompt.isBlank() == false) {
            messages.add(new ChatMessage("system", systemPrompt));
        }
        for (int i = 0; i < postMessages.size(); i++) {
            messages.add(new ChatMessage("user", postMessages.get(i)));
            messages.add(new ChatMessage("assistant", reponseMessages.get(i)));
        }
        messages.add(new ChatMessage("user", cachedPostMessage));
        return messages.toArray(new ChatMessage[messages.size()]);
    }

    @SuppressWarnings("unused")
    private static class ChatRequest {
        private final ChatMessage[] messages;
        private final double temperature;
        // private final int top_k = 40;
        // private final double top_p = 0.95;
        // private final double min_p = 0.05;
        // private final int max_tokens = 4096;
        // private final double frequency_penalty = 0.0;
        // private final double presence_penalty = 0.0;
        private final String model;

        public ChatRequest(GptData data) {
            this.messages = data.getChatMessageList();
            this.temperature = data.cachedResponseTemperature;
            this.model = data.cachedGptModel;
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
}
