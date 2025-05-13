package com.xbs.serialport.source

import androidx.annotation.IntDef

/**
 * 创建日期：2023/3/2 17:49
 * @author ASheng
 * @version 1.0.1
 * 包名： com.xbs.serialport.tool
 * 类说明：
 */

@IntDef(
    SerialPortFailType.OPEN_FAIL, SerialPortFailType.SEND_FAIL, SerialPortFailType.READ_FAIL, SerialPortFailType.CLOSE
)
@Retention(AnnotationRetention.SOURCE)
annotation class SerialPortFailType{
    companion object{
        const val OPEN_FAIL = 0
        const val SEND_FAIL = 1
        const val READ_FAIL = 2
        const val CLOSE = 3
    }
}

