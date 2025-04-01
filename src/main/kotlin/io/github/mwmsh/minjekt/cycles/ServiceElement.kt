package io.github.mwmsh.minjekt.cycles

import io.github.mwmsh.minjekt.store.ServiceStore
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KVisibility
import kotlin.reflect.full.primaryConstructor

interface ServiceImplementation {
    fun exists(): Boolean
    fun hasPrimaryConstructor(): Boolean
    fun isPrimaryConstructorPublic(): Boolean
    fun isImplConstructable(): Boolean
    fun getChildren(): List<ServiceElement?>
    fun getImplClass(): KClass<*>?
}

class ServiceElementCreator(val store: ServiceStore) {
    val services = HashMap<KClass<*>, ServiceElement>()

    fun createOrGet(service: KClass<*>): ServiceElement {
        if(!services.containsKey(service)) {
            services[service] = ServiceElement(service, ReflectionBasedServiceImpl(store.findServiceImpl(service), this))
        }

        return services[service]!!
    }
}

class ReflectionBasedServiceImpl(private val implClass: KClass<*>?, val serviceCreator: ServiceElementCreator): ServiceImplementation {
    private var children: List<ServiceElement?>? = null
    private var isConstructable: Boolean? = null

    override fun isPrimaryConstructorPublic(): Boolean {
        return exists() && hasPrimaryConstructor() && implClass!!.primaryConstructor!!.visibility == KVisibility.PUBLIC
    }

    override fun hasPrimaryConstructor(): Boolean {
        return exists() && implClass!!.primaryConstructor != null
    }

    override fun exists(): Boolean {
        return implClass != null
    }

    override fun isImplConstructable(): Boolean {
        if (isConstructable == null) {
            isConstructable = allChildrenAreConstructable()
        }

        return isConstructable!!
    }

    private fun allChildrenAreConstructable() =
        getChildren().all { child -> child == null || child.isServiceConstructable() }


    override fun getChildren(): List<ServiceElement?> {
        if (children == null) {
            children = getChildrenInternal()
        }

        return children!!
    }


    private fun getChildrenInternal(): List<ServiceElement?> {
        val primaryConstructor = implClass!!.primaryConstructor

        val parameters = primaryConstructor!!.parameters
        val children: List<KClassifier?> = parameters
            .filter { !it.isOptional }
            .map {
                it.type.classifier
            }

        return children.map {
            if (it != null) serviceCreator.createOrGet(it as KClass<*>) else null
        }
    }

    override fun getImplClass(): KClass<*>? {
        return implClass
    }
}

class ServiceElement(private val service: KClass<*>, val impl: ServiceImplementation) {
    var constructableCheckInProgress: Boolean = false
    var isConstrictable: Boolean? = null

    fun getServiceClass(): KClass<*> {
        return service
    }

    fun isServiceConstructable(): Boolean {
        if (isConstrictable != null) {
            return isConstrictable!!
        }

        if (constructableCheckInProgress) {
            return false
        }

        constructableCheckInProgress = true

        isConstrictable = impl.isImplConstructable()

        return isConstrictable!!
    }

    fun hasRegisteredImplementation(): Boolean {
        return impl.exists()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ServiceElement

        return service == other.service
    }

    override fun hashCode(): Int {
        return service.hashCode()
    }
}