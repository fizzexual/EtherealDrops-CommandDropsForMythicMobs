# EtherealDrops

A powerful damage tracking and reward system for MythicMobs. Track player damage, display victory messages, and reward top performers with items or commands.

**Author:** Fizzexual

---

## Features

- Track damage to MythicMobs bosses in real-time
- Position-based rewards (1st, 2nd, 3rd place, etc.)
- Item rewards with inventory or ground drop options
- Command rewards with placeholder support
- Victory messages with MiniMessage formatting
- Victory holograms via FancyHolograms
- PlaceholderAPI integration
- SQLite or MySQL database support

---

## Requirements

**Required:**
- Minecraft 1.19+
- Java 17+
- MythicMobs 5.0+
- PlaceholderAPI 2.11+
- FancyHolograms 2.0+

**Optional:**
- Vault (for player prefixes)
- LuckPerms (for advanced prefixes)

---

## Installation

1. Download the latest release
2. Place `EtherealDrops.jar` in your `plugins` folder
3. Install required dependencies
4. Restart your server
5. Configure in `plugins/EtherealDrops/`

---

## Quick Start

### Track a Boss

Edit `tracked_bosses.yml`:

```yaml
bosses:
  MyBoss:
    enabled: true
    top_players_shown: 3
    broadcast: true
    hologram: true
```

### Add Rewards

Edit `rewards.yml`:

```yaml
bosses:
  MyBoss:
    enabled: true
    rewards:
      - position: 1
        type: command
        command: "give {player} minecraft:diamond 10"
```

### Customize Messages

Edit `messages.yml`:

```yaml
victory:
  default:
    - "<gold><bold>BOSS DEFEATED!</bold></gold>"
    - "<yellow>{boss_name} has been slain!</yellow>"
```

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ed reload` | Reload configuration | `etherealdrops.reload` |
| `/ed check <boss>` | Check your damage | `etherealdrops.check` |
| `/ed top <boss>` | View leaderboard | `etherealdrops.checktop` |
| `/ed clear <boss>` | Clear boss data | `etherealdrops.cleardata` |
| `/ed help` | Show help menu | `etherealdrops.help` |

---

## Placeholders

Use with PlaceholderAPI:

- `%etherealdrops_damage_<boss>%` - Your damage to boss
- `%etherealdrops_position_<boss>%` - Your leaderboard position
- `%etherealdrops_percentage_<boss>%` - Your damage percentage
- `%etherealdrops_top1_<boss>%` - Top player name
- `%etherealdrops_top1_damage_<boss>%` - Top player damage

---

## Reward Types

### Command Rewards

```yaml
- position: 1
  type: command
  command: "give {player} minecraft:diamond 10"
```

**Placeholders:** `{player}`, `{boss}`, `{position}`

### Item Rewards (Coming Soon)

```yaml
- position: 1
  type: item
  material: DIAMOND
  amount: 10
  inventory: true
  glow: true
```

---

## Configuration Files

| File | Purpose |
|------|---------|
| `config.yml` | Main settings, database, display options |
| `tracked_bosses.yml` | Define which bosses to track |
| `rewards.yml` | Position-based rewards per boss |
| `messages.yml` | Victory messages and formats |
| `holograms.yml` | Hologram templates and settings |

---

## Example Setup

```yaml
# tracked_bosses.yml
bosses:
  EpicDragon:
    enabled: true
    top_players_shown: 5
    broadcast: true
    hologram: true

# rewards.yml
bosses:
  EpicDragon:
    enabled: true
    rewards:
      - position: 1
        type: command
        command: "give {player} minecraft:elytra 1"
      
      - position: 1
        type: command
        command: "say {player} defeated {boss} and got 1st place!"
      
      - position: 2
        type: command
        command: "give {player} minecraft:diamond 10"
      
      - position: 3
        type: command
        command: "give {player} minecraft:gold_ingot 20"
```

---

## Support

- **Issues:** https://github.com/fizzexual/EtherealDrops/issues

---

## License

MIT License

**Created by Fizzexual**
