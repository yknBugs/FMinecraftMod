package com.ykn.fmod.server.base.config;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerConfig extends ConfigReader {

    // Multiple threads may access the config class at the same time, the getters and setters in this class must be locked to ensure thread safety.
    protected final transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * If enabled, the server will translate all the messages and then send to the client.
     * This is useful if only the server has this mod installed and the client does not have it.
     * But if enabled, the message will not follow the client's language setting.
     * Default: false
     */
    protected boolean enableServerTranslation;

    /**
     * If enabled, the server will send an actionbar message to the client when an entity dies.
     * Default: false
     */
    protected boolean enableEntityDeathMsg;

    /**
     * If enabled, the server will send a text message to the client when a boss dies.
     * Default: false
     */
    protected boolean bcBossDeathMsg;

    /**
     * If enabled, the server will send a text message to the client when a mob with custom name dies.
     * Default: false
     */
    protected boolean namedMobDeathMsg;

    /**
     * If an entity has a health greater than this value, it will be considered as a boss.
     * Default: 150
     */
    protected double bossMaxHpThreshold;

    /**
     * The URL of the target GPT server.
     * This mod will use the OpenAI API.
     * So if you want to deploy a local LLM, you must make sure it is compatible with the OpenAI API.
     * Default: A server created by llama-server --host 0.0.0.0 --port 12345 (Search the llama.cpp project on github for more information)
     */
    protected String gptUrl;

    /**
     * The access tokens of the GPT server.
     * If the sever does not need an access token, set it to an empty string, but not null.
     * Default: ""
     */
    protected String gptAccessTokens;

    /**
     * The model of the GPT server.
     * Default: ""
     */
    protected String gptModel;

    /**
     * The temperature parameter of generating the response.
     * Default: 0.8
     */
    protected double gptTemperature;

    /**
     * The timeout of the GPT server in milliseconds.
     * Default: 60000 (60 seconds)
     */
    protected int gptServerTimeout;

    public ServerConfig() {
        super("server.json");
        this.enableServerTranslation = false;
        this.enableEntityDeathMsg = false;
        this.bcBossDeathMsg = false;
        this.namedMobDeathMsg = false;
        this.bossMaxHpThreshold = 150;
        this.gptUrl = "http://127.0.0.1:12345/v1/chat/completions";
        this.gptAccessTokens = "";
        this.gptModel = "";
        this.gptTemperature = 0.8;
        this.gptServerTimeout = 60000;
    }

    public boolean isEnableServerTranslation() {
        lock.readLock().lock();
        try {
            return enableServerTranslation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEnableServerTranslation(boolean enableServerTranslation) {
        lock.writeLock().lock();
        try {
            this.enableServerTranslation = enableServerTranslation;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isEnableEntityDeathMsg() {
        lock.readLock().lock();
        try {
            return enableEntityDeathMsg;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEnableEntityDeathMsg(boolean enableEntityDeathMsg) {
        lock.writeLock().lock();
        try {
            this.enableEntityDeathMsg = enableEntityDeathMsg;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isBcBossDeathMsg() {
        lock.readLock().lock();
        try {
            return bcBossDeathMsg;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBcBossDeathMsg(boolean bcBossDeathMsg) {
        lock.writeLock().lock();
        try {
            this.bcBossDeathMsg = bcBossDeathMsg;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isNamedMobDeathMsg() {
        lock.readLock().lock();
        try {
            return namedMobDeathMsg;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setNamedMobDeathMsg(boolean namedMobDeathMsg) {
        lock.writeLock().lock();
        try {
            this.namedMobDeathMsg = namedMobDeathMsg;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getBossMaxHpThreshold() {
        lock.readLock().lock();
        try {
            return bossMaxHpThreshold;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBossMaxHpThreshold(double bossMaxHpThreshold) {
        lock.writeLock().lock();
        try {
            this.bossMaxHpThreshold = bossMaxHpThreshold;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getGptUrl() {
        lock.readLock().lock();
        try {
            return gptUrl;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptUrl(String gptUrl) {
        lock.writeLock().lock();
        try {
            this.gptUrl = gptUrl;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getGptAccessTokens() {
        lock.readLock().lock();
        try {
            return gptAccessTokens;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Retrieves a secure version of the GPT access token.
     * The secure token masks the middle part of the original token with asterisks for security purposes.
     * 
     * @return A string representing the secure version of the GPT access token. 
     *         If the token length is greater than 20, the first 5 and last 5 characters are visible, 
     *         with the middle characters replaced by asterisks. 
     *         If the token length is between 1 and 20, the entire token is replaced by asterisks. 
     *         If the token is empty, "null" is returned.
     */
    public String getSecureGptAccessTokens() {
        String token = "";
        lock.readLock().lock();
        try {
            token = gptAccessTokens;
        } finally {
            lock.readLock().unlock();
        }
        String secureToken = "";
        if (token.length() > 20) {
            secureToken = token.substring(0, 5) + String.valueOf("*".repeat(token.length() - 10)) + token.substring(token.length() - 5);
        } else if (token.length() > 0) {
            secureToken = String.valueOf("*".repeat(token.length()));
        } else {
            secureToken = "null";
        }
        return secureToken;
    }

    public void setGptAccessTokens(String gptAccessTokens) {
        lock.writeLock().lock();
        try {
            this.gptAccessTokens = gptAccessTokens;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getGptModel() {
        lock.readLock().lock();
        try {
            return gptModel;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptModel(String gptModel) {
        lock.writeLock().lock();
        try {
            this.gptModel = gptModel;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getGptTemperature() {
        lock.readLock().lock();
        try {
            return gptTemperature;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptTemperature(double gptTemprature) {
        lock.writeLock().lock();
        try {
            this.gptTemperature = gptTemprature;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getGptServerTimeout() {
        lock.readLock().lock();
        try {
            return gptServerTimeout;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setGptServerTimeout(int gptServerTimeout) {
        lock.writeLock().lock();
        try {
            this.gptServerTimeout = gptServerTimeout;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
