package com.grytsyna.fixitpro.activity.main

import com.grytsyna.fixitpro.entity.Order

interface OnOrdersLoadedListener {
    fun onOrdersLoaded(orders: List<Order>)
    fun onOrderAdded(order: Order)
}
