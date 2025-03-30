package com.minjeKt.locator

class MinjeKtServiceLocator() {
    val lazySingletonLocator = LazySingletonLocator()

    companion object {
        private var instance: MinjeKtServiceLocator? = null
        fun instance(): MinjeKtServiceLocator {
            if (instance == null) {
                instance = MinjeKtServiceLocator()
            }

            return instance!!
        }
    }

    inline fun <reified TService> locate(): TService {
        return lazySingletonLocator.locate(TService::class) as TService
    }

    inline fun <reified TService, reified TImpl : TService> registerLazySingleton(): MinjeKtServiceLocator {
        lazySingletonLocator.register(TService::class, TImpl::class)
        return this
    }

    inline fun <reified TService> registerLazySingleton(value: TService): MinjeKtServiceLocator {
        lazySingletonLocator.register(TService::class, value as Any)
        return this
    }
}