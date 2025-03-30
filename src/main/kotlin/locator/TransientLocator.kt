package com.minjeKt.locator

import com.minjeKt.exception.DependencyNotRegisteredException
import locator.ObjectConstructor
import kotlin.reflect.KClass

class TransientLocator(val objectConstructor: ObjectConstructor): Locator {
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
            throw DependencyNotRegisteredException("Service not registered $service")
        }

        return objectConstructor.construct(impl)
    }

}