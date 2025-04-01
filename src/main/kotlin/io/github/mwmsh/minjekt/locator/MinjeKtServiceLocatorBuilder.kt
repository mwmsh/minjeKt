package org.minjekt.locator

import io.github.mwmsh.minjekt.locator.LocatorType
import io.github.mwmsh.minjekt.store.ServiceStore
import io.github.mwmsh.minjekt.cycles.ServiceGraphSanityChecker

class MinjeKtServiceLocator(val store: ServiceStore) {
    inline fun <reified TService> locate(): TService {
        return store.locate<TService>()
    }
}

class MinjeKtServiceLocatorBuilder(val store: ServiceStore, val serviceGraphSanityChecker: ServiceGraphSanityChecker) {
    companion object {
        fun create(): MinjeKtServiceLocatorBuilder {
            val store = ServiceStore().init()
            val sanityChecker = ServiceGraphSanityChecker(store)
            return MinjeKtServiceLocatorBuilder(store, sanityChecker)
        }
    }

    inline fun <reified TService, reified TImpl : TService> registerLazySingleton(): MinjeKtServiceLocatorBuilder {
        store.register(TService::class, TImpl::class, LocatorType.LAZY_SINGLETON)
        return this
    }

    inline fun <reified TService> registerLazySingleton(value: TService): MinjeKtServiceLocatorBuilder {
        store.register<TService>(value, LocatorType.LAZY_SINGLETON)
        return this
    }

    inline fun <reified TService, reified TImpl : TService> registerSingleton(): MinjeKtServiceLocatorBuilder {
        store.register(TService::class, TImpl::class, LocatorType.SINGLETON)
        return this
    }

    inline fun <reified TService> registerSingleton(value: TService): MinjeKtServiceLocatorBuilder {
        store.register<TService>(value, LocatorType.SINGLETON)
        return this
    }

    inline fun <reified TService, reified TImpl : TService> registerTransient(): MinjeKtServiceLocatorBuilder {
        store.register<TService, TImpl>(LocatorType.TRANSIENT)
        return this
    }

    fun build(): MinjeKtServiceLocator {
        serviceGraphSanityChecker.ensureSaneServiceGraphOrThrowErrors()

        store.getAllEagerLocators().forEach {
            it.initialize()
        }

        return MinjeKtServiceLocator(store)
    }
}