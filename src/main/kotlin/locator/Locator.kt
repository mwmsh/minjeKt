package com.minjeKt.locator

import kotlin.reflect.KClass

interface Locator {
    fun register(service: KClass<*>, impl: KClass<*>)
    fun register(service: KClass<*>, obj: Any)
    fun locate(service: KClass<*>): Any
}

interface EagerLocator: Locator {
    fun initialize()
}
