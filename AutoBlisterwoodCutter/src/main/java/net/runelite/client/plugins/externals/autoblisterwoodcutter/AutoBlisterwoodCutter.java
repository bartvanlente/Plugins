/*
 * Copyright (c) 2019-2020, ganom <https://github.com/Ganom>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.externals.autoblisterwoodcutter;

import com.google.inject.Provides;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.Point;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.externals.utils.ExtUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
		name = "Auto blisterwood cutter",
		enabledByDefault = false,
		type = PluginType.UTILITY
)
@Slf4j
public class AutoBlisterwoodCutter extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private AutoBlistwoodCutterConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ExtUtils extUtils;

	private ExecutorService executorService;
	private Random random;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private boolean flash;

	@Provides
	AutoBlistwoodCutterConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AutoBlistwoodCutterConfig.class);
	}

	@Override
	protected void startUp()
	{
		executorService = Executors.newSingleThreadExecutor();
		random = new Random();
	}

	@Override
	protected void shutDown()
	{
		executorService.shutdown();
		random = null;
	}

	@Subscribe
	private void AnimationChanged(AnimationChanged event)
	{
		log.info(event.getActor().toString());
	}
}