package com.xbs.serialport.listener

import com.xbs.serialport.source.SerialPortFailType

/**
 * 创建日期：2023/3/2 17:41
 * @author ASheng
 * @version 1.0.1
 * 包名： com.xbs.serialport.listener
 * 类说明：
 */
interface SPAbstractListener {
    /**
     * Open succeed
     * 开启串口成功
     */
    fun onOpenSucceed() {

    }

    fun onSendSucceed(message: String) {

    }

    /**
     * 消息回调
     * @param byteArray ByteArray 未转数据
     * @param message String 一转换数据
     */
    fun onMessage(byteMessage: ByteArray,hexMessage: String) {

    }

    fun onFail(@SerialPortFailType type: Int) {

    }
}