package com.xyfindables.sdk;

import java.util.UUID;

/**
 * Created by arietrouw on 12/31/16.
 */

public class XYDeviceCharacteristic {

    public static final UUID ControlBuzzer = UUID.fromString("F014FFF1-0439-3000-E001-00001001FFFF");
    public static final UUID ControlHandshake = UUID.fromString("F014FFF2-0439-3000-E001-00001001FFFF");
    public static final UUID ControlVersion = UUID.fromString("F014FFF4-0439-3000-E001-00001001FFFF");
    public static final UUID ControlBuzzerSelect = UUID.fromString("F014FFF6-0439-3000-E001-00001001FFFF");
    public static final UUID ControlSurge = UUID.fromString("F014FFF7-0439-3000-E001-00001001FFFF");
    public static final UUID ControlButton = UUID.fromString("F014FFF8-0439-3000-E001-00001001FFFF");
    public static final UUID ControlDisconnect = UUID.fromString("F014FFF9-0439-3000-E001-00001001FFFF");

    public static final UUID ExtendedConfigVirtualBeaconSettings = UUID.fromString("F014FF02-0439-3000-E001-00001001FFFF");
    public static final UUID ExtendedConfigTone = UUID.fromString("F014FF03-0439-3000-E001-00001001FFFF");
    public static final UUID ExtendedConfigRegistration = UUID.fromString("F014FF05-0439-3000-E001-00001001FFFF");
    public static final UUID ExtendedConfigInactiveVirtualBeaconSettings = UUID.fromString("F014FF06-0439-3000-E001-00001001FFFF");
    public static final UUID ExtendedConfigInactiveInterval = UUID.fromString("F014FF07-0439-3000-E001-00001001FFFF");
    public static final UUID ExtendedConfigGPSInterval = UUID.fromString("2ABBAA00-0439-3000-E001-00001001FFFF");
    public static final UUID ExtendedConfigGPSMode = UUID.fromString("2A99AA00-0439-3000-E001-00001001FFFF");

    public static final UUID BasicConfigLockStatus = UUID.fromString("F014EE01-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigLock = UUID.fromString("F014EE02-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigUnlock = UUID.fromString("F014EE03-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigUUID = UUID.fromString("F014EE04-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigMajor = UUID.fromString("F014EE05-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigMinor = UUID.fromString("F014EE06-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigInterval = UUID.fromString("F014EE07-0439-3000-E001-00001001FFFF");

    public static final UUID ExtendedControlSIMStatus = UUID.fromString("2ADDAA00-0439-3000-E001-00001001FFFF");
    public static final UUID ExtendedControlLED = UUID.fromString("2AAAAA00-0439-3000-E001-00001001FFFF");

    public static final UUID BatteryLevel = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
}
