/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2017, Devin French <https://github.com/devinfrench>
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
package net.runelite.client.plugins.externals.zulrahswapper.patterns;

import net.runelite.api.Prayer;
import net.runelite.client.plugins.externals.zulrahswapper.phase.StandLocation;
import net.runelite.client.plugins.externals.zulrahswapper.phase.ZulrahLocation;
import net.runelite.client.plugins.externals.zulrahswapper.phase.ZulrahType;

public class ZulrahPatternD extends ZulrahPattern
{
	public ZulrahPatternD()
	{
		add(ZulrahLocation.NORTH, ZulrahType.RANGE, StandLocation.TOP_EAST, null); //1
		add(ZulrahLocation.EAST, ZulrahType.MAGIC, StandLocation.TOP_EAST, Prayer.PROTECT_FROM_MAGIC); //2
		add(ZulrahLocation.SOUTH, ZulrahType.RANGE, StandLocation.PILLAR_WEST_INSIDE, Prayer.PROTECT_FROM_MISSILES); //3
		add(ZulrahLocation.WEST, ZulrahType.MAGIC, StandLocation.PILLAR_WEST_INSIDE, Prayer.PROTECT_FROM_MAGIC); //4
		add(ZulrahLocation.NORTH, ZulrahType.MELEE, StandLocation.PILLAR_EAST_OUTSIDE, null); //5
		add(ZulrahLocation.EAST, ZulrahType.RANGE, StandLocation.PILLAR_EAST_OUTSIDE, Prayer.PROTECT_FROM_MISSILES); //6
		add(ZulrahLocation.SOUTH, ZulrahType.RANGE, StandLocation.PILLAR_WEST_OUTSIDE, null); //7
		add(ZulrahLocation.WEST, ZulrahType.MAGIC, StandLocation.PILLAR_WEST_OUTSIDE, Prayer.PROTECT_FROM_MAGIC); //8
		add(ZulrahLocation.NORTH, ZulrahType.RANGE, StandLocation.TOP_EAST, Prayer.PROTECT_FROM_MISSILES); //9
		add(ZulrahLocation.NORTH, ZulrahType.MAGIC, StandLocation.TOP_EAST, Prayer.PROTECT_FROM_MAGIC); //10
		addJad(ZulrahLocation.EAST, ZulrahType.MAGIC, StandLocation.TOP_EAST, Prayer.PROTECT_FROM_MAGIC); //11
		add(ZulrahLocation.NORTH, ZulrahType.MAGIC, StandLocation.TOP_EAST, null); //12
	}

	@Override
	public String toString()
	{
		return "Pattern D";
	}
}
