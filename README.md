# WesterosMobs

A Fabric 1.21.1 Minecraft mod for the WesterosCraft server that adds a pet manager and mount system.

## Features

### Pet System (`/wcm`)

Players can own and manage pets that persist across server restarts. Each player is limited to one pet per type.

- **Pet Types**: Horse and Direwolf (using vanilla entities with custom behavior)
- **Commands**:
  - `/wcm summon [type]` — Summon a pet or create a new one
  - `/wcm dismiss` — Despawn your active pet
  - `/wcm list` — List your owned pets
  - `/wcm select <number>` — Switch active pet
  - `/wcm rename <name>` — Rename your active pet
  - `/wcm info` — View pet details
  - `/wcm release <number>` — Permanently release a pet
- **Admin Commands**:
  - `/wcm admin give <player> <type>` — Give a pet to a player
  - `/wcm admin remove <player> <number>` — Remove a pet from a player

Pets are immortal — they despawn instead of dying. They also despawn automatically when the owner disconnects or changes dimensions, and can be re-summoned at any time.

### Mount System (`/mount`)

A simple toggle command that spawns or despawns a temporary saddled horse. Mounts are not persistent — they exist only for the current session and despawn on disconnect or dimension change.

### Permissions

Integrates optionally with [LuckPerms](https://luckperms.net/). When LuckPerms is not installed, all features are available by default.

| Permission | Description |
|---|---|
| `westerosmobs.mount` | Use the `/mount` command |
| `westerosmobs.pet` | Use the `/wcm` commands |
| `westerosmobs.pet.<type>` | Create a specific pet type |
| `westerosmobs.pet.admin` | Use admin pet commands |

### Configuration

A config file is generated at `config/westerosmobs.json` on first run:

```json
{
  "mountEnabled": true,
  "petEnabled": true
}
```