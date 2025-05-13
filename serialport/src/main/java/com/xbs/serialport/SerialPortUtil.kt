package com.xbs.serialport

import android.util.Log
import com.xbs.serialport.jni.SerialPort
import com.xbs.serialport.listener.SPAbstractListener
import com.xbs.serialport.source.BaudRate

import com.xbs.serialport.source.SerialPortFailType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * 创建日期：2023/3/2 16:03
 * @author ASheng
 * @version 1.0.1
 * 包名： com.xbs.serialport.tool
 * 类说明：
 */
class SerialPortUtil private constructor() {

    companion object {
        @Volatile
        private var instance: SerialPortUtil? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: SerialPortUtil().also { instance = it }
        }

    }

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var serialPort: SerialPort? = null
    private var config: Config? = null
    private var receiveJob: Job? = null

    /**
     * Init
     * 初始化
     * @param config
     */
    fun init(config: Config) {
        this.config = config
    }

    /**
     * Open serial port
     * 连接设备
     */
    fun openSerialPort(isOpenReceive: Boolean = true) {
        if (config == null) {
            throw NullPointerException("请先初始化!")
        }
        try {
            serialPort = SerialPort().initAndOpen(
                config!!.device,
                config!!.baudRate,
                config!!.dataBits,
                config!!.parity,
                config!!.stopBits,
                config!!.flags
            )
            inputStream = serialPort!!.mFileInputStream
            outputStream = serialPort!!.mFileOutputStream
            listenerMap.forEach {
                it.value.onOpenSucceed()
            }
            if (isOpenReceive) {
                receiveSerialPort()
            }
            config!!.pollValue?.let {
                pollValue()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listenerMap.forEach {
                it.value.onFail(SerialPortFailType.OPEN_FAIL)
            }
        }
    }

    private var toMessagePoll: Any? = null
    fun sendInsertionToMessagePoll(toMessagePoll: Any) {
        this.toMessagePoll = toMessagePoll
    }

    private var pollValueJob: Job? = null
    private fun pollValue() {
        pollValueJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch(Dispatchers.IO) {
            while (serialPort != null) {
                toMessagePoll?.let {
                    delay(100)
                    sendMessage(it)
                }
                toMessagePoll = null
                delay(100)
                sendMessage(config!!.pollValue!!)
            }
        }
    }

    /**
     * Send message
     * 发送消息
     * @param data
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun sendMessage(data: Any) {
        if (outputStream == null) {
            Log.i("SerialPortUtil", "请先连接串口")
            return
        }
        try {
            val type = data.javaClass.simpleName
            val dataStr: String
            when (type) {
                "byte[]" -> {
                    val myData = data as ByteArray
                    outputStream?.write(myData)
                    dataStr = myData.toHexString().chunked(2).joinToString(" ").trim()
                }

                "String" -> {
                    val myData = data as String
                    outputStream?.write(myData.toByteArray())
                    dataStr = myData
                }

                else -> {
                    return
                }
            }
            outputStream?.flush()


            listenerMap.forEach {
                it.value.onSendSucceed(dataStr)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            closeSerialPort(false)
            listenerMap.forEach {
                it.value.onFail(SerialPortFailType.SEND_FAIL)
            }
        }
    }

    /**
     * Receive serial port
     * 接受消息
     */
    fun receiveSerialPort() {
        if (inputStream == null) {
            return
        }
        receiveJob?.cancel()
        receiveJob = job()
        receiveJob!!.start()
    }

    /**
     * Listener map
     * 监听
     */
    private val listenerMap: MutableMap<String, SPAbstractListener> = HashMap()

    /**
     * Job
     *  接收消息协程
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun job() = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch(
        Dispatchers.IO, CoroutineStart.LAZY
    ) {
        while (serialPort != null) {
            try {
                if (inputStream != null) {
                    //Log.i("SerialPortUtil", "waiting.......")
                    val readData = ByteArray(1024)
                    val size: Int = inputStream!!.read(readData)
                    if (size > 0) {
                        val realData = readData.copyOfRange(0, size)
                        val serialData = if (config!!.receiveHex) {
                            realData.toHexString().chunked(2).joinToString(" ").trim()
                        } else {
                            String(realData, 0, realData.size)
                        }
                        listenerMap.forEach {
                            it.value.onMessage(realData, serialData)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                closeSerialPort(false)
                listenerMap.forEach {
                    it.value.onFail(SerialPortFailType.READ_FAIL)
                }
            }
        }

    }

    /**
     * Register serial port listener
     * 注册串口回调
     * @param key
     * @param listener
     */
    fun registerSerialPortListener(key: String, listener: SPAbstractListener) {
        listenerMap[key] = listener
    }

    /**
     * Un register serial port listener
     * 释放串口回调
     * @param key
     */
    fun unRegisterSerialPortListener(key: String) {
        listenerMap.remove(key)
    }

    /**
     * Clear all listener
     *清楚所有回调
     */
    fun clearAllListener() {
        listenerMap.clear()
    }

    /**
     * 关闭串口的方法
     * 关闭串口中的输入输出流
     * 然后将flag的值设为flag，终止接收数据线程
     */
    fun closeSerialPort(isInitiative: Boolean = true) {
        try {
            pollValueJob?.cancel()
            inputStream?.close()
            inputStream = null
            outputStream?.close()
            outputStream = null
            serialPort?.close()
            serialPort = null
            receiveJob?.cancel()
            receiveJob = null

            if (isInitiative) {
                listenerMap.forEach {
                    it.value.onFail(SerialPortFailType.CLOSE)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()

        }
    }

    class Config private constructor(val device: File, @BaudRate val baudRate: Int) {

        var dataBits = 8
            private set
        var parity = 0
            private set
        var stopBits = 1
            private set
        var flags = 0
            private set
        var receiveHex = true
            private set
        var pollValue: Any? = null


        constructor(devicePath: String, baudrate: Int) : this(File(devicePath), baudrate)

        /**
         * 数据位
         *
         * @param dataBits 默认8,可选值为5~8
         * @return
         */
        fun dataBits(dataBits: Int): Config {
            this.dataBits = dataBits
            return this
        }

        /**
         * 校验位
         *
         * @param parity 0:无校验位(NONE，默认)；1:奇校验位(ODD);2:偶校验位(EVEN)
         * @return
         */
        fun parity(parity: Int): Config {
            this.parity = parity
            return this
        }

        /**
         * 接收是否转为Hex
         * @param receiveHex Boolean
         * @return Config
         */
        fun receiveHex(receiveHex: Boolean): Config {
            this.receiveHex = receiveHex
            return this
        }

        /**
         * 停止位
         *
         * @param stopBits 默认1；1:1位停止位；2:2位停止位
         * @return
         */
        fun stopBits(stopBits: Int): Config {
            this.stopBits = stopBits
            return this
        }

        /**
         * 标志
         *
         * @param flags 默认0
         * @return
         */
        fun flags(flags: Int): Config {
            this.flags = flags
            return this
        }

        /**
         * 轮询值
         *
         * @param flags 默认0
         * @return
         */
        fun pollValue(pollValue: Any): Config {
            this.pollValue = pollValue
            return this
        }


    }


}
