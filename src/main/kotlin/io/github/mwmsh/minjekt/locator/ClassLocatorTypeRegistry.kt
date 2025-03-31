package io.github.mwmsh.minjekt.locator

import java.util.*
import kotlin.reflect.KClass

class ClassLocatorTypeRegistry {
    private val classToLocatorMap: HashMap<KClass<*>, LocatorType> = HashMap()

    fun find(clazz: KClass<*>): LocatorType? {
        return classToLocatorMap[clazz]
    }

    fun add(clazz: KClass<*>, locatorType: LocatorType) {
        classToLocatorMap[clazz] = locatorType
    }
}
