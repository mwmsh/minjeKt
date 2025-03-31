package io.github.mwmsh.minjekt.locator

import java.util.*
import kotlin.reflect.KClass

class LazySingletonLocator(val objectConstructor: ObjectConstructor) : Locator {
    val singletonInstances: HashMap<KClass<*>, Any> = HashMap()
    val singletonClasses: HashMap<KClass<*>, KClass<*>> = HashMap()

    override fun register(service: KClass<*>, impl: KClass<*>) {
        singletonClasses[service] = impl
    }

    override fun register(service: KClass<*>, obj: Any) {
        singletonInstances[service] = obj
    }

    override fun locate(service: KClass<*>): Any {
        if (!singletonInstances.containsKey(service)) {
            init(service)
        }

        return singletonInstances[service]!!
    }

    private fun init(service: KClass<*>): Any {
        val implClass = singletonClasses[service]!!
        val instance = objectConstructor.construct(implClass)
        singletonInstances[service] = instance
        return instance
    }
}
