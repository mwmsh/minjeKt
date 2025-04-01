package io.github.mwmsh.minjekt.store

import io.github.mwmsh.minjekt.cycles.ServiceElement
import io.github.mwmsh.minjekt.cycles.ServiceElementCreator
import io.github.mwmsh.minjekt.locator.*
import io.github.mwmsh.minjekt.exception.DependencyNotRegisteredException
import java.util.*
import kotlin.reflect.KClass

class ServiceStore() {

    private var locatorFactory: LocatorFactory? = null
    private var serviceLocatorRegistry: ClassLocatorTypeRegistry? = null
    private var registeredClasses: HashMap<KClass<*>, KClass<*>> = HashMap()
    private var serviceElementCreator = ServiceElementCreator(this)

    fun init(): ServiceStore {
        val parameterInitializer = CrossLocatorObjectConstructor(this)
        val lazySingletonLocator = LazySingletonLocator(parameterInitializer)
        val transientLocator = TransientLocator(parameterInitializer)
        val singletonLocator = EagerSingletonLocator(parameterInitializer)
        this.locatorFactory =
            LocatorFactory(lazySingletonLocator, transientLocator, singletonLocator)
        this.serviceLocatorRegistry = ClassLocatorTypeRegistry()
        return this
    }

    inline fun <reified TService> locate(): TService {
        return locate(TService::class) as TService
    }

    inline fun <reified TService, reified TImpl : TService> register(locatorType: LocatorType) {
        return register(TService::class, TImpl::class, locatorType)
    }

    inline fun <reified TService> register(value: TService, locatorType: LocatorType) {
        return register(TService::class, value as Any, locatorType)
    }

    fun locate(service: KClass<*>): Any {
        ensureInitialized()

        val locatorType = serviceLocatorRegistry!!.find(service)

        if (locatorType == null) {
            throw DependencyNotRegisteredException("No registered service for $service")
        }

        val locator = locatorFactory!!.get(locatorType)

        return locator.locate(service)
    }

    private fun ensureInitialized() {
        if (isNotInitialized()) {
            init()

            if (isNotInitialized()) {
                throw IllegalStateException("ServiceStore is not initialized")
            }
        }
    }

    private fun isNotInitialized() = locatorFactory == null || serviceLocatorRegistry == null

    fun register(service: KClass<*>, impl: KClass<*>, locatorType: LocatorType) {
        ensureInitialized()

        serviceLocatorRegistry!!.add(service, locatorType)
        val locator = locatorFactory!!.get(locatorType)
        locator.register(service, impl)
        registeredClasses[service] = impl
    }

    fun register(service: KClass<*>, value: Any, locatorType: LocatorType) {
        ensureInitialized()

        serviceLocatorRegistry!!.add(service, locatorType)
        val locator = locatorFactory!!.get(locatorType)
        locator.register(service, value)
        registeredClasses[service] = value::class
    }

    internal fun findServiceImpl(service: KClass<*>): KClass<*>? {
        return registeredClasses[service]
    }

    internal fun getAllServices(): List<ServiceElement> {
        return registeredClasses.keys.map{serviceElementCreator.createOrGet(it)}
    }

    internal fun getAllEagerLocators(): List<EagerLocator> {
        ensureInitialized()

        return locatorFactory!!.getByType<EagerLocator>()
    }
}