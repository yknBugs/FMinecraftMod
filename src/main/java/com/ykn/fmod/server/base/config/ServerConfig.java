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

    public ServerConfig() {
        super("server.json");
        this.enableServerTranslation = false;
        this.enableEntityDeathMsg = false;
        this.bcBossDeathMsg = false;
        this.namedMobDeathMsg = false;
        this.bossMaxHpThreshold = 150;
        this.gptUrl = "http://127.0.0.1:12345/v1/chat/completions";
        this.gptAccessTokens = "";
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

    public void setGptAccessTokens(String gptAccessTokens) {
        lock.writeLock().lock();
        try {
            this.gptAccessTokens = gptAccessTokens;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
