# F Minecraft Mod

A Minecraft Mod for Management

## Features

- Talk to GPT in game.

- Show death message and death coordinates.

- Detect AFKing players.

- Get informed if you successfully shoot an entity with your bow, and know its HP.

## Setup

### Step 1: Clone this repository

```bash
git clone https://github.com/yknBugs/FMinecraftMod
cd FMinecraftMod
```

### Step 2: Build this project

```bash
.\gradlew build
```

If you got an error on this step, please go to `gradle.properties` and find this line:

```text
org.gradle.java.home=...
```

Please modify the Path to your JDK17 JAVA_HOME.

### Step 3

You will find the mod file located in your `./build/libs` folder, copy the mod file to your Minecraft Mod directory and launch the game.

## Known Issues

Java Syntax Highlight may not work properly with full package path and ? symbol in generics.

## Changelog

### Version 0.0.6

- Detect AFKing players.
- Add projectile hit entity messages.
- Add change biome messages.

### Version 0.0.5

The config file of this version is not compatible with the old version.

- You can choose where to show the entity death message.
- GPT command supports conversation history and system prompt.
- I18n file enhancement.

### Version 0.0.4

- Broadcast coordinates when a player died.
- Send the death message to chat when a killer entity died.

### Version 0.0.3

Security Update:

- Disable access tokens directly prints to the game and console.
- Fix issues when connecting to the ChatGPT server.

### Version 0.0.2

Hint: This mod is used on Minecraft 1.20.1 Fabric

- Allow change options by using command /f options.
- Send the death message to chat when a boss or named entity died.

### Version 0.0.1

- Use command /f gpt to chat with large language model in game with simple markdown syntax highlight.
- View the death message at actionbar when any kinds of entity died.
