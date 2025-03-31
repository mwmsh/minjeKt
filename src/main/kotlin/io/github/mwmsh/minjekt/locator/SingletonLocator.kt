package io.github.mwmsh.minjekt.locator

import io.github.mwmsh.minjekt.exception.DependencyNotRegisteredException
import java.util.*
import kotlin.reflect.KClass

class SingletonLocator(private val objectConstructor: ObjectConstructor) : EagerLocator {
    val serviceClasses: HashMap<KClass<*>, KClass<*>> = HashMap()
    val serviceInstances = HashMap<KClass<*>, Any>()
    var initialized: Boolean = false
    override fun initialize() {
        val initializationCandidates = serviceClasses.keys.toSet().minus(serviceInstances.keys.toSet())
        initializationCandidates.forEach { service ->
            val implClass: KClass<*>? = serviceClasses[service]

            if (implClass == null) {
                throw DependencyNotRegisteredException("$service not registered")
            }

            serviceInstances[service] = objectConstructor.construct(implClass)
        }
        initialized = true
    }

    override fun register(service: KClass<*>, impl: KClass<*>) {
        serviceClasses[service] = impl
    }

    override fun register(service: KClass<*>, obj: Any) {
        serviceInstances[service] = obj
    }

    override fun locate(service: KClass<*>): Any {
        if (!initialized) {
            throw IllegalStateException("Cannot locate $service. Service locator is not initialized.")
        }

        if (!serviceClasses.containsKey(service) && !serviceInstances.containsKey(service)) {
            throw DependencyNotRegisteredException("$service not registered.")
        }

        return serviceInstances[service]!!
    }
}