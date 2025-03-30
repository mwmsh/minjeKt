package com.minjeKt.locator

import cycles.CycleDetector
import locator.LocatorType
import store.ServiceStore

class MinjeKtServiceLocator(val store: ServiceStore) {
    inline fun <reified TService> locate(): TService {
        return store.locate<TService>()
    }

}

class MinjeKtServiceLocatorBuilder(val store: ServiceStore) {
    companion object {
        private val instance: MinjeKtServiceLocatorBuilder? = null
        fun instance(): MinjeKtServiceLocatorBuilder {
            val store = ServiceStore().init()
            return MinjeKtServiceLocatorBuilder(store)
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

    inline fun <reified TService, reified TImpl : TService> registerTransient(): MinjeKtServiceLocatorBuilder {
        store.register<TService, TImpl>(LocatorType.TRANSIENT)
        return this
    }

    fun build(): MinjeKtServiceLocator {
        CycleDetector.ensureSaneDependencyGraph(store)
        return MinjeKtServiceLocator(store)
    }
}