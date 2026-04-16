package com.westeroscraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.westeroscraft.config.WesterosMobsConfig;
import com.westeroscraft.mount.MountCommand;
import com.westeroscraft.mount.MountManager;

public class WesterosMobs implements ModInitializer {
	public static final String MOD_ID = "westerosmobs";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing WesterosMobs...");

		WesterosMobsConfig.load();
		MountManager.init();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			MountCommand.register(dispatcher);
		});

		LOGGER.info("WesterosMobs initialized!");
	}
}
