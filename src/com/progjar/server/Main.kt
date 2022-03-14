package com.progjar.server

import java.io.*
import java.net.ServerSocket
import java.net.Socket


/**
 * TASKS:
 *
 * 1. Serves any file under the website root directory to the client. If the requested file is a text or HTML file, the browser will show the content. If it is a binary file (e.g. PDF, images, documents, etc) the browser will download it. (Hint: use the correct content-type for each file served)
 * 2. Shows a list of files and folders when the client requests a directory that does not have index.html inside it. The list must show the file/folder name, last modified date, and size. The name must also be clickable and make the user requests the file/folder when they click it. (Hint: send a temporary HTML string containing the necessary contents)
 * 3. Serves multiple websites like Nginx/Apache VirtualHost. The client must be able to access various domains handled by your web server. (Hint: modify /etc/hosts in Linux or C:\Windows\System32\drivers\etc\hosts in Windows to add your own domain names such that your browser can recognise them)
 * 4. Has a configuration file that allows us to configure the IP address and port bound by the webserver. The file must also include the root directory of each website handled by your webserver.
 * 5. Keeps the connection open if the client requests it. (Hint: check the Connection HTTP header. Your webserver will not be able to accept another client once the connection is still open, but that is okay)
 */

val CONF_PATH = "C:\\Users\\LENOVO\\IdeaProjects\\simple-web-server\\res\\httpd.conf"
var serverroot = "C:\\"
//var serverroot = "C:\\developing\\project\\project-java\\simple-web-server\\res\\"
var port = 80
var ip : String? = "127.0.0.1"
val REQUEST_TIMEOUT = 3L
var host = ""

var clientAlive : Socket? = null

fun main(args: Array<String>) {
    try {
        val server = ServerSocket( port )
        while (true) {
            val client = server.accept()

            val br = BufferedReader(InputStreamReader(client.getInputStream()))
            val out = PrintWriter(client.getOutputStream())

            var message = br.readLine()
            println( "----connection accepted----" )
            println("Request: $message")

            var urn = message.split(" ")[1]
            urn = urn.substring( 1 )

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

            response( out, urn )

            /*dont keep chrome alive*/
            if (isKeepAlive && !isChrome) {
                println( client.keepAlive )
                println( "----connection still alive----" )
                keepAlive( client )
            }

            println( "----connection closed----\n" )
            client.close()
        }
        server.close()
    } catch (e: Exception) {
        println(e.message)
    }
}

fun readConf(domain: String) {
    val f = File(CONF_PATH)
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

                line = br.readLine()
                break
            }
        }

        line = br.readLine()
    }

    return
}

fun keepAlive( client: Socket ) {
    println( '1' )
    var keepAlive = false

    var br = BufferedReader(InputStreamReader(client.getInputStream()))
    var message: String? = ""
    try{
        message = br.readLine()
        println( "**empty input**" )
    } catch (e: IOException) {
        println( "**empty input**" )
        message = ""
        return
    }

    val out = PrintWriter(client.getOutputStream())
    println("Request: $message")
    if (message.isNullOrEmpty()) {
        message = ""
        return
    }

    var urn = message.split(" ")[1]
    urn = urn.substring( 1 )

    println( '2' )
    while(!message!!.isEmpty()) {
        message = br.readLine()

        if (message.contains("keep-alive")) {
            keepAlive = true
        }
    }
    println( '3' )

    response( out, urn )

    if (keepAlive == true) {
        println( "----connection still alive----\n" )
        keepAlive( client )
    } else return
}

fun response( out: PrintWriter, urn: String ) {
    var f = File(serverroot + urn)
    if (f.exists() && !f.isDirectory()) {
        if (f.extension == "html"){
            responseHtmlFile(out, f)
        } else {
            responseBinaryFile(out, f)
        }
    } else if (f.exists() && f.isDirectory()) {
        var fIndex = File(serverroot + urn + "index.html")
        if ( !urn.isEmpty() && !urn.last().equals('\\') && !fIndex.exists() ) {
            fIndex = File(serverroot + urn + "\\index.html")
        }
        if (fIndex.exists()) {
            responseHtmlFile(out, fIndex)
        } else {
            responseDirectoryListing(out, f)
        }
    } else {
        responseByStatusCode(out, 404)
    }
}

fun responseHtmlFile(out: PrintWriter, f: File) {
    val fis = FileInputStream(f)
    var fileContent = String(fis.readAllBytes())

    out.println("HTTP/1.0 200 OK")
    out.println("Content-Type: text/html")
    out.println("Content-length: " + fileContent.length)
    out.println()
    out.println(fileContent)
    out.println()
    out.flush()
}

// Not working correctly
fun responseBinaryFile(out: PrintWriter, f: File) {
    val fis = FileInputStream(f)

    out.println("HTTP/1.0 200 OK")
    out.println("Content-Type: application/octet-stream")
    out.println("Content-Disposition: attactment; filename=\"" + f.path.substring(serverroot.length) + "\"")
    out.println()
    var reads = fis.read()
    while(reads != -1) {
        out.write(reads)
        reads = fis.read()
    }
    out.println()
    out.flush()
}

fun responseDirectoryListing(out: PrintWriter, f: File) {
    val fileList = f.listFiles()

    var response = "<ul>"
    for (file in fileList) {
        response += "<li>"
        response += "<a href=\"" + f.path.substring(serverroot.length - 1) + "/" + file.name + "\">"

        if (file.isDirectory()) {
            response += file.name + "/\n"
        } else {
            response += file.name + "\n"
        }

        response += "</a>"
        response += "</li>"
    }
    response += "</ul>"

    out.println("HTTP/1.0 200 OK")
    out.println("Content-Type: text/html")
    out.println()
    out.println(response)
    out.println()
    out.flush()
}

fun responseByStatusCode(out: PrintWriter, code: Int) {
    var status = ""
    if (code == 404) {
        status = "404 Not Found"
    }

    out.println("HTTP/1.0 " + status)
    out.println("Content-Type: html")
    out.println()
    out.println(status)
    out.println()
    out.flush()
}