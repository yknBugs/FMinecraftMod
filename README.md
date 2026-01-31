# F Minecraft Mod

A Minecraft Mod for Management

## Features

This is a server side mod, and it can work even the client does not have this mod installed.

- In-game node based simple flowchart engine. Build your custom feature at any time.

- Talk to GPT in game with simple Markdown syntax highlight.

- Playing noteblock song to all online players.

- Use command to get information (such as inventory information) of other players.

- Show death message and death coordinates.

- Detect AFKing players.

- Get informed if you successfully shoot an entity with your bow, and know its HP.

- Monitor the number of entities

- and more.

By default, this mod will not enable any features, you need to manually enable the feature you want to use.

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

If you got an error on this step, please go to `gradle.properties` and add this line:

```text
org.gradle.java.home=...
```

to your actual JDK17 JAVA_HOME.

### Step 3

You will find the mod file located in your `./build/libs` folder, copy the mod file to your Minecraft Mod directory and launch the game.

## Known Issues

Markdown Syntax Highlight has many limitations.

Say command cannot render colored text (Need more info).

## Changelog

### Version 0.2.5

- Add more config entries.
- Add get afk and get travel command.
- Now you can enable the death messages separately for hostile and passive entities.

### Version 0.2.4

- Player Teleport Message
- Fix command `/f get distance` yaw calculation error.
- Optimize i18n.

### Version 0.2.3

- Player Travel Message
- Fix NullPointerException issue when console execute /f say

### Version 0.2.2

- Now say command supports more custom placeholders
- Now even server translation is enabled, the mod will only translate its own text
- Fix the bug that say command cannot render colored text

### Version 0.2.1

- Show message to player when it is the time to sleep
- Add some flow nodes

### Version 0.2.0

Important Update: Custom Logic Flow

- Use command /f flow to build simple algorithm without need to restart the game.
- Save your custom flow into a file and load it back at any time.
- The .flow file should be put into the folder config/fminecraftmod.

### Version 0.1.4

- Song playback speed
- Show song playback status in Actionbar
- And jump to a specific time point of a song

### Version 0.1.3

- Share your information to other players.
- Fix bugs.

### Version 0.1.2

Noteblock Song Update

- Use command /f song to play noteblock song to target players.
- The .nbs file should be put into the folder config/fminecraftmod.

### Version 0.1.1

Get Command Update

- List the inventory of a certain player.
- Get the coordinates of chosen entities.

### Version 0.1

First release of the mod

- Monitor the number of the entities in the world.
- Add more player combat related messages.

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
