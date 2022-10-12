package com.pr

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.pr.plugins.*
import java.lang.Thread.sleep

var activeClients = 0
var clientList = ArrayList<Client>()

fun main() {
    while(true){
        if(activeClients<5) {
            activeClients++
            var tmp = Client((0..20).random())
            tmp.start()
            clientList.add(tmp)

        }
    }
}
