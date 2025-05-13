package com.xbs.serialport.tool

/**
 * 版权：创世易名 版权所有
 * @author ASheng
 * 创建日期：2023/5/9 9:35
 * 描述：
 */
object Cr8 {


    fun CRC8_MAXIM(source: ByteArray, offset: Int, length: Int): Int {
        var wCRCin = 0x00
        val wCPoly = 0x8C
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0xFF)
            for (j in 0 until 8) {
                wCRCin = if ((wCRCin and 0x01) != 0) {
                    (wCRCin shr 1) xor wCPoly
                } else {
                    wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0x00
    }
}