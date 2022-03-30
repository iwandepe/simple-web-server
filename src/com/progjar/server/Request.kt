package com.progjar.server

class Request(val request: String) {
    val requestHeader = mutableMapOf<String, String>()

    init {
        val requestList = request.split("\n")
        for (line in requestList) {
            if (line.contains(":")) {
                var key = line.substring(0, line.indexOf(":"))
                var value = line.substring(key.length + 2, line.length)

                requestHeader.put(key.lowercase(), value)
            }

            else if (line.contains("GET") || line.contains("POST")) {
                var value = line.split(" ")

                requestHeader.put("method", value[0])
                requestHeader.put("path", value[1])
                requestHeader.put("http-version", value[2])
            }
        }
    }

    fun getHost(): String? {
        return requestHeader.get("host")
    }

    fun isHostExist(): Boolean {
        return requestHeader.contains("host")
    }

    fun isConnectionExist(): Boolean {
        return requestHeader.contains("connection")
    }

    fun getConnection(): String? {
        return requestHeader.get("connection")
    }

    fun getAccept(): String? {
        return requestHeader.get("accept")
    }

    fun getAcceptEncoding(): String? {
        return requestHeader.get("accept-encoding")
    }

    fun getAcceptLanguage(): String? {
        return requestHeader.get("accept-language")
    }

    fun getAcceptCharset(): String? {
        return requestHeader.get("accept-charset")
    }

    fun getReferer(): String? {
        return requestHeader.get("referer")
    }

    fun getRange(): String? {
        return requestHeader.get("range")
    }

    fun isRangeExist(): Boolean {
        return requestHeader.contains("range")
    }

    fun getRangeStart(): Long? {
        val range = getRange()!!.split("=")[1]

        return range.split("-")[0].toLong()
    }

    fun getRangeEnd(): Long? {
        val range = getRange()!!.split("=")[1]

        return range.split("-")[1].toLong()
    }

    fun getRequestMethod(): String? {
        return requestHeader.get("method")
    }

    fun getRequestPath(): String? {
        return requestHeader.get("path")
    }

    fun getHttpVersion(): String? {
        return requestHeader.get("http-version")
    }

    fun getUserAgent(): String? {
        return requestHeader.get("user-agent")
    }

    fun isUserAgentExist(): Boolean {
        return requestHeader.contains("user-agent")
    }

    fun getRequestStr(): String {
        return requestHeader.toString()
    }

    fun getRequestKeys(): String {
        return requestHeader.keys.toString()
    }
}