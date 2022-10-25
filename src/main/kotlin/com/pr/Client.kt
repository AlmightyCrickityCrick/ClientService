package com.pr

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

var client = HttpClient()

class Client(var client_id :Int):Thread() {
    lateinit var menu : RestaurantList
    lateinit var orderDetails :ClientResponseList
    var finishedOrders:ArrayList<FinishedFoodOrderingOrder> = ArrayList()
    var orderTime :ConcurrentHashMap<Int, Int> = ConcurrentHashMap()
    lateinit var bar :CyclicBarrier

    override fun run() {
        sleep((3*Constants.TIME_UNIT .. 10*Constants.TIME_UNIT).random().toLong())
        getMenu()
        sleep((10*Constants.TIME_UNIT .. 30*Constants.TIME_UNIT).random().toLong())
        order()
        orderDetails.orders = orderByTime(orderDetails.orders)
        bar = CyclicBarrier(orderDetails.orders.size +1)
        for (ord in orderDetails.orders) thread{
            awaitOrder(ord)}
        bar.await()
        sendRatings()
        activeClients.release()
    }
    fun orderByTime(ord:ArrayList<ClientOrderResponse>):ArrayList<ClientOrderResponse>{
        var tmp = ord.sortedBy { it.estimated_waiting_time }.toCollection(ArrayList())
        return tmp
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
        var orderedHere = ArrayList<Int>()
        var maxOrders = (1..menu.restaurants).random()
        for (i in 0 until maxOrders) {
            var currRest:Int
            do currRest = (1..menu.restaurants).random() while (currRest in orderedHere)
            orderedHere.add(currRest)
            var food = ArrayList<Int>()
            var maxFood = intArrayOf(1, 2, 2, 3, 3, 3, 3, 4, 5).random()
            var max_time = 0
            for(j in 0 until maxFood ){
                var curr = (1..menu.restaurants_data[currRest - 1].menuItems).random()
                food.add(curr)
                max_time = if(max_time<menu.restaurants_data[currRest-1].menu[curr - 1].preparationTime) menu.restaurants_data[currRest -1].menu[curr - 1].preparationTime else max_time
            }
            max_time = (max_time *1.8).toInt()
            tmp.add(ClientOrder(currRest, food, setFoodPriority(max_time), max_time, System.currentTimeMillis()))

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

    fun setFoodPriority(max: Int):Int{
        if(max< 10) return 1
        else if (max < 20) return 2
        else if (max < 27) return 3
        else if (max < 40) return 4
        else return 5
    }

    fun awaitOrder(ord:ClientOrderResponse){
        var currOrder = ord
        var is_ready = false
        var actual_time :Int
        do {
            sleep((currOrder.estimated_waiting_time * Constants.TIME_UNIT).toLong())
            runBlocking {
                var job = launch {
                    var time = System.currentTimeMillis()
                    println("${client_id} Trying to get order ${ord.order_id} from restaurant ${ord.restaurant_id}")
                    var resp: HttpResponse = client.get(ord.restaurant_address + "/v2/order/${ord.order_id}")
                    var data = Json.decodeFromString(FinishedFoodOrderingOrder.serializer(), resp.body())
                    if (data.is_ready) {
                        is_ready = true
                        actual_time = ((time - data.registered_time)/Constants.TIME_UNIT).toInt()
                        finishedOrders.add(data)
                        orderTime.put(data.order_id, actual_time)
                        println("Got order ${data.order_id}")
                    }
                    else {
                        currOrder.estimated_waiting_time = data.estimated_waiting_time
                        println("No success with ${ord.order_id}")
                    }
                }
            }
        }while(!is_ready)
        bar.await()
    }
    fun sendRatings(){
        var ratingList = giveRatings()
        runBlocking {
            var job = launch {
                println("Rating for $client_id are $ratingList")
                var resp:HttpResponse = client.post(Constants.FOOD_ORDERING_URL+"/rating"){
                    setBody(Json.encodeToString(RatingRequestList.serializer(), ratingList))
                }
                println(resp.status)
            }
        }
    }

    fun giveRatings() : RatingRequestList{
        var ratingList = RatingRequestList(this.client_id, orderDetails.order_id, ArrayList())
        for (ord in finishedOrders) {
            var rat = orderTime[ord.order_id]?.let { calculateRating(it, ord.max_wait) }
            var orderInfo = orderDetails.getOneOrder(ord.order_id)
            var request = orderInfo?.estimated_waiting_time?.let {
                orderTime[ord.order_id]?.let { it1 ->
                    RatingRequest(
                        orderInfo?.restaurant_id ?: 0, ord.order_id,
                        rat!!, it, it1
                    )
                }
            }
            if (request != null) {
                ratingList.orders.add(request)
            }
        }
        return ratingList

    }

    fun calculateRating(actual:Int, max:Int):Int{
        if(actual<=max) return 5
        else if (actual <= max*1.1) return 4
        else if (actual <= max*1.2) return 3
        else if (actual <= max*1.3) return 2
        else if (actual <= max*1.4) return 1
        else return 0
    }
}