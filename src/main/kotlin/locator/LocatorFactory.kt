package locator

import com.minjeKt.locator.LazySingletonLocator
import com.minjeKt.locator.Locator
import com.minjeKt.locator.TransientLocator

class LocatorFactory(lazySingletonLocator: LazySingletonLocator, transientLocator: TransientLocator){

    private val locators: Map<LocatorType, Locator> = mapOf(
        LocatorType.TRANSIENT to transientLocator,
        LocatorType.LAZY_SINGLETON to lazySingletonLocator,
        LocatorType.SINGLETON to lazySingletonLocator
    )

    fun get(type: LocatorType): Locator {
        if (!locators.containsKey(type)) {
            throw IllegalArgumentException("Could not find locator $type")
        }

        return locators[type]!!
    }
}
