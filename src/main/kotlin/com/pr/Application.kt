package com.pr

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.pr.plugins.*
import kotlinx.coroutines.sync.Semaphore
import java.lang.Thread.sleep

var activeClients = Semaphore(5)
var clientList = ArrayList<Client>()

suspend fun main() {
    while(true){
        if(activeClients.availablePermits>0) {
            activeClients.acquire()
            var tmp = Client((0..20).random())
            tmp.start()
            clientList.add(tmp)

        }
    }
}
