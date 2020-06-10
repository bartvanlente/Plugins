package net.runelite.client.plugins.nightmareswapper;

import com.google.inject.Provides;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Actor;
import net.runelite.api.GameState;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Skill;
import net.runelite.api.VarClientInt;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.NpcDefinitionChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.vars.InterfaceTab;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.externals.utils.ExtUtils;
import net.runelite.client.plugins.externals.utils.Tab;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Nightmare of Ashihama Prayer Swapper",
	enabledByDefault = false,
	description = "Swaps the prayers and which tiles to avoid",
	tags = {"bosses", "combat", "nm", "overlay", "nightmare", "pve", "pvm", "ashihama", "bot", "swap"},
	type = PluginType.PVM
)

@Slf4j
@Singleton
public class NightmareSwapperPlugin extends Plugin
{
	// Nightmare's attack animations
	private static final int NIGHTMARE_HUSK_SPAWN = 8565;
	private static final int NIGHTMARE_CHARGE_1 = 8597;
	private static final int NIGHTMARE_SHADOW_SPAWN = 8598;
	private static final int NIGHTMARE_CURSE = 8599;
	private static final int NIGHTMARE_QUADRANTS = 8601;
	private static final int NIGHTMARE_SLEEP_DAMAGE = 8604;
	private static final int NIGHTMARE_PARASITE_TOSS = 8605;
	private static final int NIGHTMARE_PARASITE_TOSS2 = 8606;
	private static final int NIGHTMARE_CHARGE_TELEPORT = 8607;
	private static final int NIGHTMARE_CHARGE_2 = 8609;
	private static final int NIGHTMARE_SPAWN = 8611;
	private static final int NIGHTMARE_DEATH = 8612;
	private static final int NIGHTMARE_MELEE_ATTACK = 8594;
	private static final int NIGHTMARE_RANGE_ATTACK = 8596;
	private static final int NIGHTMARE_MAGIC_ATTACK = 8595;
	private static final int NIGHTMARE_PRE_MUSHROOM = 37738;
	private static final int NIGHTMARE_MUSHROOM = 37739;

	private static final List<Integer> INACTIVE_TOTEMS = Arrays.asList(9434, 9437, 9440, 9443);

	@Inject
	private Client client;

	@Inject
	private ExtUtils utils;

	@Inject
	private NightmareSwapperConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NightmarePrayerOverlay prayerOverlay;

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private NightmareAttack pendingNightmareAttack;

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private NPC nm;

	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, MemorizedTotem> totems = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private int ticksUntilParasite = 0;

	@Getter(AccessLevel.PACKAGE)
	private final Map<LocalPoint, GameObject> spores = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private boolean inFight;

	private boolean cursed;
	private int attacksSinceCurse;

	@Getter(AccessLevel.PACKAGE)
	private int ticksUntilNextAttack = 0;

	private Robot robot;
	private ExecutorService executor;

	public NightmareSwapperPlugin()
	{
		inFight = false;
	}

	@Provides
	NightmareSwapperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NightmareSwapperConfig.class);
	}

	@Inject
	private NightmareOverlay overlay;

	@Override
	protected void startUp() throws AWTException
	{
		overlayManager.add(overlay);
		overlayManager.add(prayerOverlay);
		robot = new Robot();
		executor = Executors.newSingleThreadExecutor();
		reset();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(prayerOverlay);
		reset();
	}

	private void reset()
	{
		inFight = false;
		nm = null;
		pendingNightmareAttack = null;
		cursed = false;
		attacksSinceCurse = 0;
		ticksUntilNextAttack = 0;
		ticksUntilParasite = 0;
		totems.clear();
		spores.clear();
		robot = null;
	}

	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!inFight)
		{
			return;
		}

		GameObject gameObj = event.getGameObject();
		int id = gameObj.getId();
		if (id == NIGHTMARE_MUSHROOM || id == NIGHTMARE_PRE_MUSHROOM)
		{
			spores.put(gameObj.getLocalLocation(), gameObj);
		}
	}

	@Subscribe
	private void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (!inFight)
		{
			return;
		}

		GameObject gameObj = event.getGameObject();
		int id = gameObj.getId();
		if (id == NIGHTMARE_MUSHROOM || id == NIGHTMARE_PRE_MUSHROOM)
		{
			spores.remove(gameObj.getLocalLocation());
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!inFight || nm == null)
		{
			return;
		}

		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) actor;
		int id = npc.getId();
		int animationId = npc.getAnimation();

		if (animationId == NIGHTMARE_MAGIC_ATTACK)
		{
			ticksUntilNextAttack = 7;
			attacksSinceCurse++;
			pendingNightmareAttack = cursed ? NightmareAttack.CURSE_MAGIC : NightmareAttack.MAGIC;
		}
		else if (animationId == NIGHTMARE_MELEE_ATTACK)
		{
			ticksUntilNextAttack = 7;
			attacksSinceCurse++;
			pendingNightmareAttack = cursed ? NightmareAttack.CURSE_MELEE : NightmareAttack.MELEE;
		}
		else if (animationId == NIGHTMARE_RANGE_ATTACK)
		{
			ticksUntilNextAttack = 7;
			attacksSinceCurse++;
			pendingNightmareAttack = cursed ? NightmareAttack.CURSE_RANGE : NightmareAttack.RANGE;
		}
		else if (animationId == NIGHTMARE_CURSE)
		{
			cursed = true;
			attacksSinceCurse = 0;
		}

		if (cursed && attacksSinceCurse == 5)
		{
			//curse is removed when she phases, or does 5 attacks
			cursed = false;
			attacksSinceCurse = -1;
		}
	}

	@Subscribe
	private void onChatMessage(ChatMessage chatMessage)
	{
		if (!inFight || chatMessage.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		final String message = chatMessage.getMessage();
		if (message.contains("The Nightmare has impregnated you with a deadly parasite!"))
		{
			ticksUntilParasite = 22;
		}
	}

	@Subscribe
	public void onNpcDefinitionChanged(NpcDefinitionChanged event)
	{
		final NPC npc = event.getNpc();

		if (npc == null)
		{
			return;
		}

		//this will trigger once when the fight begins
		if (npc.getId() == 9432)
		{
			//reset everything
			reset();
			nm = npc;
			inFight = true;
		}

		//if ID changes to 9431 (3rd phase) and is cursed, remove the curse
		if (cursed && npc.getId() == 9431)
		{
			cursed = false;
			attacksSinceCurse = -1;
		}

		//if npc is in the totems map, update its phase
		if (totems.containsKey(npc.getIndex()))
		{
			totems.get(npc.getIndex()).updateCurrentPhase(npc.getId());
		}
		else if (INACTIVE_TOTEMS.contains(npc.getId()))
		{
			//else if the totem is not in the totem array and it is an inactive totem, add it to the totem map.
			totems.putIfAbsent(npc.getIndex(), new MemorizedTotem(npc));
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		GameState gamestate = event.getGameState();

		//if loading happens while inFight, the user has left the area (either via death or teleporting).
		if (gamestate == GameState.LOADING && inFight)
		{
			reset();
		}
	}

	@Subscribe
	private void onGameTick(final GameTick event)
	{
		if (!inFight || nm == null)
		{
			return;
		}

		//if nightmare's id is 9433, the fight has ended and everything should be reset
		if (nm.getId() == 9433)
		{
			reset();
		}

		if (pendingNightmareAttack != null)
		{
			clickPrayer(pendingNightmareAttack.getPrayer());
		}

		ticksUntilNextAttack--;

		if (ticksUntilParasite > 0)
		{
			ticksUntilParasite--;
		}

		if (pendingNightmareAttack != null && ticksUntilNextAttack <= 3)
		{
			pendingNightmareAttack = null;
		}
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

	private boolean isNightmareNpc(int id)
	{
		return id >= 9425 && id <= 9433;
	}

	private long getMillis()
	{
		return (long) (Math.random() * 70 + 80);
	}
}
