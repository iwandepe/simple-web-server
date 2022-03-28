package com.progjar.server

import java.io.*
import java.net.ServerSocket

/**
 * TASKS:
 *
 * 1. Modify your web server to be able to handle multiple clients at the same time
 * 2. make your web server being able to handle parallel downloads from IDM
 */

fun main(args: Array<String>) {
    val PORT = 80
    try {
        val server = ServerSocket( PORT )
        var clientCounter = 0

        while (true) {
            val client = server.accept()
            clientCounter++

            var clientThread = ClientWorker( "client-$clientCounter", client )
            clientThread.start()
        }
        server.close()
    } catch (e: IOException) {
        println( "IO excepetion: \n${e.message}" )
    } catch (e: Exception) {
        println(e.message)
    }
}

