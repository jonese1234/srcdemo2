package net.srcdemo.ui;

import net.srcdemo.ui.VideoUI.VideoType;

import com.trolltech.qt.core.QSettings;

public class SrcSettings extends QSettings
{
	SrcSettings()
	{
		super("SrcDemo");
	}

	String getLastBackingDirectory()
	{
		return (String) value("backingDirectory", "");
	}

	int getLastBlendRate()
	{
		return (Integer) value("blendRate", 32);
	}

	String getLastMountpoint()
	{
		return (String) value("mountpoint", "");
	}

	int getLastShutterAngle()
	{
		return (Integer) value("shutterAngle", 180);
	}

	int getLastTargetFps()
	{
		return (Integer) value("targetFps", 30);
	}

	VideoType getLastVideoType()
	{
		return (VideoType) value("videoType", VideoType.PNG);
	}

	void setLastBackingDirectory(final String backingDirectory)
	{
		setValue("backingDirectory", backingDirectory);
	}

	void setLastBlendRate(final int blendRate)
	{
		setValue("blendRate", blendRate);
	}

	void setLastMountpoint(final String mountpoint)
	{
		setValue("mountpoint", mountpoint);
	}

	void setLastShutterAngle(final int shutterAngle)
	{
		setValue("shutterAngle", shutterAngle);
	}

	void setLastTargetFps(final int targetFps)
	{
		setValue("targetFps", targetFps);
	}

	void setLastVideoType(final VideoType videoType)
	{
		setValue("videoType", videoType);
	}
}
