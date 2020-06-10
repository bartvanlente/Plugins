package net.runelite.client.plugins.externals.zulrahswapper.patterns;

import net.runelite.api.Prayer;
import net.runelite.client.plugins.externals.zulrahswapper.phase.StandLocation;
import net.runelite.client.plugins.externals.zulrahswapper.phase.ZulrahLocation;
import net.runelite.client.plugins.externals.zulrahswapper.phase.ZulrahType;

public class ZulrahPatternB extends ZulrahPattern
{
	public ZulrahPatternB()
	{
		add(ZulrahLocation.NORTH, ZulrahType.RANGE, StandLocation.TOP_EAST, null); //1
		add(ZulrahLocation.NORTH, ZulrahType.MELEE, StandLocation.TOP_EAST, null); //2
		add(ZulrahLocation.NORTH, ZulrahType.MAGIC, StandLocation.PILLAR_WEST_OUTSIDE, Prayer.PROTECT_FROM_MAGIC); //3
		add(ZulrahLocation.WEST, ZulrahType.RANGE, StandLocation.PILLAR_WEST_OUTSIDE, null); //4
		add(ZulrahLocation.SOUTH, ZulrahType.MAGIC, StandLocation.PILLAR_WEST_INSIDE, Prayer.PROTECT_FROM_MAGIC); //5 optional phase
		add(ZulrahLocation.NORTH, ZulrahType.MELEE, StandLocation.PILLAR_WEST_INSIDE, null); //6
		add(ZulrahLocation.EAST, ZulrahType.RANGE, StandLocation.SOUTH_EAST, Prayer.PROTECT_FROM_MISSILES); //7
		add(ZulrahLocation.SOUTH, ZulrahType.MAGIC, StandLocation.SOUTH_WEST, Prayer.PROTECT_FROM_MAGIC); //8
		addJad(ZulrahLocation.WEST, ZulrahType.RANGE, StandLocation.TOP_WEST, Prayer.PROTECT_FROM_MISSILES); //9
		add(ZulrahLocation.NORTH, ZulrahType.MELEE, StandLocation.TOP_WEST, null); //10
	}

	@Override
	public String toString()
	{
		return "Pattern B";
	}
}
