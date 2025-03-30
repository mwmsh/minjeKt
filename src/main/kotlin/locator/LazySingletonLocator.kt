package com.minjeKt.locator

import com.minjeKt.exception.CircularDependencyException
import com.minjeKt.exception.ConstructorIsNotAccessibleException
import com.minjeKt.exception.DependencyNotRegisteredException
import com.minjeKt.exception.PrimaryConstructorNotFoundException
import java.util.HashSet
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.primaryConstructor

class LazySingletonLocator : Locator {
    val singletonInstances: HashMap<KClass<*>, Any> = HashMap()
    val singletonClasses: HashMap<KClass<*>, KClass<*>> = HashMap()
    val visited: HashSet<KClass<*>> = HashSet()

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

    fun init(clazz: KClass<*>): Any {
        if (singletonInstances.containsKey(clazz)) {
            return singletonInstances[clazz]!!
        }

        if (!singletonClasses.containsKey(clazz)) {
            throw DependencyNotRegisteredException("Type ${clazz.qualifiedName} is not registered. You can register classes using ServiceLocator (e.g., ServiceLocator.registerSingleton<${clazz.simpleName}, ${clazz.simpleName}Impl>() }")
        }

        if (visited.contains(clazz)) {
            throw CircularDependencyException("Could not initialize $clazz. This is likely due to a circular dependency")
        }

        visited.add(clazz)

        val implClass = singletonClasses[clazz]!!

        val constructor = implClass.primaryConstructor

        if (constructor == null) {
            throw PrimaryConstructorNotFoundException("Could not locate primary constructor for type $implClass")
        }

        if (constructor.visibility != KVisibility.PUBLIC) {
            throw ConstructorIsNotAccessibleException("Primary constructor for type $clazz is not public")
        }

        val parameters = constructor.parameters

        val parameterInstances: Array<Any> = parameters.map { p ->
            val parameterClazz = p.type.classifier as KClass<*>

            init(parameterClazz)
        }.toTypedArray()

        val instance = constructor.call(*parameterInstances)

        singletonInstances[clazz] = instance

        return instance
    }
}
