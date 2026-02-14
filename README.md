# F Minecraft Mod

English [中文](https://github.com/yknBugs/FMinecraftMod/blob/main/README_zh.md)

A Minecraft Mod for Management.

## Note

This is a **server-side mod** that works even if clients don't have it installed.

All features are disabled by default and can be configured using commands or the config file.

## Features

### Notifications

Track events that are useful for admins or your friends.

- Entity count warnings when threshold exceeded
- Automatic entity density analysis
- Player death coordinates
- Boss, named entity, and killer mob deaths
- Detect AFK status
- Projectile hit messages with distance and HP
- Boss fight information
- Fast travel detection
- Teleportation notifications
- Biome change notifications
- Sleep time reminders

All the notification messages are configurable.

### Player Information Commands

Get and share detailed player information:

- Coordinates and distance
- Health, hunger, saturation, xp
- Inventory contents (with hover details)
- Current items held
- travel statistics

### Note Block Song Player

Play Note Block Studio (`.nbs`) songs to players in-game.

- Play songs to specific players or all online players
- Control playback speed and position
- Show/hide progress in action bar
- Pause, resume, and stop playback

### GPT Integration

Chat with Large Language Models directly in-game with markdown syntax highlighting.

- OpenAI-compatible API support
- Conversation history management
- System prompts and temperature control
- Code block syntax highlighting for multiple languages

### Flow System

Build custom automation with an in-game node-based flowchart engine. Create, edit, and execute logic flows without restarting the server.

- Event triggers (player actions, entity events, scheduled tasks, etc.)
- Arithmetic, conditional, and data manipulation nodes
- Save and load flows as `.flow` files

## Usage Guide

All commands start with `/f` and require OP permissions unless otherwise noted.

### Basic Commands

- `/f` - Display mod version information
- `/f reload` - Reload configuration file
- `/f options` - View or change mod settings

### Say Command

Broadcast messages to all players with rich text formatting and dynamic placeholders (no OP required):

```text
/f say <message>
```

**Formatting Codes:**

Use Minecraft color codes with `&` prefix:

- `&0-&f` - Colors
- `&l` - Bold, `&o` - Italic, `&n` - Underline, `&m` - Strikethrough, `&k` - Obfuscated
- `&r` - Reset formatting

**Custom Styles:**

- `${color:RRGGBB}`     - Apply hex color (e.g., `${color:FF5733}`)
- `${link:url}`         - Create clickable URL link
- `${copy:text}`        - Create click-to-copy text
- `${hint:text}`        - Add hover tooltip
- `${suggest:command}`  - Suggest command on click
- `${markdown}`         - Parse content as Markdown

**Player Placeholders:**

- `${player}`                   - Player display name
- `${health}`, `${hp}`          - Current health
- `${maxhealth}`, `${maxhp}`    - Maximum health
- `${level}`                    - Experience level
- `${hunger}`                   - Hunger level
- `${saturation}`               - Saturation level
- `${x}`, `${y}`, `${z}`        - Player coordinates
- `${pitch}`, `${yaw}`          - Player rotation
- `${biome}`                    - Current biome
- `${coord}`                    - Formatted coordinates
- `${mainhand}`, `${offhand}`   - Item in hand

**Examples:**

- `/f say &aHello &b${player}&a! Your HP is ${hp}/${maxhp}`
- `/f say ${color:FF5733}Warning! ${player} is at ${coord}`
- `/f say Check this out: ${link:https://example.com}${hint:Click to open}Click here!`
- `/f say ${suggest:/tp @s yourName}${hint:Click to suggest teleport}Click to teleport to my location`

### GPT Commands

Configure the GPT server URL and API key in the config first.

- `/f gpt new <message>`        - Start a new conversation
- `/f gpt reply <message>`      - Reply in current conversation
- `/f gpt regenerate`           - Regenerate last response
- `/f gpt edit <index> <text>`  - Edit a previous message
- `/f gpt history [page]`       - View conversation history

### Information Commands

Get information about other players:

- `/f get coord <player>`             - Get player coordinates
- `/f get distance <player>`          - Get distance to player
- `/f get health <player>`            - Get player health
- `/f get status <player>`            - Get comprehensive status
- `/f get inventory <player>`         - View player inventory
- `/f get item <player>`              - See held item
- `/f get crowd [number] [radius]`    - Find most crowded entity area

Share your own information (no OP required):

- `/f share coord`              - Share your coordinates
- `/f share distance`           - Share distance to others
- `/f share health`             - Share your health
- `/f share status`             - Share comprehensive status
- `/f share inventory`          - Share your inventory
- `/f share item`               - Share held item

### Note Block Song Commands

Place `.nbs` files in `config/fminecraftmod/` folder, then:

- `/f song play <players> <song>`             - Play a song
- `/f song get <players>`                     - Check playback status
- `/f song cancel <players>`                  - Stop playback
- `/f song seek <players> <time>`             - Jump to specific time (seconds)
- `/f song speed <players> <multiplier>`      - Set playback speed (e.g., 1.5)

### Flow Commands

Create and manage custom automation flows:

- `/f flow create <name> <event_type> <first_node_name>`  - Create new flow
- `/f flow list`                                          - List all flows
- `/f flow enable <name> [true|false]`                    - Enable/disable flow
- `/f flow rename <old> <new>`                            - Rename flow
- `/f flow copy <source> <target>`                        - Copy flow
- `/f flow delete <name>`                                 - Delete flow

Edit flows:

- `/f flow edit <flow> new <node_type> <node_name>`                 - Add node
- `/f flow edit <flow> remove <node_name>`                          - Remove node
- `/f flow edit <flow> rename <old> <new>`                          - Rename node
- `/f flow edit <flow> const <node> <input_index> <value>`          - Set constant input
- `/f flow edit <flow> reference <node> <input> <source> <output>`  - Connect nodes
- `/f flow edit <flow> disconnect <node> <input>`                   - Disconnect input
- `/f flow edit <flow> next <node> <branch> <target>`               - Set next node
- `/f flow edit <flow> final <node> <branch>`                       - Mark as final node
- `/f flow edit <flow> undo`                                        - Undo last edit
- `/f flow edit <flow> redo`                                        - Redo last undo

Save and load flows:

- `/f flow save <name>`               - Save to file (config/fminecraftmod/)
- `/f flow save *`                    - Save all flows
- `/f flow load <filename>`           - Load from file
- `/f flow load *`                    - Load all .flow files

Trigger and execution:

- `/f flow history [page]`              - View execution history
- `/f flow log <index>`                 - View detailed execution log
- `/f trigger <trigger_name> [args]`    - Execute trigger node (no OP required)

### Configuration Examples

Enable features using `/f options`:

- `/f options serverTranslation [true|false]`           - Server-side translations
- `/f options entityDeathMessage [location]`            - Entity death messages
- `/f options passiveDeathMessage [location]`           - Passive entity deaths
- `/f options hostileDeathMessage [location]`           - Hostile entity deaths
- `/f options playerDeathCoordLocation [location]`      - Player death coordinates
- `/f options gptUrl [url]`                             - GPT API endpoint
- etc.

Hint: Use Tab to auto complete the command.

## Installation

### For Players

1. Download the mod JAR file from releases.
2. Place it in your Minecraft `mods` folder.
3. Launch the game

### For Developers

#### Step 1: Clone this repository

```bash
git clone https://github.com/yknBugs/FMinecraftMod
cd FMinecraftMod
```

#### Step 2: Build this project

```bash
./gradlew build
```

If you encounter an error, add the following line to `gradle.properties`:

```text
org.gradle.java.home=<path_to_your_JDK17>
```

#### Step 3: Install

The compiled mod file will be in `./build/libs/`. Copy it to your Minecraft mods folder and launch the game.

## Configuration

After first launch, a configuration file will be created at `config/fminecraftmod/server.json`.

You can edit this file directly or use `/f options` commands in-game.

You can use `/f reload` command to reload the config from file.

**Note:** Place `.nbs` song files and `.flow` automation files in the `config/fminecraftmod/` folder.

## Supported Version

- Minecraft 1.20.1 Fabric
- Minecraft 1.20.1 Forge (Beta)

## Known Issues

- Markdown syntax highlighting has some limitations with complex code blocks

## Changelog

### Version 0.3 (Current)

**Major Features:**

- Extended logic flow system with many new node types:
  - RunCommandNode for executing Minecraft commands
  - UnaryArithmeticNode for single-operand operations
  - GetNbtValueNode, GetEntityDataNode for data retrieval
  - GetWorldListNode, GatherEntityNode, GetBlockNode for world queries
- Enhanced BinaryArithmeticNode and BroadcastMessageNode
- List type support in logic flows
- Scheduled flow variable addition capabilities
- Travel and teleport detection features
- Player travel statistics tracking
- Entity density calculation with crowded area detection
- Ability to replace event nodes in flows
- Trigger event nodes callable by non-OP players

**Improvements:**

- Travel check no longer resets history coordinates
- Infinite recursion prevention in logic flows
- Separate death message configuration for hostile and passive entities
- Additional config entries
- AFK and travel information commands

**Bug Fixes:**

- Fixed `/f get distance` yaw calculation error
- Fixed NullPointerException when console executes `/f say`
- Fixed say command colored text rendering

### Version 0.2

**Major Features:**

- In-game node-based flowchart engine
  - Create, edit, and manage flows with `/f flow` commands
  - Save flows to `.flow` files for persistence
  - Event-driven execution system
- Play `.nbs` files to players
  - Playback controls (play, pause, stop, seek)
  - Adjustable playback speed
  - Progress display in action bar

### Version 0.1

**Initial Features:**

- Chat with AI in-game
  - OpenAI-compatible API support
  - Conversation history and management
  - System prompts and temperature control
  - Markdown syntax highlighting
- Player Information System:
  - `/f get` commands for player data (coordinates, health, inventory, etc.)
  - `/f share` commands for sharing information
  - Distance and direction calculations
- Entity Monitoring:
  - Entity count tracking and warnings
  - Configurable thresholds
- Combat Features:
  - Projectile hit messages with distance and HP
  - Boss fight notifications
  - Monster surround warnings
  - Enhanced death messages
- AFK Detection:
  - Automatic AFK player detection
  - Configurable thresholds
  - Broadcast and notification system
- Additional Features:
  - Biome change notifications
  - Player death coordinate broadcasting
  - Configuration system with `/f options`
  - Customizable message locations and receivers
