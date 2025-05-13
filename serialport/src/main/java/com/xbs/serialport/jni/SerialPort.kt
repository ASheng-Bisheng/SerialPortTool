package com.xbs.serialport.jni

import android.util.Log
import com.xbs.serialport.source.BaudRate
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 创建日期：2023/3/2 14:29
 * @author ASheng
 * @version 1.0.1
 * 包名： com.xbs.serialport.jni
 * 类说明：
 */
class SerialPort {

    private var mFd: FileDescriptor? = null
    var mFileInputStream: FileInputStream? = null
        private set
    var mFileOutputStream: FileOutputStream? = null
        private set

    @Throws(SecurityException::class, IOException::class)
    fun initAndOpen(
        device: File, @BaudRate baudRate: Int,
        dataBits: Int,
        parity: Int,
        stopBits: Int,
        flags: Int
    ): SerialPort {
        if (!device.canRead() || !device.canWrite()) {
            try {
                //通过挂载到linux的方式，修改文件的操作权限
                val su = Runtime.getRuntime().exec("/system/xbin/su")
                val cmd = """
                chmod 777 ${device.absolutePath}
                exit

                """.trimIndent()
                su.outputStream.use { it.write(cmd.toByteArray()) }

                if (su.waitFor() != 0 || !device.canRead() || !device.canWrite()) {
                    throw SecurityException()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw SecurityException()
            }
        }
         mFd = open(device.absolutePath, baudRate, dataBits, parity, stopBits, flags)
        if (mFd == null) {
            throw IOException()
        }
        mFileInputStream = FileInputStream(mFd)
        mFileOutputStream = FileOutputStream(mFd)
        return this
    }


    // JNI(调用java本地接口，实现串口的打开和关闭)
    /**串口有五个重要的参数：串口设备名，波特率，检验位，数据位，停止位
     * 其中检验位一般默认位NONE,数据位一般默认为8，停止位默认为1 */
    /**
     * Open
     *
     * @param path 串口设备的据对路径
     * @param baudRate 波特率
     * @param dataBits 数据位；默认8,可选值为5~8
     * @param parity 奇偶校验；0:无校验位(NONE，默认)；1:奇校验位(ODD);2:偶校验位(EVEN)
     * @param stopBits 停止位；默认1；1:1位停止位；2:2位停止位
     * @param flags 校验位；默认0
     * @return
     */
    private external fun open(
        path: String,
        @BaudRate baudRate: Int,
        dataBits: Int,
        parity: Int,
        stopBits: Int,
        flags: Int
    ): FileDescriptor?

    external fun close()

    companion object {

        init {
            System.loadLibrary("SerialPort")
        }
    }
}