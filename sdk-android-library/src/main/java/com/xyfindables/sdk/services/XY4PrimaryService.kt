package com.xyfindables.sdk.services

import com.xyfindables.sdk.devices.XYBluetoothDevice
import kotlinx.coroutines.experimental.Deferred
import java.util.*

class XY4PrimaryService(device: XYBluetoothDevice) : XYService(device) {

    override val uuid : UUID
        get() {
            return services.primary.uuid
        }

    fun readStayAwake() : Deferred<Int?> {
        return readInt(characteristics.StayAwake.uuid)
    }

    fun writeStayAwake(flag: Int) : Deferred<Boolean?> {
        return writeInt(characteristics.StayAwake.uuid, flag)
    }

    fun writeUnlock(bytes: ByteArray) : Deferred<Boolean?> {
        return writeBytes(characteristics.Unlock.uuid, bytes)
    }

    fun readLock() : Deferred<ByteArray?> {
        return readBytes(characteristics.Lock.uuid)
    }

    fun writeLock(bytes: ByteArray) : Deferred<Boolean?> {
        return writeBytes(characteristics.Lock.uuid, bytes)
    }

    fun readMajor() : Deferred<Int?> {
        return readInt(characteristics.Major.uuid)
    }

    fun writeMajor(flag: Int) : Deferred<Boolean?> {
        return writeInt(characteristics.Major.uuid, flag)
    }

    fun readMinor() : Deferred<Int?> {
        return readInt(characteristics. Minor.uuid)
    }

    fun writeMinor(flag: Int) : Deferred<Boolean?> {
        return writeInt(characteristics.Minor.uuid, flag)
    }

    fun readUuid() : Deferred<ByteArray?> {
        return readBytes(characteristics.Uuid.uuid)
    }

    fun writeUuid(bytes: ByteArray) : Deferred<Boolean?> {
        return writeBytes(characteristics.Uuid.uuid, bytes)
    }

    fun writeBuzzer(tone: Int) : Deferred<Boolean?> {
        return writeInt(characteristics.Buzzer.uuid, tone)
    }

    fun writeBuzzerConfig(bytes: ByteArray) : Deferred<Boolean?> {
        return writeBytes(characteristics.BuzzerConfig.uuid, bytes)
    }

    companion object {

        enum class services(val uuid: UUID) {
            primary(UUID.fromString(            "a44eacf4-0104-0001-0000-5f784c9977b5"))
        }

        enum class characteristics(val uuid: UUID) {
            StayAwake(UUID.fromString(          "a44eacf4-0104-0001-0001-5f784c9977b5")),
            Unlock(UUID.fromString(             "a44eacf4-0104-0001-0002-5f784c9977b5")),
            Lock(UUID.fromString(               "a44eacf4-0104-0001-0003-5f784c9977b5")),
            Major(UUID.fromString(              "a44eacf4-0104-0001-0004-5f784c9977b5")),
            Minor(UUID.fromString(              "a44eacf4-0104-0001-0005-5f784c9977b5")),
            Uuid(UUID.fromString(               "a44eacf4-0104-0001-0006-5f784c9977b5")),
            ButtonState(UUID.fromString(        "a44eacf4-0104-0001-0007-5f784c9977b5")),
            Buzzer(UUID.fromString(             "a44eacf4-0104-0001-0008-5f784c9977b5")),
            BuzzerConfig(UUID.fromString(       "a44eacf4-0104-0001-0009-5f784c9977b5")),
            AdConfig(UUID.fromString(           "a44eacf4-0104-0001-000a-5f784c9977b5")),
            ButtonConfig(UUID.fromString(       "a44eacf4-0104-0001-000b-5f784c9977b5")),
            LastError(UUID.fromString(          "a44eacf4-0104-0001-000c-5f784c9977b5")),
            Uptime(UUID.fromString(             "a44eacf4-0104-0001-000d-5f784c9977b5")),
            Reset(UUID.fromString(              "a44eacf4-0104-0001-000e-5f784c9977b5")),
            SelfTest(UUID.fromString(           "a44eacf4-0104-0001-000f-5f784c9977b5")),
            Debug(UUID.fromString(              "a44eacf4-0104-0001-0010-5f784c9977b5")),
            LeftBehind(UUID.fromString(         "a44eacf4-0104-0001-0011-5f784c9977b5")),
            EddystoneUID(UUID.fromString(       "a44eacf4-0104-0001-0012-5f784c9977b5")),
            EddystoneURL(UUID.fromString(       "a44eacf4-0104-0001-0013-5f784c9977b5")),
            EddystoneEID(UUID.fromString(       "a44eacf4-0104-0001-0014-5f784c9977b5")),
            Color(UUID.fromString(              "a44eacf4-0104-0001-0015-5f784c9977b5")),
            HardwareCreateDate(UUID.fromString( "a44eacf4-0104-0001-0017-5f784c9977b5"))
        }
    }
}