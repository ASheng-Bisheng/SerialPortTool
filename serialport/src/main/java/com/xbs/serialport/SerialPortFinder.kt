/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.xbs.serialport

import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.LineNumberReader
import java.util.*

class SerialPortFinder {
    private inner class Driver(val name: String, val root: String) {
        var mDevices: Vector<File>? = null
            get() {
                if (field == null) {
                    field = Vector()
                    val dev = File("/dev")
                    val files = dev.listFiles()
                    files?.let {
                        for (i in files.indices) {
                            if (files[i].absolutePath.startsWith(root)) {
                                field!!.add(files[i])
                            }
                        }
                    }
                }
                return field
            }
    }

    private var mDrivers: Vector<Driver>? = null
        @Throws(IOException::class)
        get() {
            if (field == null) {
                field = Vector<Driver>()
                val r = LineNumberReader(FileReader("/proc/tty/drivers"))
                var l: String
                while (r.readLine().also { l = it } != null) {
                    // Issue 3:
                    // Since driver name may contain spaces, we do not extract driver name with split()
                    val driverName = l.substring(0, 0x15).trim { it <= ' ' }
                    val w = l.split(" +".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    if (w.size >= 5 && w[w.size - 1] == "serial") {
                        field!!.add(
                            Driver(
                                driverName,
                                w[w.size - 4]
                            )
                        )
                    }
                }
                r.close()
            }
            return field
        }


    fun getAllDevices(): Array<String> {
        val devices = Vector<String>()
        // Parse each driver
        val itdriv: Iterator<Driver>
        try {
            itdriv = mDrivers!!.iterator()
            while (itdriv.hasNext()) {
                val driver: Driver = itdriv.next()
                val itdev: Iterator<File> = driver.mDevices!!.iterator()
                while (itdev.hasNext()) {
                    val device = itdev.next().name
                    val value = String.format("%s (%s)", device, driver.name)
                    devices.add(value)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return devices.toTypedArray()
    }

    fun getAllDevicesPath(): Array<String> {
        val devices = Vector<String>()
        // Parse each driver
        val itdriv: Iterator<Driver>
        try {
            itdriv = mDrivers!!.iterator()
            while (itdriv.hasNext()) {
                val driver: Driver = itdriv.next()
                val itdev: Iterator<File> = driver.mDevices!!.iterator()
                while (itdev.hasNext()) {
                    val device = itdev.next().absolutePath
                    devices.add(device)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return devices.toTypedArray()
    }
}