package io.github.mwmsh.minjekt.locator

import io.github.mwmsh.minjekt.store.ServiceStore
import io.github.mwmsh.minjekt.exception.ConstructorIsNotAccessibleException
import io.github.mwmsh.minjekt.exception.PrimaryConstructorNotFoundException
import java.util.stream.Collectors
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.primaryConstructor

interface Constructor {
    fun construct(clazz: KClass<*>): Any
}

class CrossLocatorObjectConstructor(val store: ServiceStore): Constructor {
    override fun construct(clazz: KClass<*>): Any {
        val constructor = clazz.primaryConstructor

        if (constructor == null) {
            throw PrimaryConstructorNotFoundException("Could not locate primary constructor for type $clazz")
        }

        if (constructor.visibility != KVisibility.PUBLIC) {
            throw ConstructorIsNotAccessibleException("Primary constructor for type $clazz is not public")
        }

        val args = constructor.parameters.stream()
            .filter { !it.isOptional }
            .collect(Collectors.toMap({ it }, { store.locate(it.type.classifier as KClass<*>) }))

        return constructor.callBy(args)
    }
}
