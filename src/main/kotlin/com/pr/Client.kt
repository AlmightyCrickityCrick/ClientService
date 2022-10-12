package com.pr

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

var client = HttpClient()

class Client(var client_id :Int):Thread() {
    lateinit var menu : RestaurantList
    lateinit var orderDetails :ClientResponseList
    override fun run() {
        sleep((1000 .. 9000).random().toLong())
        getMenu()
        order()
        activeClients--
    }


    fun getMenu(){
        runBlocking {
            var job = launch{
                var resp: HttpResponse = client.get(Constants.FOOD_ORDERING_URL +"/menu")
                menu = Json.decodeFromString(RestaurantList.serializer(), resp.body())
                println("Client $client_id got menu")
            }
        }
    }

    fun order(){
        var tmp = ArrayList<ClientOrder>()
        for (i in 0 until menu.restaurants) {
            var food = ArrayList<Int>()
            var max_time = 0
            for(j in 0 .. 2 ){
                var curr = (1..menu.restaurants_data[i].menuItems).random()
                food.add(curr)
                max_time = if(max_time<menu.restaurants_data[i].menu[curr - 1].preparationTime) menu.restaurants_data[i].menu[curr - 1].preparationTime else max_time
            }
            max_time = (max_time *1.8).toInt()
            tmp.add(ClientOrder(i+1, food, (1..5).random(), max_time, System.currentTimeMillis()))

        }
        var ord = ClientOrderList(client_id, tmp)
        runBlocking {
            var job = launch{
                var resp: HttpResponse = client.post(Constants.FOOD_ORDERING_URL +"/order"){
                    setBody(Json.encodeToString(ClientOrderList.serializer(), ord))
                }
                println("Client $client_id ordered $tmp")

                 orderDetails= Json.decodeFromString(ClientResponseList.serializer(), resp.body())
                println("Client waiting for food. Got response $orderDetails from food ordering ")
            }
        }
    }
}