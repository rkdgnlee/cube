package com.tangoplus.tangoq.Object

import java.nio.ByteBuffer
import java.nio.ByteOrder

object CommonDefines {
    val TAG = CommonDefines::class.java.getSimpleName()
    const val CMD_ACK = 0x06.toByte()
    const val CMD_SET_TIME = 0x81.toByte()
    const val CMD_GET_COUNT = 0x82.toByte()
    const val CMD_SYNC_START = 0x083.toByte()
    const val CMD_SYNC_ACK = 0x84.toByte()
    const val CMD_REALTIME_OR_WRITE = 0x85.toByte()
    fun convertLittleEndianInt(data: ByteArray?, index: Int, length: Int): Long {
        val buf = ByteArray(length)
        System.arraycopy(data, index, buf, 0, length)
        val buffer = ByteBuffer.wrap(buf, 0, length)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return if (length == 2) {
            buffer.getShort().toLong() and 0xFFFFFFFFL
        } else buffer.getInt().toLong() and 0xFFFFFFFFL
    }

    fun convertBigEndianInt(data: ByteArray?, index: Int, length: Int): Long {
        val buf = ByteArray(length)
        System.arraycopy(data, index, buf, 0, length)
        val buffer = ByteBuffer.wrap(buf, 0, length)
        buffer.order(ByteOrder.BIG_ENDIAN)
        return if (length == 2) buffer.getShort().toLong() and 0xFFFFFFFFL else buffer.getInt()
            .toLong() and 0xFFFFFFFFL
    }
}

