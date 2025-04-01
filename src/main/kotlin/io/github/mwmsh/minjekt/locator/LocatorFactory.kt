package io.github.mwmsh.minjekt.locator


class LocatorFactory(lazySingletonLocator: LazySingletonLocator, transientLocator: TransientLocator, singletonLocator: EagerSingletonLocator){
    val locators: Map<LocatorType, Locator> = mapOf(
        LocatorType.TRANSIENT to transientLocator,
        LocatorType.LAZY_SINGLETON to lazySingletonLocator,
        LocatorType.SINGLETON to singletonLocator
    )

    fun get(type: LocatorType): Locator {
        if (!locators.containsKey(type)) {
            throw IllegalArgumentException("Could not find locator $type")
        }

        return locators[type]!!
    }

    inline fun <reified T>getByType(): List<T> {
        return locators.values.filterIsInstance<T>()
    }
}
