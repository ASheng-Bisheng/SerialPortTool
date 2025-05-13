package com.xbs.serialport.source

/**
 * 创建日期：2023/3/3 10:00
 * @author ASheng
 * @version 1.0.1
 * 包名： com.xbs.serialport.source
 * 类说明：
 */
class AgreementAnalyse(agreeList: List<CommandDetailBean>) {
    private val sbData: StringBuffer = StringBuffer()
    private var sumLength: Int = 0

    init {
        agreeList.forEach {
            sumLength += it.length
        }
    }

    /**
     * 拼接协议数据  只能判断长度
     * @param message String
     */
    suspend fun jointCommand(message: String) {

        sbData.append(message)


        //获取当前sb中的数据长度
        val existLength = sbData.toString().length / 2
        //当刚好等于长度
        if (existLength == sumLength) {
            //发送一包

            sbData.setLength(0)
        } else if (existLength > sumLength) { //如果拼接数量超过了
            //截取出一包 并发送出去
            val data = sbData.substring(0, sumLength)

            val nextData = sbData.substring(sumLength, sbData.length)
            sbData.setLength(0)
            //其他重新加入
            sbData.append(nextData)
        }


    }

}