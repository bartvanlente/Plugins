/*
 * Copyright (c) 2019-2020, ganom <https://github.com/Ganom>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.externals.autoblisterwoodcutter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigTitleSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Title;

@ConfigGroup("AutoBlisterwoodCutterConfig")
public interface AutoBlistwoodCutterConfig extends Config
{
	@ConfigTitleSection(
			position = 0,
			keyName = "mainConfig",
			name = "Bot Config",
			description = ""
	)
	default Title mainConfig()
	{
		return new Title();
	}

	@ConfigItem(
			keyName = "toggle",
			name = "Toggle",
			description = "",
			position = 1,
			titleSection = "mainConfig"
	)
	default Keybind toggle()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			position = 2,
			keyName = "randLow",
			name = "Minimum Delay",
			description = "",
			titleSection = "mainConfig"
	)
	default int randLow()
	{
		return 70;
	}

	@ConfigItem(
			position = 3,
			keyName = "randLower",
			name = "Maximum Delay",
			description = "",
			titleSection = "mainConfig"
	)
	default int randHigh()
	{
		return 80;
	}
}
