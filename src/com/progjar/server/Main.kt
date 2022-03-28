package com.progjar.server

import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.Date

/**
 * TASKS:
 *
 * 1. Modify your web server to be able to handle multiple clients at the same time
 * 2. make your web server being able to handle parallel downloads from IDM
 */

val ROOT = "C:\\developing\\project\\project-java\\simple-web-server\\res"
//val ROOT = "C:\\Users\\LENOVO\\IdeaProjects\\simple-web-server\\res"
var serverroot = ""
var port = 80
var ip = ""
var host = ""
var clientCounter = 0

var clientAlive : Socket? = null

fun main(args: Array<String>) {
    try {
        val server = ServerSocket( port )
        while (true) {
            val client = server.accept()
            clientCounter++
            var clientThread = ThreadHandler( "client-$clientCounter", client )
            clientThread.start()
        }
        server.close()
    } catch (e: IOException) {
        println( "IO excepetion: \n${e.message}" )
    } catch (e: Exception) {
        println(e.message)
    }
}

class ThreadHandler(name: String, var client: Socket) : Thread(name) {
    override fun run () {
        super.run()
        println( "----connection accepted:$name----" )

        val br = BufferedReader(InputStreamReader( client.getInputStream()))
        val ps = PrintStream( client.getOutputStream())

        var message = ""

        println( "----reading input:$name----" )
        message = br.readLine()

        println("Request: $message")

        var urn = ""
        if (message.split(" ").size > 1) {
            urn = message.split(" ")[1]
            urn = urn.substring( 1 )
        }

        var isKeepAlive = false
        var isChrome = false
        while(!message.isEmpty()) {
            message = br.readLine()

            if (message.contains("keep-alive", true)) {
                isKeepAlive = true
            }
            if (message.contains("chrome", true) || message.contains("Mozilla", true)) {
                isChrome = true
            }

            if (message.startsWith("host", true)) {
                host = message.split(" ")[1]
            }
        }

        readConf(host)

        response( ps, urn )

        /*dont keep chrome alive*/
        if (isKeepAlive && !isChrome ) {
            println( client.keepAlive )
            println( "----connection still alive:$name----" )
//            keepAlive( this.client )
            run()
            return
        }

        println( "----connection closed:$name----\n" )
        client.close()
    }
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

fun response( ps: PrintStream, urn: String ) {
    var f = File(serverroot + urn)
    if (f.exists() && !f.isDirectory()) {
        if (f.extension == "html"){
            responseHtmlFile(ps, f)
        } else {
            responseBinaryFile(ps, f)
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

fun responseBinaryFile(ps: PrintStream, f: File) {
    val fis = FileInputStream(f)

    var response = StringBuilder()

    ps.println("HTTP/1.0 200 OK")
    ps.println("Content-Type: application/" + f.name.substring(f.name.lastIndexOf(".")))
    println( "f.name: " + f.name.substring(f.name.lastIndexOf(".")) )
    println( "ext: " + f.extension )
    ps.println("Content-Length: " + f.length())
    ps.println()

    var buffer = ByteArray(1000)
    while(fis.available() > 0) {
        ps.write(buffer, 0, fis.read(buffer))
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