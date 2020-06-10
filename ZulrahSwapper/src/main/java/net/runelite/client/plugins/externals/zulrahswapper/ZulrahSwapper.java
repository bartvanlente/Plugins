/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2017, Devin French <https://github.com/devinfrench>
 * Copyright (c) 2019, Ganom <https://github.com/ganom>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.externals.zulrahswapper;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.Actor;
import net.runelite.api.AnimationID;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.Skill;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.vars.InterfaceTab;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SoundManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.externals.utils.ExtUtils;
import net.runelite.client.plugins.externals.utils.Tab;
import net.runelite.client.plugins.externals.zulrahswapper.overlays.ZulrahCurrentPhaseOverlay;
import net.runelite.client.plugins.externals.zulrahswapper.overlays.ZulrahNextPhaseOverlay;
import net.runelite.client.plugins.externals.zulrahswapper.overlays.ZulrahOverlay;
import net.runelite.client.plugins.externals.zulrahswapper.overlays.ZulrahPrayerOverlay;
import net.runelite.client.plugins.externals.zulrahswapper.patterns.ZulrahPattern;
import net.runelite.client.plugins.externals.zulrahswapper.patterns.ZulrahPatternA;
import net.runelite.client.plugins.externals.zulrahswapper.patterns.ZulrahPatternB;
import net.runelite.client.plugins.externals.zulrahswapper.patterns.ZulrahPatternC;
import net.runelite.client.plugins.externals.zulrahswapper.patterns.ZulrahPatternD;
import net.runelite.client.plugins.externals.zulrahswapper.phase.ZulrahPhase;
import net.runelite.client.plugins.externals.zulrahswapper.phase.ZulrahType;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Prayer;
import net.runelite.api.VarClientInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import com.google.common.base.Splitter;
import java.awt.Color;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Clipboard;

@Extension
@PluginDescriptor(
		name = "Zulrah Pray Swapper",
		description = "Automatically swaps prayers for zulrah",
		tags = {"prayer", "zulrah", "bot", "swap"},
		type = PluginType.PVM
)
@Slf4j
public class ZulrahSwapper extends Plugin
{
	private static final Splitter NEWLINE_SPLITTER = Splitter
			.on("\n")
			.omitEmptyStrings()
			.trimResults();

	private static final ZulrahPattern[] patterns = new ZulrahPattern[]
			{
					new ZulrahPatternA(),
					new ZulrahPatternB(),
					new ZulrahPatternC(),
					new ZulrahPatternD()
			};

	@Getter(AccessLevel.PACKAGE)
	private NPC zulrah;

	@Inject
	private Client client;

	@Inject
	private ZulrahConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SoundManager soundManager;

	@Inject
	private ZulrahCurrentPhaseOverlay currentPhaseOverlay;

	@Inject
	private ZulrahNextPhaseOverlay nextPhaseOverlay;

	@Inject
	private ZulrahPrayerOverlay zulrahPrayerOverlay;

	@Inject
	private ZulrahOverlay zulrahOverlay;

	private ZulrahInstance instance;

	int count;

	@Inject
	private ExtUtils utils;

	private BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1);
	private ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, 1, 2, TimeUnit.SECONDS, queue,
			new ThreadPoolExecutor.DiscardPolicy());
	private Robot robot;
	private ExecutorService executor;

	@Provides
	ZulrahConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ZulrahConfig.class);
	}

	@Override
	protected void startUp() throws AWTException
	{
		executor = Executors.newSingleThreadExecutor();
		robot = new Robot();

		overlayManager.add(currentPhaseOverlay);
		overlayManager.add(nextPhaseOverlay);
		overlayManager.add(zulrahPrayerOverlay);
		overlayManager.add(zulrahOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(currentPhaseOverlay);
		overlayManager.remove(nextPhaseOverlay);
		overlayManager.remove(zulrahPrayerOverlay);
		overlayManager.remove(zulrahOverlay);
		zulrah = null;
		instance = null;
		robot = null;
	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (zulrah == null)
		{
			if (instance != null)
			{
				log.debug("Zulrah encounter has ended.");
				instance = null;
			}
			return;
		}

		if (instance == null)
		{
			instance = new ZulrahInstance(zulrah);
			log.debug("Zulrah encounter has started.");
		}

		ZulrahPhase currentPhase = ZulrahPhase.valueOf(zulrah, instance.getStartLocation());

		if (instance.getPhase() == null)
		{
			instance.setPhase(currentPhase);
		}
		else if (!instance.getPhase().equals(currentPhase))
		{
			ZulrahPhase previousPhase = instance.getPhase();
			instance.setPhase(currentPhase);
			instance.nextStage();

			log.debug("Zulrah phase has moved from {} -> {}, stage: {}", previousPhase, currentPhase, instance.getStage());
		}

		ZulrahPattern pattern = instance.getPattern();

		if (pattern == null)
		{
			int potential = 0;
			ZulrahPattern potentialPattern = null;

			for (ZulrahPattern p : patterns)
			{
				if (p.stageMatches(instance.getStage(), instance.getPhase()))
				{
					potential++;
					potentialPattern = p;
				}
			}

			if (potential == 1)
			{
				log.debug("Zulrah pattern identified: {}", potentialPattern);

				instance.setPattern(potentialPattern);
			}
		}
		else if (pattern.canReset(instance.getStage()) && (instance.getPhase() == null || instance.getPhase().equals(pattern.get(0))))
		{
			log.debug("Zulrah pattern has reset.");

			instance.reset();
		}

		if (instance == null)
		{
			return;
		}

		if (instance.getPhase() == null || instance.getNextPhase() == null)
		{
			return;
		}

		if (instance.getPhase().getPrayer() != null)
		{
			if (!client.isPrayerActive(instance.getPhase().getPrayer()))
			{
				clickPrayer(instance.getPhase().getPrayer());
			}
		}

		switch (instance.getPhase().getType())
		{
			case RANGE:
			case MELEE:
				if (!client.isPrayerActive(config.overheadPrayerMage()))
				{
					clickPrayer(config.overheadPrayerMage());
				}
				break;
			case MAGIC:
				if (!client.isPrayerActive(config.overheadPrayerRange()))
				{
					clickPrayer(config.overheadPrayerRange());
				}
		}
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged event)
	{
		if (instance == null)
		{
			return;
		}

		ZulrahPhase currentPhase = instance.getPhase();
		ZulrahPhase nextPhase = instance.getNextPhase();

		if (currentPhase == null || nextPhase == null)
		{
			return;
		}

		final Actor actor = event.getActor();

		if (zulrah != null && zulrah.equals(actor) && zulrah.getAnimation() == AnimationID.ZULRAH_PHASE)
		{
			ZulrahType type = nextPhase.getType();

			switch (type)
			{
				case RANGE:
				case MELEE:
					decode(config.mage());
					break;

				case MAGIC:
					decode(config.range());
					break;
			}
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (event.getCommand().equalsIgnoreCase("copycs"))
		{
			final ItemContainer e = client.getItemContainer(InventoryID.EQUIPMENT);

			if (e == null)
			{
				log.error("CopyCS: Can't find equipment container.");
				return;
			}

			final StringBuilder sb = new StringBuilder();

			for (Item item : e.getItems())
			{
				if (item.getId() == -1 || item.getId() == 0)
				{
					continue;
				}

				sb.append(item.getId());
				sb.append(":");
				sb.append("Equip");
				sb.append("\n");
			}

			final String string = sb.toString();
			Clipboard.store(string);
		}
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (npc != null && npc.getName() != null &&
				npc.getName().toLowerCase().contains("zulrah"))
		{
			zulrah = npc;
		}
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (npc != null && npc.getName() != null &&
				npc.getName().toLowerCase().contains("zulrah"))
		{
			zulrah = null;
		}
	}

	public ZulrahInstance getInstance()
	{
		return instance;
	}

	private void clickPrayer(Prayer prayer)
	{
		if (client.isPrayerActive(prayer) || client.getBoostedSkillLevel(Skill.PRAYER) < 1)
		{
			return;
		}

		final Widget widget = client.getWidget(prayer.getWidgetInfo());

		if (widget == null)
		{
			log.error("Olm: Unable to find prayer widget.");
			return;
		}

		final Rectangle bounds = widget.getBounds();

		executor.submit(() ->
		{
			if (client.getVar(VarClientInt.INTERFACE_TAB) != InterfaceTab.PRAYER.getId())
			{
				robot.keyPress(utils.getTabHotkey(Tab.PRAYER));
				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException e)
				{
					return;
				}
			}

			utils.click(bounds);

			try
			{
				Thread.sleep(getMillis());
			}
			catch (InterruptedException e)
			{
				return;
			}

			if (client.isPrayerActive(prayer))
			{
				robot.keyPress(utils.getTabHotkey(Tab.INVENTORY));
			}

			try
			{
				Thread.sleep(getMillis());
			}
			catch (InterruptedException ignored)
			{
			}
		});
	}

	private void decode(String string)
	{
		final Map<String, String> map = new LinkedHashMap<>();
		final List<Pair<Tab, Rectangle>> rectPairs = new ArrayList<>();
		final Iterable<String> tmp = NEWLINE_SPLITTER.split(string);

		for (String s : tmp)
		{
			String[] split = s.split(":");
			try
			{
				map.put(split[0], split[1]);
			}
			catch (IndexOutOfBoundsException e)
			{
				log.error("Decode: Invalid Syntax in decoder.");
				dispatchError("Invalid Syntax in decoder.");
				return;
			}
		}

		for (Map.Entry<String, String> entry : map.entrySet())
		{
			String param = entry.getKey();
			String command = entry.getValue().toLowerCase();

			switch (command)
			{
				case "equip":
				{
					final Rectangle rect = invBounds(Integer.parseInt(param));

					if (rect == null)
					{
						log.debug("Equip: Can't find valid bounds for param {}.", param);
						continue;
					}

					rectPairs.add(Pair.of(Tab.INVENTORY, rect));
				}
				break;
				case "clean":
				{
					final List<Rectangle> rectangleList = listOfBounds(Integer.parseInt(param));

					if (rectangleList.isEmpty())
					{
						log.debug("Clean: Can't find valid bounds for param {}.", param);
						continue;
					}

					for (Rectangle rect : rectangleList)
					{
						rectPairs.add(Pair.of(Tab.INVENTORY, rect));
					}
				}
				break;
				case "remove":
				{
					final Rectangle rect = equipBounds(Integer.parseInt(param));

					if (rect == null)
					{
						log.debug("Remove: Can't find valid bounds for param {}.", param);
						continue;
					}

					rectPairs.add(Pair.of(Tab.EQUIPMENT, rect));
				}
				break;
				case "prayer":
				{
					final WidgetInfo info = utils.getPrayerWidgetInfo(param);
					final Prayer p = Prayer.valueOf(param.toUpperCase().replace(" ", "_"));

					if (client.isPrayerActive(p))
					{
						continue;
					}

					if (info == null)
					{
						log.debug("Prayer: Can't find valid widget info for param {}.", param);
						continue;
					}

					final Widget widget = client.getWidget(info);

					if (widget == null)
					{
						log.debug("Prayer: Can't find valid widget for param {}.", param);
						continue;
					}

					rectPairs.add(Pair.of(Tab.PRAYER, widget.getBounds()));
				}
				break;
				case "cast":
				{
					final WidgetInfo info = utils.getSpellWidgetInfo(param);

					if (info == null)
					{
						log.debug("Cast: Can't find valid widget info for param {}.", param);
						continue;
					}

					final Widget widget = client.getWidget(info);

					if (widget == null)
					{
						log.debug("Cast: Can't find valid widget for param {}.", param);
						continue;
					}

					rectPairs.add(Pair.of(Tab.SPELLBOOK, widget.getBounds()));
				}
				break;
				case "enable":
				{
					final Widget widget = client.getWidget(593, 35);

					if (widget == null)
					{
						log.debug("Spec: Can't find valid widget");
						continue;
					}

					rectPairs.add(Pair.of(Tab.COMBAT, widget.getBounds()));
				}
				break;
			}
		}

		executor.submit(() ->
		{
			for (Pair<Tab, Rectangle> pair : rectPairs)
			{
				int key = utils.getTabHotkey(pair.getLeft());

				if (key == -1 || key == 0)
				{
					log.error("Unable to find key for tab.");
					dispatchError("Unable to find " + pair.getLeft().toString() + " hotkey.");
					break;
				}

				executePair(pair);
				log.debug("Executing click on: {}", pair);

				try
				{
					Thread.sleep(getMillis());
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			log.debug("Swapping back to inventory.");
			robot.keyPress(utils.getTabHotkey(Tab.INVENTORY));
		});
	}

	private void executePair(Pair<Tab, Rectangle> pair)
	{
		switch (pair.getLeft())
		{
			case COMBAT:
				if (client.getVar(VarClientInt.INTERFACE_TAB) != InterfaceTab.COMBAT.getId())
				{
					robot.delay((int) getMillis());
					robot.keyPress(utils.getTabHotkey(pair.getLeft()));
					robot.delay((int) getMillis());
				}
				utils.click(pair.getRight());
				break;
			case EQUIPMENT:
				if (client.getVar(VarClientInt.INTERFACE_TAB) != InterfaceTab.EQUIPMENT.getId())
				{
					robot.delay((int) getMillis());
					robot.keyPress(utils.getTabHotkey(pair.getLeft()));
					robot.delay((int) getMillis());
				}
				utils.click(pair.getRight());
				break;
			case INVENTORY:
				if (client.getVar(VarClientInt.INTERFACE_TAB) != InterfaceTab.INVENTORY.getId())
				{
					robot.delay((int) getMillis());
					robot.keyPress(utils.getTabHotkey(pair.getLeft()));
					robot.delay((int) getMillis());
				}
				utils.click(pair.getRight());
				break;
			case PRAYER:
				if (client.getVar(VarClientInt.INTERFACE_TAB) != InterfaceTab.PRAYER.getId())
				{
					robot.delay((int) getMillis());
					robot.keyPress(utils.getTabHotkey(pair.getLeft()));
					robot.delay((int) getMillis());
				}
				utils.click(pair.getRight());
				break;
			case SPELLBOOK:
				if (client.getVar(VarClientInt.INTERFACE_TAB) != InterfaceTab.SPELLBOOK.getId())
				{
					robot.delay((int) getMillis());
					robot.keyPress(utils.getTabHotkey(pair.getLeft()));
					robot.delay((int) getMillis());
				}
				utils.click(pair.getRight());
				break;
		}
	}

	private Rectangle invBounds(int id)
	{
		final Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);

		for (WidgetItem item : inventoryWidget.getWidgetItems())
		{
			if (item.getId() == id)
			{
				return item.getCanvasBounds();
			}
		}

		return null;
	}

	private List<Rectangle> listOfBounds(int id)
	{
		final Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		final List<Rectangle> bounds = new ArrayList<>();

		for (WidgetItem item : inventoryWidget.getWidgetItems())
		{
			if (item.getId() == id)
			{
				bounds.add(item.getCanvasBounds());
			}
		}

		return bounds;
	}

	private Rectangle equipBounds(int id)
	{
		final Widget equipmentWidget = client.getWidget(WidgetInfo.EQUIPMENT);

		if (equipmentWidget.getStaticChildren() == null)
		{
			return null;
		}

		for (Widget widgets : equipmentWidget.getStaticChildren())
		{
			for (Widget items : widgets.getDynamicChildren())
			{
				if (items.getItemId() == id)
				{
					return items.getBounds();
				}
			}
		}

		return null;
	}

	private void dispatchError(String error)
	{
		String str = ColorUtil.wrapWithColorTag("Zulrah Swapper", Color.MAGENTA)
				+ " has encountered an "
				+ ColorUtil.wrapWithColorTag("error", Color.RED)
				+ ": "
				+ error;

		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", str, null);
	}

	public int getMillis()
	{
		return (int) (Math.random() * 70 + 80);
	}
}