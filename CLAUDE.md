# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WesterosMobs is a Fabric 1.21.1 Minecraft mod for the WesterosCraft server that adds custom mobs and a pet manager system. The mod is currently in its initial scaffolding stage with the basic Fabric mod structure in place.

- **Mod ID**: `westerosmobs`
- **Minecraft**: 1.21.1
- **Fabric Loader**: 0.18.4
- **Java**: 21

## Build Commands

```bash
# Build the mod JAR (outputs to build/libs/)
./gradlew build

# Run development client with mod loaded
./gradlew runClient

# Run development server with mod loaded
./gradlew runServer

# Generate mod data files (models, textures, loot tables)
./gradlew runDatagen

# Clean build artifacts
./gradlew clean
```

## Architecture

### Entry Points

- **Main**: `WesterosMobs.java` implements `ModInitializer` - runs on mod startup, this is where mob registration and pet manager initialization should occur
- **Data Generation**: `WesterosMobsDataGenerator.java` implements `DataGeneratorEntrypoint` - generates asset files during `runDatagen`

### Mixin System

Mixins are in `com.westeroscraft.mixin` package, configured via `westerosmobs.mixins.json`. The example mixin hooks into `MinecraftServer.loadWorld()` for server-side initialization.

### Key Files

- `src/main/resources/fabric.mod.json` - Mod manifest defining entry points, dependencies, and metadata
- `src/main/resources/westerosmobs.mixins.json` - Mixin configuration (Java 21 compatibility, strict injection)
- `gradle.properties` - Version numbers for Minecraft, Fabric, and mod dependencies

### Package Structure (Expected)

The mod should be organized as:
- `com.westeroscraft` - Core mod initialization
- `com.westeroscraft.mixin` - Minecraft bytecode mixins
- `com.westeroscraft.pet` - Pet manager system (to be implemented)
- `com.westeroscraft.mob` - Custom mob entities and registration (to be implemented)

## Fabric Development Notes

- Entity registration happens in `onInitialize()` using Fabric's Registry system
- Custom entities need both server-side logic and client-side rendering registration
- Use Fabric Events API for player join/quit, entity spawn/death events
- Pet/mob data persistence uses Minecraft's NBT system and world save data
- Data generators create JSON files for models, textures metadata, and loot tables
