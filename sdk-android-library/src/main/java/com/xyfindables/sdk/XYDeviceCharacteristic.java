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
    public static final UUID ExtendedConfigSIMId = UUID.fromString("2ACCAA00-0439-3000-E001-00001001FFFF");

    public static final UUID BasicConfigLockStatus = UUID.fromString("F014EE01-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigLock = UUID.fromString("F014EE02-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigUnlock = UUID.fromString("F014EE03-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigUUID = UUID.fromString("F014EE04-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigMajor = UUID.fromString("F014EE05-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigMinor = UUID.fromString("F014EE06-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigInterval = UUID.fromString("F014EE07-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigOtaWrite = UUID.fromString("F014EE09-0439-3000-E001-00001001FFFF");
    public static final UUID BasicConfigReboot = UUID.fromString("F014EE0A-0439-3000-E001-00001001FFFF");

    public static final UUID ExtendedControlSIMStatus = UUID.fromString("2ADDAA00-0439-3000-E001-00001001FFFF");
    public static final UUID ExtendedControlLED = UUID.fromString("2AAAAA00-0439-3000-E001-00001001FFFF");
    public static final UUID ExtendedControlSelfTest = UUID.fromString("2a77AA00-0439-3000-E001-00001001FFFF");

    public static final UUID BatteryLevel = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID BatterySinceCharged = UUID.fromString("00002a20-0000-1000-8000-00805f9b34fb");

    public static final UUID SensorRaw = UUID.fromString("F014DD01-0439-3000-E001-00001001FFFF");
    public static final UUID SensorTimeout = UUID.fromString("F014DD02-0439-3000-E001-00001001FFFF");
    public static final UUID SensorThreshold = UUID.fromString("F014DD03-0439-3000-E001-00001001FFFF");
    public static final UUID SensorInactive = UUID.fromString("F014DD04-0439-3000-E001-00001001FFFF");
    public static final UUID SensorMovementCount = UUID.fromString("F014DD05-0439-3000-E001-00001001FFFF");

    public static final UUID GpsProfile = UUID.fromString("2abbAA00-0439-3000-E001-00001001FFFF");

    public static final UUID XY4PrimaryStayAwake = UUID.fromString("a44eacf4-0104-0001-0001-5f784c9977b5");
    public static final UUID XY4PrimaryUnlock = UUID.fromString("a44eacf4-0104-0001-0002-5f784c9977b5");
    public static final UUID XY4PrimaryLock = UUID.fromString("a44eacf4-0104-0001-0003-5f784c9977b5");
    public static final UUID XY4PrimaryMajor = UUID.fromString("a44eacf4-0104-0001-0004-5f784c9977b5");
    public static final UUID XY4PrimaryMinor = UUID.fromString("a44eacf4-0104-0001-0005-5f784c9977b5");
    public static final UUID XY4PrimaryUUID = UUID.fromString("a44eacf4-0104-0001-0006-5f784c9977b5");
    public static final UUID XY4PrimaryButtonState = UUID.fromString("a44eacf4-0104-0001-0007-5f784c9977b5");
    public static final UUID XY4PrimaryBuzzer = UUID.fromString("a44eacf4-0104-0001-0008-5f784c9977b5");
    public static final UUID XY4PrimaryBuzzerConfig = UUID.fromString("a44eacf4-0104-0001-0009-5f784c9977b5");
    public static final UUID XY4PrimaryAdConfig = UUID.fromString("a44eacf4-0104-0001-000a-5f784c9977b5");
    public static final UUID XY4PrimaryButtonConfig = UUID.fromString("a44eacf4-0104-0001-000b-5f784c9977b5");
    public static final UUID XY4PrimaryLastError = UUID.fromString("a44eacf4-0104-0001-000c-5f784c9977b5");
    public static final UUID XY4PrimaryUptime = UUID.fromString("a44eacf4-0104-0001-000d-5f784c9977b5");
    public static final UUID XY4PrimaryReset = UUID.fromString("a44eacf4-0104-0001-000e-5f784c9977b5");
    public static final UUID XY4PrimarySelfTest = UUID.fromString("a44eacf4-0104-0001-000f-5f784c9977b5");
    public static final UUID XY4PrimaryDebug = UUID.fromString("a44eacf4-0104-0001-0010-5f784c9977b5");
    public static final UUID XY4PrimaryLeftBehind = UUID.fromString("a44eacf4-0104-0001-0011-5f784c9977b5");
    public static final UUID XY4PrimaryEddystoneUID = UUID.fromString("a44eacf4-0104-0001-0012-5f784c9977b5");
    public static final UUID XY4PrimaryEddystoneURL = UUID.fromString("a44eacf4-0104-0001-0013-5f784c9977b5");
    public static final UUID XY4PrimaryEddystoneEID = UUID.fromString("a44eacf4-0104-0001-0014-5f784c9977b5");

    public static final UUID XY4GenericDeviceName = UUID.fromString("a44eacf4-0104-1800-2a00-5f784c9977b5");
    public static final UUID XY4GenericAppearance = UUID.fromString("a44eacf4-0104-1800-2a01-5f784c9977b5");
    public static final UUID XY4GenericPrivacyFlag = UUID.fromString("a44eacf4-0104-1800-2a02-5f784c9977b5");
    public static final UUID XY4GenericConnParams = UUID.fromString("a44eacf4-0104-1800-2a04-5f784c9977b5");

    public static final UUID XY4AttributeServiceChanged = UUID.fromString("a44eacf4-0104-1801-2a05-5f784c9977b5");

    public static final UUID XY4DeviceManufacturer = UUID.fromString("a44eacf4-0104-180a-2a29-5f784c9977b5");
    public static final UUID XY4DeviceModel = UUID.fromString("a44eacf4-0104-180a-2a24-5f784c9977b5");
    public static final UUID XY4DeviceSoftware = UUID.fromString("a44eacf4-0104-180a-2a28-5f784c9977b5");
    public static final UUID XY4DeviceHardware = UUID.fromString("a44eacf4-0104-180a-2a27-5f784c9977b5");
    public static final UUID XY4DeviceFirmware = UUID.fromString("a44eacf4-0104-180a-2a26-5f784c9977b5");
    public static final UUID XY4DeviceSystemID = UUID.fromString("a44eacf4-0104-180a-2a23-5f784c9977b5");

    public static final UUID XY4PowerLevel = UUID.fromString("a44eacf4-0104-1804-2a07-5f784c9977b5");

    public static final UUID XY4AlertLevel = UUID.fromString("a44eacf4-0104-1802-2a06-5f784c9977b5");

    public static final UUID XY4BatteryLevel = UUID.fromString("a44eacf4-0104-180f-2a19-5f784c9977b5");

    public static final UUID XY4TimeCurrent = UUID.fromString("a44eacf4-0104-1805-2a2b-5f784c9977b5");
    public static final UUID XY4TimeLocal = UUID.fromString("a44eacf4-0104-1805-2a0f-5f784c9977b5");
}
