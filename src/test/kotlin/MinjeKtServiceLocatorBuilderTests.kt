import com.minjeKt.exception.CircularDependencyException
import com.minjeKt.exception.DependencyNotRegisteredException
import com.minjeKt.locator.MinjeKtServiceLocatorBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import store.ServiceStore
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DummyTestClass {

}

interface DummyInterface {

}

class DummyImpl : DummyInterface {}

class CircularDep1(x: CircularDep2)

class CircularDep2(x: CircularDep1)

class CircularDepA(x: CircularDepB)
class CircularDepB(x: CircularDepC)
class CircularDepC(x: CircularDepA)

class SingletonDep(){}
class TransientDep(val x: SingletonDep){}

class TransientDep2()
class SingletonDep2(val x: TransientDep2)

class MinjeKtServiceLocatorBuilderTests {
    @Test
    fun whenAnUnregisteredClassIsLocated_AnExceptionIsThrown() {
        assertThrows<DependencyNotRegisteredException> {
            MinjeKtServiceLocatorBuilder(ServiceStore()).build().locate<DummyTestClass>()
        }
    }

    @Test
    fun whenARegisteredSingletonClassIsLocated_NoExceptionIsThrown() {
        val locator = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerLazySingleton<DummyTestClass, DummyTestClass>()

        val dummyObj = locator.build().locate<DummyTestClass>()

        assertEquals("DummyTestClass", dummyObj::class.simpleName)
    }

    @Test
    fun whenASingletonInterfaceIsLocated_AnImplementationIsReturned() {
        val locator = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerLazySingleton<DummyInterface, DummyImpl>()
            .build()

        val dummyObj = locator.locate<DummyInterface>()

        assertEquals("DummyImpl", dummyObj::class.simpleName)
    }

    @Test
    fun whenSingletonTwoClassCircularDepIsEncountered_AnExceptionIsThrown() {
        val builder = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerLazySingleton<CircularDep1, CircularDep1>()
            .registerLazySingleton<CircularDep2, CircularDep2>()

        assertThrows<CircularDependencyException> { builder.build() }
        assertThrows<CircularDependencyException> { builder.build() }
    }

    @Test
    fun whenOneClassCircularDepIsEncountered_AnExceptionIsThrown() {
        class CircularDep(val x: CircularDep)

        val builder = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerLazySingleton<CircularDep, CircularDep>()

        assertThrows<CircularDependencyException> { builder.build() }
    }

    @Test
    fun whenThreeClassCircularDependencyIsEncountered_AnExceptionIsThrown(){
        val builder = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerLazySingleton<CircularDepA, CircularDepA>()
            .registerLazySingleton<CircularDepB, CircularDepB>()
            .registerLazySingleton<CircularDepC, CircularDepC>()

        assertThrows<CircularDependencyException> { builder.build() }
        assertThrows<CircularDependencyException> { builder.build() }
        assertThrows<CircularDependencyException> { builder.build() }
    }

    @Test
    fun whenASingletonDependencyIsLocatedMultipleTimes_ItIsAlwaysTheSameObject(){
        val locator = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerLazySingleton<DummyInterface, DummyImpl>()
            .build()

        val obj1 = locator.locate<DummyInterface>()
        val obj2 = locator.locate<DummyInterface>()
        val obj3 = locator.locate<DummyInterface>()
        val obj4 = locator.locate<DummyInterface>()

        assertSame(obj1, obj2)
        assertSame(obj2, obj3)
        assertSame(obj3, obj4)
    }

    @Test
    fun whenARegisteredTransientClassIsLocated_NoExceptionIsThrown() {
        val locator = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerTransient<DummyTestClass, DummyTestClass>()
            .build()

        val dummyObj = locator.locate<DummyTestClass>()

        assertEquals("DummyTestClass", dummyObj::class.simpleName)
    }

    @Test
    fun whenATransientInterfaceIsLocated_AnImplementationIsReturned() {
        val locator = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerTransient<DummyInterface, DummyImpl>()
            .build()

        val dummyObj = locator.locate<DummyInterface>()

        assertEquals("DummyImpl", dummyObj::class.simpleName)
    }

    @Test
    fun whenTransientTwoClassCircularDepIsEncountered_AnExceptionIsThrown() {
        val builder = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerTransient<CircularDep1, CircularDep1>()
            .registerTransient<CircularDep2, CircularDep2>()

        assertThrows<CircularDependencyException> { builder.build() }
        assertThrows<CircularDependencyException> { builder.build() }
    }

    @Test
    fun whenTransientAndSingletonClassesCircularDepIsEncountered_AnExceptionIsThrown() {
        val builder = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerTransient<CircularDep1, CircularDep1>()
            .registerLazySingleton<CircularDep2, CircularDep2>()

        assertThrows<CircularDependencyException> { builder.build() }
        assertThrows<CircularDependencyException> { builder.build() }
    }

    @Test
    fun whenATransientDependencyIsLocatedMultipleTimes_ItIsAlwaysADifferentObject(){
        val locator = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerTransient<DummyInterface, DummyImpl>()
            .build()

        val obj1 = locator.locate<DummyInterface>()
        val obj2 = locator.locate<DummyInterface>()
        val obj3 = locator.locate<DummyInterface>()
        val obj4 = locator.locate<DummyInterface>()

        assertNotSame(obj1, obj2)
        assertNotSame(obj2, obj3)
        assertNotSame(obj3, obj4)
    }

    @Test
    fun whenATransientClassReferencesASingletonClass_NoErrorsOccur(){
        val locator = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerLazySingleton<SingletonDep, SingletonDep>()
            .registerTransient<TransientDep, TransientDep>()
            .build()

        val transient = locator.locate<TransientDep>()
        val transient2 = locator.locate<TransientDep>()
        val singleton = locator.locate<SingletonDep>()
        val singleton2 = locator.locate<SingletonDep>()

        assertSame(transient.x, singleton)
        assertNotSame(transient, transient2)
        assertSame(singleton, singleton2)
        assertEquals("SingletonDep", singleton::class.simpleName)
        assertEquals("TransientDep", transient::class.simpleName)
        assertEquals("SingletonDep", transient.x::class.simpleName)
    }

    @Test
    fun whenASingletonClassReferencesATriansientClass_NoErrorsOccur(){
        val locator = MinjeKtServiceLocatorBuilder(ServiceStore())
            .registerTransient<TransientDep2, TransientDep2>()
            .registerLazySingleton<SingletonDep2, SingletonDep2>()
            .build()

        val transient = locator.locate<TransientDep2>()
        val transient2 = locator.locate<TransientDep2>()
        val singleton = locator.locate<SingletonDep2>()
        val singleton2 = locator.locate<SingletonDep2>()

        assertSame(singleton, singleton2)

        // although singleton.x are transient, they are still the same because they are a part of a singleton
        assertSame(singleton.x, singleton2.x)
        assertNotSame(transient, transient2)
        assertEquals("SingletonDep2", singleton::class.simpleName)
        assertEquals("SingletonDep2", singleton2::class.simpleName)
        assertEquals("TransientDep2", transient::class.simpleName)
        assertEquals("TransientDep2", transient2::class.simpleName)
        assertEquals("TransientDep2", singleton.x::class.simpleName)
        assertEquals("TransientDep2", singleton2.x::class.simpleName)
    }
}