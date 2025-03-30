package locator

import com.minjeKt.exception.ConstructorIsNotAccessibleException
import com.minjeKt.exception.PrimaryConstructorNotFoundException
import store.ServiceStore
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.primaryConstructor

class ObjectConstructor(val store: ServiceStore) {
    fun construct(clazz: KClass<*>): Any {
        val constructor = clazz.primaryConstructor

        if (constructor == null) {
            throw PrimaryConstructorNotFoundException("Could not locate primary constructor for type $clazz")
        }

        if (constructor.visibility != KVisibility.PUBLIC) {
            throw ConstructorIsNotAccessibleException("Primary constructor for type $clazz is not public")
        }

        val params = constructor.parameters.map { store.locate(it.type.classifier as KClass<*>) }.toTypedArray()

        return constructor.call(*params)
    }
}
