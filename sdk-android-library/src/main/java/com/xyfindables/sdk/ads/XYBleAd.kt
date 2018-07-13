package com.xyfindables.sdk.ads

import com.xyfindables.core.XYBase
import java.nio.ByteBuffer
import java.security.MessageDigest

open class XYBleAd(buffer: ByteBuffer) : XYBase() {

    val size: Byte
    val type: Byte
    var data: ByteArray? = null

    init {
        size = buffer.get()
        type = buffer.get()
        if (size > 0) {
            data = ByteArray(size - 1)
            buffer.get(data, 0, size - 1)
        } else {
            //if size is zero, we hit the end
            while (buffer.hasRemaining()) {
                buffer.get()
            }
        }
    }

    override fun hashCode(): Int {
        val p : Int = 16777619
        var hash : Int = 216613626

        hash = (Math.pow(hash.toDouble(), size.toDouble()) * p).toInt()
        hash += hash.shl(13)
        hash = hash.xor(hash.shr(7))
        hash += hash.shl(3)
        hash = hash.xor(hash.shr(17))
        hash += hash.shl(5)

        hash = (Math.pow(hash.toDouble(), type.toDouble()) * p).toInt()
        hash += hash.shl(13)
        hash = hash.xor(hash.shr(7))
        hash += hash.shl(3)
        hash = hash.xor(hash.shr(17))
        hash += hash.shl(5)

        if (data != null) {

            for (byte in data!!) {
                hash = (Math.pow(hash.toDouble(), byte.toDouble()) * p).toInt()
                hash += hash.shl(13)
                hash = hash.xor(hash.shr(7))
                hash += hash.shl(3)
                hash = hash.xor(hash.shr(17))
                hash += hash.shl(5)
            }

        }
        return hash
    }

    override fun toString(): String {
        if (data != null) {
            return "Type: $type, Bytes: ${data!!.contentToString()}"
        } else {
            return ""
        }
    }
}