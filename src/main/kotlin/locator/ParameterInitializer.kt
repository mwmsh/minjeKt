package locator

import store.ServiceStore
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

class ParameterInitializer(private val store: ServiceStore) {
    fun initialize(params: List<KParameter>): Array<Any> {
        return params.map {
            val clazz = it.type.classifier as KClass<*>
            store.locate(clazz)
        }.toTypedArray()
    }
}
