/*
 * Copyright (c) 2019-2020, bartvanlente <https://github.com/bartvanlente>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.externals.autoblisterwoodcutter;

import com.google.inject.Provides;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.AnimationID;
import net.runelite.api.InventoryID;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.ItemContainer;
import net.runelite.api.Item;
import net.runelite.api.ChatMessageType;
import net.runelite.api.queries.InventoryWidgetItemQuery;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.externals.utils.ExtUtils;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.HotkeyListener;
import org.pf4j.Extension;
import net.runelite.client.game.ItemManager;

@Extension
@PluginDescriptor(
		name = "Auto blisterwood cutter",
		enabledByDefault = false,
		type = PluginType.UTILITY
)
@Slf4j
public class AutoBlisterwoodCutter extends Plugin
{
	private static final int BLISTERWOOD_TREE = 37989;

	static final Set<Integer> REGION_IDS = Stream.of(14133,
			14388,
			14389,
			14644,
			14645)
			.collect(Collectors.toCollection(HashSet::new));

	@Inject
	private Client client;

	@Inject
	private AutoBlistwoodCutterConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MenuManager menuManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ExtUtils utils;

	private boolean idle;
	private int randomLogsInInventory;
	private boolean iterating;
	private int iterTicks;

	private final List<WidgetItem> items = new ArrayList<>();
	private final Set<Integer> ids = new HashSet<>();
	private final Set<String> names = new HashSet<>();
	private List<GameObject> BlistwoodTree = new ArrayList<>();

	private Robot robot;
	private boolean run = false;
	private BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1);
	private ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, 1, 25, TimeUnit.SECONDS, queue,
			new ThreadPoolExecutor.DiscardPolicy());

	@Provides
	AutoBlistwoodCutterConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AutoBlistwoodCutterConfig.class);
	}

	private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle())
	{
		@Override
		public void hotkeyPressed()
		{
			run = !run;

			if (client.getGameState() == GameState.LOGGED_IN)
			{
				if (run)
				{
					sendMessage("Auto blisterwood cutter Activated");
				}
				else
				{
					sendMessage("Auto blisterwood cutter De-Activated");
				}
			}
		}
	};

	@Override
	protected void startUp() throws AWTException
	{
		robot = new Robot();

		keyManager.registerKeyListener(toggle);

		updateConfig();

		if (randomLogsInInventory == 0)
		{
			randomLogsInInventory = getRandomNumberInRange(9, 24);
		}
	}

	@Override
	protected void shutDown()
	{
		executorService.shutdown();
		keyManager.unregisterKeyListener(toggle);
		robot = null;
	}

	@Subscribe
	private void OnGameTick(GameTick event)
	{
		if (!isInBlisterwoodRegion() && !run)
		{
			return;
		}

		if (idle)
		{
			log.info("idle");

			try
			{
				Thread.sleep((long) (Math.random() * 2000 + 4000));
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			for (GameObject object : BlistwoodTree)
			{
				executorService.submit(() ->
				{
					utils.click(object.getCanvasLocation());
				});
			}

			idle = false;
		}
		else
		{
			log.info("niet iiiiiidle");
		}

		if (calculateInventory() >= randomLogsInInventory)
		{
			List<WidgetItem> list = new InventoryWidgetItemQuery()
					.idEquals(ids)
					.result(client)
					.list;

			items.addAll(list);

			if (items.isEmpty())
			{
				if (iterating)
				{
					iterTicks++;
					if (iterTicks > 10)
					{
						iterating = false;
						clearNames();
					}
				}
				else
				{
					if (iterTicks > 0)
					{
						iterTicks = 0;
					}
				}
				return;
			}

			dropItems(items);
			items.clear();
			randomLogsInInventory = getRandomNumberInRange(9, 24);
		}
	}

	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!isInBlisterwoodRegion() && !run)
		{
			return;
		}

		GameObject gameObject = event.getGameObject();

		addGameObject(gameObject);
	}

	@Subscribe
	private void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (!isInBlisterwoodRegion() && !run)
		{
			return;
		}

		GameObject gameObject = event.getGameObject();

		removeGameObject(gameObject);
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged event)
	{
		if (!isInBlisterwoodRegion() && !run)
		{
			return;
		}

		Player localPlayer = client.getLocalPlayer();

		if (localPlayer != event.getActor())
		{
			return;
		}

		int animation = localPlayer.getAnimation();

		if (animation == AnimationID.IDLE)
		{
			log.info("wordt idle");
			idle = true;
		}
		else
		{
			log.info("wordt niet meer idle");
			idle = false;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (!isInBlisterwoodRegion() && !run)
		{
			return;
		}

		if (event.getItemContainer() != client.getItemContainer(InventoryID.INVENTORY))
		{
			return;
		}

		int quant = 0;

		for (Item item : event.getItemContainer().getItems())
		{
			if (ids.contains(item.getId()))
			{
				quant++;
			}
		}

		if (iterating && quant == 0)
		{
			iterating = false;
			clearNames();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (!isInBlisterwoodRegion() && !run)
		{
			return;
		}

		if (event.getGameState() == GameState.LOGGED_IN)
		{
			updateConfig();
		}
	}

	private void addGameObject(GameObject gameObject)
	{
		if (gameObject.getId() == BLISTERWOOD_TREE)
		{
			BlistwoodTree.add(gameObject);
		}
	}

	private void removeGameObject(GameObject gameObject)
	{
		if (gameObject.getId() == BLISTERWOOD_TREE)
		{
			BlistwoodTree.remove(gameObject);
		}
	}

	private Integer calculateInventory()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			return null;
		}

		Item[] result = inventory.getItems();

		int amount = 0;

		for (Item item : result)
		{
			if (item.getId() == 24691)
			{
				amount++;
			}
		}

		return amount;
	}

	private static int getRandomNumberInRange(int min, int max)
	{
		Random r = new Random();
		return r.ints(min, (max + 1)).limit(1).findFirst().getAsInt();
	}

	private void dropItems(List<WidgetItem> dropList)
	{
		iterating = true;

		for (String name : names)
		{
			menuManager.addPriorityEntry("drop", name);
			menuManager.addPriorityEntry("release", name);
			menuManager.addPriorityEntry("destroy", name);
		}

		List<Rectangle> rects = new ArrayList<>();

		for (WidgetItem item : dropList)
		{
			rects.add(item.getCanvasBounds());
		}

		executorService.submit(() ->
		{
			for (Rectangle rect : rects)
			{
				utils.click(rect);

				try
				{
					Thread.sleep((int) getMillis());
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	private void updateConfig()
	{
		ids.clear();

		for (int i : utils.stringToIntArray("24691"))
		{
			ids.add(i);
		}

		clearNames();

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			names.clear();

			for (int i : ids)
			{
				final String name = Text.standardize(itemManager.getItemDefinition(i).getName());
				names.add(name);
			}
		}
	}

	private void clearNames()
	{
		for (String name : names)
		{
			menuManager.removePriorityEntry("drop", name);
			menuManager.removePriorityEntry("release", name);
			menuManager.removePriorityEntry("destroy", name);
		}
	}

	private long getMillis()
	{
		return (long) (Math.random() * config.randLow() + config.randHigh());
	}

	private boolean isInBlisterwoodRegion()
	{
		return REGION_IDS.contains(client.getMapRegions()[0]);
	}

	private void sendMessage(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", ColorUtil.wrapWithColorTag(message, Color.MAGENTA), "");
	}
}