package com.pr

import kotlinx.serialization.Serializable

//Short info about Restaurant when sending menu to client
@Serializable
data class RestaurantSmall(val restaurant_id: Int, val name:String, val menuItems: Int, val menu: ArrayList<Food>, var rating: Float)

@Serializable
data class Food(val id: Int, val name : String, val preparationTime : Int, val complexity: Int, val cookingApparatus: String? =null)

//Sending menu to client
@Serializable
data class RestaurantList(val restaurants:Int, val restaurants_data: ArrayList<RestaurantSmall>)

//Receiving Order list from many restaurants from client
@Serializable
data class ClientOrderList(val client_id:Int, val orders: ArrayList<ClientOrder>)

//Info about each order from 1 restaurant for 1 client
@Serializable
data class ClientOrder(val restaurant_id: Int, val items:ArrayList<Int>, val priority:Int, val max_wait:Int, val created_time: Long)

//What to send to client after Receiving order list
@Serializable
data class ClientResponseList(val order_id:Int, var orders: ArrayList<ClientOrderResponse>){
    fun orderBytime(){
        var new_orders = orders.sortedBy { it.estimated_waiting_time }
        this.orders = new_orders as ArrayList<ClientOrderResponse>
    }
    fun getOneOrder(i:Int):ClientOrderResponse?{
        for (ord in orders) if (ord.order_id == i) return ord
        return null
    }
}

//Each Restaurant response component for sending to client after receiving OrderList
@Serializable
data class ClientOrderResponse(val restaurant_id: Int, val restaurant_address:String, val order_id: Int, var estimated_waiting_time:Int, val created_time: Long, val registered_time:Long)

//What client sends to rate restaurants
@Serializable
data class RatingRequestList(val client_id: Int, val order_id: Int, val orders: ArrayList<RatingRequest>)
//Rating a single restaurant from Client
@Serializable
data class RatingRequest(var restaurant_id: Int, val order_id: Int, val rating: Int, val estimated_waiting_time: Int, val waiting_time: Int)

@Serializable
data class CookingDetail(val food_id:Int, val cook_id:Int? = null)

//Response when client checks if order ready
@Serializable
data class FinishedFoodOrderingOrder(val order_id:Int, var is_ready: Boolean, var estimated_waiting_time: Int, val priority: Int, val max_wait: Int, val created_time: Long, val registered_time: Long, var preparedTime:Long, var cooking_time: Long, var cooking_details: ArrayList<CookingDetail>?=null)

//Request from client to rate restaurant
@Serializable
data class ClientRating(val order_id: Int, val rating: Int, val estimated_waiting_time: Int, val waiting_time:Int)