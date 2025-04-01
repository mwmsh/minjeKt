package io.github.mwmsh.minjekt.locator

import io.github.mwmsh.minjekt.exception.DependencyNotRegisteredException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

enum class InitializationStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED;
}

class EagerSingletonLocator(private val objectConstructor: CrossLocatorObjectConstructor) : EagerLocator {
    val serviceClasses: HashMap<KClass<*>, KClass<*>> = HashMap()
    val serviceInstances = HashMap<KClass<*>, Any>()
    var initialization = InitializationStatus.NOT_STARTED

    @Synchronized
    override fun initialize() {
        initialization = InitializationStatus.IN_PROGRESS

        val initializationCandidates = serviceClasses.keys.toSet().minus(serviceInstances.keys.toSet())
        initializationCandidates.forEach { service ->
            val implClass: KClass<*>? = serviceClasses[service]

            if (implClass == null) {
                throw DependencyNotRegisteredException("$service not registered")
            }

            serviceInstances[service] = objectConstructor.construct(implClass)
        }

        initialization = InitializationStatus.COMPLETED
    }

    override fun register(service: KClass<*>, impl: KClass<*>) {
        serviceClasses[service] = impl
    }

    override fun register(service: KClass<*>, obj: Any) {
        serviceInstances[service] = obj
    }

    override fun locate(service: KClass<*>): Any {
        when (initialization) {
            InitializationStatus.COMPLETED -> {
                if (!serviceClasses.containsKey(service) && !serviceInstances.containsKey(service)) {
                    throw DependencyNotRegisteredException("$service not registered.")
                }

                return serviceInstances[service]!!
            }

            InitializationStatus.NOT_STARTED -> {
                throw IllegalStateException("Cannot locate $service. EagerSingletonLocator is not initialized.")
            }

            //This is objectConstructor asking us to locate dependencies during Eager initialization
            InitializationStatus.IN_PROGRESS -> {
                if (!serviceClasses.containsKey(service) && !serviceInstances.containsKey(service)) {
                    throw DependencyNotRegisteredException("$service not registered.")
                }

                synchronized(service.jvmName) {
                    if (!serviceInstances.containsKey(service)) {
                        val implClass = serviceClasses[service]!!
                        serviceInstances[service] = objectConstructor.construct(implClass)
                    }

                    return serviceInstances[service]!!
                }
            }

        }
    }
}