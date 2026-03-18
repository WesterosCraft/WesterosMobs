# WesterosMobs

A Fabric 1.21.1 Minecraft mod for the WesterosCraft server that adds a mount system. Server-side only.

## Mount System

The `/horse` command spawns or despawns a tamed, saddled horse for the player.

- **Summon**: `/horse` — spawns a horse with a random appearance (color + marking). If a horse is already out, teleports it to the player.
- **Set name**: `/horse setname <name>` — sets or changes the horse's name. If a horse is currently spawned, updates the name tag live.
- **Auto-despawn**: Mounts are automatically removed when the player disconnects or changes dimensions.
- **Name persistence**: Horse names are saved per-player and persist across server restarts via mixin-injected NBT data.
- **Invulnerable**: Mounts cannot be killed by damage.

## Permissions

Integrates optionally with [LuckPerms](https://luckperms.net/). When LuckPerms is not installed, all features are available to players with OP level 2+.

| Permission | Description |
|---|---|
| `westerosmobs.horse` | Use the `/horse` command |

## Configuration

A config file is generated at `config/westerosmobs.json` on first run:

```json
{
  "mountEnabled": true
}
```

## Building

Requires Java 21.

```bash
./gradlew build        # Build the mod JAR (outputs to build/libs/)
./gradlew runServer    # Run a development server with the mod loaded
./gradlew clean        # Clean build artifacts
```
