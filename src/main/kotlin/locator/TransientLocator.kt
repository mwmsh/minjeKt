package com.minjeKt.locator

import locator.ParameterInitializer
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class TransientLocator(val paramInitializer: ParameterInitializer): Locator {
    private val serviceClasses: HashMap<KClass<*>, KClass<*>> = HashMap()

    override fun register(service: KClass<*>, impl: KClass<*>) {
        serviceClasses[service] = impl
    }

    //TODO registering instances is meaningless for transient objects. This is a possible refactor
    override fun register(service: KClass<*>, obj: Any) {
        throw NotImplementedError()
    }

    override fun locate(service: KClass<*>): Any {
        val impl = serviceClasses[service]
        if (impl == null) {
            throw IllegalArgumentException("Service not registered $service")
        }

        val constructor = impl.primaryConstructor

        if (constructor == null) {
            throw IllegalArgumentException("No primary constructor found for type $impl")
        }

        val params = paramInitializer.initialize(constructor.parameters)

        return constructor.call(*params)
    }

}