package io.github.mwmsh.minjekt.locator

import io.github.mwmsh.minjekt.store.ServiceStore
import io.github.mwmsh.minjekt.cycles.ServiceGraphSanityChecker

class MinjeKt(val store: ServiceStore) {
    inline fun <reified TService> locate(): TService {
        return store.locate<TService>()
    }
}

class MinjeKtBuilder(val store: ServiceStore, val serviceGraphSanityChecker: ServiceGraphSanityChecker) {
    companion object {
        fun create(): MinjeKtBuilder {
            val store = ServiceStore().init()
            val sanityChecker = ServiceGraphSanityChecker(store)
            return MinjeKtBuilder(store, sanityChecker)
        }
    }

    inline fun <reified TService, reified TImpl : TService> registerLazySingleton(): MinjeKtBuilder {
        store.register(TService::class, TImpl::class, LocatorType.LAZY_SINGLETON)
        return this
    }

    inline fun <reified TService> registerLazySingleton(value: TService): MinjeKtBuilder {
        store.register<TService>(value, LocatorType.LAZY_SINGLETON)
        return this
    }

    inline fun <reified TService, reified TImpl : TService> registerSingleton(): MinjeKtBuilder {
        store.register(TService::class, TImpl::class, LocatorType.SINGLETON)
        return this
    }

    inline fun <reified TService> registerSingleton(value: TService): MinjeKtBuilder {
        store.register<TService>(value, LocatorType.SINGLETON)
        return this
    }

    inline fun <reified TService, reified TImpl : TService> registerTransient(): MinjeKtBuilder {
        store.register<TService, TImpl>(LocatorType.TRANSIENT)
        return this
    }

    fun build(): MinjeKt {
        serviceGraphSanityChecker.ensureSaneServiceGraphOrThrowErrors()

        store.getAllEagerLocators().forEach {
            it.initialize()
        }

        return MinjeKt(store)
    }
}