package com.progjar.server

import java.io.*
import java.net.ServerSocket


/**
 * TASKS:
 *
 * 1. Serves any file under the website root directory to the client. If the requested file is a text or HTML file, the browser will show the content. If it is a binary file (e.g. PDF, images, documents, etc) the browser will download it. (Hint: use the correct content-type for each file served)
 * 2. Shows a list of files and folders when the client requests a directory that does not have index.html inside it. The list must show the file/folder name, last modified date, and size. The name must also be clickable and make the user requests the file/folder when they click it. (Hint: send a temporary HTML string containing the necessary contents)
 * 3. Serves multiple websites like Nginx/Apache VirtualHost. The client must be able to access various domains handled by your web server. (Hint: modify /etc/hosts in Linux or C:\Windows\System32\drivers\etc\hosts in Windows to add your own domain names such that your browser can recognise them)
 * 4. Has a configuration file that allows us to configure the IP address and port bound by the webserver. The file must also include the root directory of each website handled by your webserver.
 * 5. Keeps the connection open if the client requests it. (Hint: check the Connection HTTP header. Your webserver will not be able to accept another client once the connection is still open, but that is okay)
 */

val ROOTDIR = "/home/iwandepe/progjar/www/"
val PORT = 8888

fun main() {
    try {
        val server = ServerSocket(PORT)

        while (true) {
            val client = server.accept()

            val br = BufferedReader(InputStreamReader(client.getInputStream()))
            val out = PrintWriter(client.getOutputStream())

            var message = br.readLine()
            println("Request: $message")

            var urn = message.split(" ")[1]
            urn = urn.substring( 1 )

            while(!message.isEmpty()) {
                message = br.readLine()
            }

            var f = File(ROOTDIR + urn)
            if (f.exists() && !f.isDirectory()) {
                if (f.extension == "html"){
                    responseHtmlFile(out, f)
                } else {
                    responseBinaryFile(out, f)
                }
            } else if (f.exists() && f.isDirectory()) {
                var fIndex = File(ROOTDIR + urn + "index.html")
                if (fIndex.exists()) {
                    responseHtmlFile(out, fIndex)
                } else {
                    responseDirectoryListing(out, f)
                }
            } else {
                responseByStatusCode(out, 404)
            }

            client.close()
        }
        server.close()
    } catch (e: Exception) {
        println(e.message)
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

// Not working right
fun responseBinaryFile(out: PrintWriter, f: File) {
    val fis = FileInputStream(f)

    out.println("HTTP/1.0 200 OK")
    out.println("Content-Type: application/octet-stream")
    out.println("Content-Disposition: attactment; filename=\"" + f.path.substring(ROOTDIR.length) + "\"")
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
        response += "<a href=\"" + f.path.substring(ROOTDIR.length) + "/" + file.name + "\">"

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

    out.println("HTTP/1.0" + status)
    out.println("Content-Type: html")
    out.println()
    out.println(status)
    out.println()
    out.flush()
}