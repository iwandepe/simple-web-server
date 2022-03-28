package com.progjar.server

class Request(val request: String) {
    val requestHeader = mutableMapOf<String, String>()

    init {
        val requestList = request.split("\n")
        for (line in requestList) {
            if (line.contains(":")) {
                var key = line.substring(0, line.indexOf(":"))
                var value = line.substring(key.length + 2, line.length)

                requestHeader.put(key, value)
            }

            else if (line.contains("GET") || line.contains("POST")) {
                var value = line.split(" ")

                requestHeader.put("Method", value[0])
                requestHeader.put("Path", value[1])
                requestHeader.put("HTTP-Version", value[2])
            }
        }
    }

    fun getHost(): String? {
        return requestHeader.get("Host")
    }

    fun isConnectionExist(): Boolean {
        return requestHeader.contains("Connection")
    }

    fun getConnection(): String? {
        return requestHeader.get("Connection")
    }

    fun getAccept(): String? {
        return requestHeader.get("Accept")
    }

    fun getAcceptEncoding(): String? {
        return requestHeader.get("Accept-Encoding")
    }

    fun getAcceptLanguage(): String? {
        return requestHeader.get("Accept-Language")
    }

    fun getAcceptCharset(): String? {
        return requestHeader.get("Accept-Charset")
    }

    fun getReferer(): String? {
        return requestHeader.get("Referer")
    }

    fun getRange(): String? {
        return requestHeader.get("Range")
    }

    fun isRangeExist(): Boolean {
        return requestHeader.contains("Range")
    }

    fun getRangeStart(): Int? {
        val range = getRange()!!.split("=")[1]

        return range.split("-")[0].toInt()
    }

    fun getRangeEnd(): Int? {
        val range = getRange()!!.split("=")[1]

        return range.split("-")[1].toInt()
    }

    fun getRequestMethod(): String? {
        return requestHeader.get("Method")
    }

    fun getRequestPath(): String? {
        return requestHeader.get("Path")
    }

    fun getHttpVersion(): String? {
        return requestHeader.get("HTTP-Version")
    }

    fun getUserAgent(): String? {
        return requestHeader.get("User-Agent")
    }

    fun getRequestStr(): String {
        return requestHeader.toString()
    }

    fun getRequestKeys(): String {
        return requestHeader.keys.toString()
    }
}