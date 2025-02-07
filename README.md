# F Minecraft Mod

A Minecraft Mod for Management

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

## Features & Changelog

### Version 0.0.2

Hint: This mod is used on Minecraft 1.20.1 Fabric

- Allow change options by using command /f options.
- Send the death message to chat when a boss or named entity died.

### Version 0.0.1

- Use command /f gpt to chat with large language model in game with simple markdown syntax highlight.
- View the death message at actionbar when any kinds of entity died.
