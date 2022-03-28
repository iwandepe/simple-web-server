package com.progjar.server

import java.io.*
import java.net.Socket
import java.util.*


class ClientWorker(name: String, var client: Socket) : Thread(name) {
    //val ROOT = "C:\\developing\\project\\project-java\\simple-web-server\\res"
    val ROOT = "C:\\Users\\LENOVO\\IdeaProjects\\simple-web-server\\res"
    var serverroot = ""
    var port = 80
    var ip = ""
    var host = ""

    override fun run () {
        super.run()
        println( "---- Connection Accepted: $name ----" )

        val br = BufferedReader(InputStreamReader( client.getInputStream()))
        val ps = PrintStream( client.getOutputStream())

        println( "---- Reading Input: $name ----" )

        var message = br.readLine()
        var fullRequest = message + "\n"
        while (!message.isEmpty()) {
            message = br.readLine()
            fullRequest += message + "\n"
        }

        val request = Request(fullRequest)

        println(request.getRequestStr())

        readConf(request.getHost()!!)

        response( ps, request )

        if (
            request.isConnectionExist() &&
            request.getConnection()!! == "keep-alive" &&
            !request.getUserAgent()!!.contains("chrome", true)
        ) {
            println( client.keepAlive )
            println( "---- Connection Keep Alive: $name ----" )
            run()
        }

        println( "---- Connection Closed: $name ----\n" )
        client.close()
    }

    fun readConf(domain: String) {
        val f = File(ROOT + "\\httpd.conf")
        val fr = FileReader(f)
        val br = BufferedReader(fr)
        var line = br.readLine()

        while (line != null) {
            if (line.startsWith("listen", true)) {
                port = line.split(" ")[1].toInt()
                line = br.readLine()
                continue
            }

            if (line.startsWith("domain", true)) {
                if (line.split(" ")[1] == domain) {
                    line = br.readLine()
                    ip = line.split(" ")[1]

                    line = br.readLine()
                    serverroot = line.split(" ")[1]

                    break
                }
            }

            line = br.readLine()
        }

        return
    }

    fun response( ps: PrintStream, request: Request ) {
        val urn = request.getRequestPath()!!
        var f = File(serverroot + urn)
        if (f.exists() && !f.isDirectory()) {
            if (f.extension == "html"){
                responseHtmlFile(ps, f)
            } else {
                responseBinaryFile(ps, f, request)
            }
        } else if (f.exists() && f.isDirectory()) {
            var fIndex = File(serverroot + urn + "index.html")
            if ( !urn.isEmpty() && !urn.last().equals('\\') && !fIndex.exists() ) {
                fIndex = File(serverroot + urn + "\\index.html")
            }
            if (fIndex.exists()) {
                responseHtmlFile(ps, fIndex)
            } else {
                responseDirectoryListing(ps, f)
            }
        } else {
            responseNotFound(ps, 404)
        }
    }

    fun responseHtmlFile(ps: PrintStream, f: File) {
        val fis = FileInputStream(f)
        var fileContent = String(fis.readAllBytes())

        ps.println("HTTP/1.0 200 OK")
        ps.println("Content-Type: text/html")
        ps.println("Content-length: " + fileContent.length)
        ps.println()
        ps.println(fileContent)
        ps.println()
        ps.flush()
    }

    fun responseBinaryFile(ps: PrintStream, f: File, request: Request) {
        val fis = FileInputStream(f)

        if (request.isRangeExist()) {
            val byteStart = request.getRangeStart()!!
            val byteEnd = request.getRangeEnd()!!

            val buffer = ByteArray(1024)
            val contentLength = byteEnd - byteStart + 1

            ps.println("HTTP/1.0 206 Partial Content")
            ps.println("Content-Type: application/" + f.name.substring(f.name.lastIndexOf(".")))
            ps.println("Content-Length: ${contentLength}")
            ps.println("Content-Range: bytes ${byteStart}-${byteEnd}/${f.length()}")
            ps.println()

            var bytesRead = -1
            var bytesTotal = 0
            while (
                fis.read(buffer, 0, 1024).also { bytesRead = it } != -1 &&
                bytesTotal <= contentLength
            ) {
                ps.write(buffer, 0, bytesRead)
                bytesTotal += bytesRead
            }
        }
        else {
            ps.println("HTTP/1.0 200 OK")
            ps.println("Content-Type: application/" + f.name.substring(f.name.lastIndexOf(".")))
            ps.println("Content-Length: " + f.length())
            ps.println()

            var buffer = ByteArray(1024)
            while(fis.available() > 0) {
                ps.write(buffer, 0, fis.read(buffer))
            }
        }

        ps.flush()
    }

    fun responseDirectoryListing(ps: PrintStream, f: File) {
        val fileList = f.listFiles()

        data class Directory(
            val name: String,
            val path: String,
            val lastModified: String,
            val size: String
        )

        val directories = mutableListOf<Directory>()

        for (file in fileList) {
            if (file.isDirectory()) {
                val name = file.name + "/";
                val path = file.path.substring(serverroot.length - 1)
                val lastModified = Date(file.lastModified()).toString()
                val size = file.length().toString()
                directories.add(Directory(name, path, lastModified, size))
            }
        }

        for (file in fileList) {
            if (!file.isDirectory) {
                val name = file.name
                val path = file.path.substring(serverroot.length - 1)
                val lastModified = Date(file.lastModified()).toString()
                val size = file.length().toString()
                directories.add(Directory(name, path, lastModified, size))
            }
        }

        var res = StringBuilder()

        res.append("<table>");
        res.append("<tr>");
        res.append("    <th>Name</th>");
        res.append("    <th>Last Modified</th>");
        res.append("    <th>Size(Bytes)</th>");
        res.append("</tr>");

        for (file in directories) {
            res.append("<tr>")
            res.append("    <td><a href=\"${file.path}\">${file.name}</a></td>")
            res.append("    <td>${file.lastModified}</td>")
            res.append("    <td>${file.size}</td>")
            res.append("<tr>")
        }
        res.append("</table>")

        ps.println("HTTP/1.0 200 OK")
        ps.println("Content-Type: text/html")
        ps.println("Content-Length: " + res.length)
        ps.println()
        ps.println(res.toString())
        ps.println()
        ps.flush()
    }

    fun responseNotFound(ps: PrintStream, code: Int) {
        val f = File(ROOT + "\\404.html")
        val fis = FileInputStream(f)
        var fileContent = String(fis.readAllBytes())

        ps.println("HTTP/1.0 404 Not Found")
        ps.println("Content-Type: text/html")
        ps.println("Content-Length: " + f.length())
        ps.println()
        ps.println(fileContent)
        ps.println()
        ps.flush()
    }
}