import com.minjeKt.exception.CircularDependencyException
import com.minjeKt.exception.DependencyNotRegisteredException
import com.minjeKt.locator.MinjeKtServiceLocator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

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

class MinjeKtServiceLocatorTests {

    @Test
    fun whenAnUnregisteredClassIsLocated_AnExceptionIsThrown() {
        assertThrows<DependencyNotRegisteredException> {
            MinjeKtServiceLocator().locate<DummyTestClass>()
        }
    }

    @Test
    fun whenARegisteredClassIsLocated_NoExceptionIsThrown() {
        val locator = MinjeKtServiceLocator()
            .registerLazySingleton<DummyTestClass, DummyTestClass>()

        val dummyObj = locator.locate<DummyTestClass>()

        assertEquals("DummyTestClass", dummyObj::class.simpleName)
    }

    @Test
    fun whenAnInterfaceIsLocated_AnImplementationIsReturned() {
        val locator = MinjeKtServiceLocator()
            .registerLazySingleton<DummyInterface, DummyImpl>()

        val dummyObj = locator.locate<DummyInterface>()

        assertEquals("DummyImpl", dummyObj::class.simpleName)
    }

    @Test
    fun whenTwoClassCircularDepIsEncountered_AnExceptionIsThrown() {
        val locator = MinjeKtServiceLocator()
            .registerLazySingleton<CircularDep1, CircularDep1>()
            .registerLazySingleton<CircularDep2, CircularDep2>()

        assertThrows<CircularDependencyException> { locator.locate<CircularDep1>() }
        assertThrows<CircularDependencyException> { locator.locate<CircularDep2>() }
    }

    @Test
    fun whenOneClassCircularDepIsEncountered_AnExceptionIsThrown() {
        class CircularDep(val x: CircularDep)

        val locator = MinjeKtServiceLocator()
            .registerLazySingleton<CircularDep, CircularDep>()

        assertThrows<CircularDependencyException> { locator.locate<CircularDep>() }
    }

    @Test
    fun whenThreeClassCircularDependencyIsEncountered_AnExceptionIsThrown(){
        val locator = MinjeKtServiceLocator()
            .registerLazySingleton<CircularDepA, CircularDepA>()
            .registerLazySingleton<CircularDepB, CircularDepB>()
            .registerLazySingleton<CircularDepC, CircularDepC>()

        assertThrows<CircularDependencyException> { locator.locate<CircularDepA>() }
        assertThrows<CircularDependencyException> { locator.locate<CircularDepB>() }
        assertThrows<CircularDependencyException> { locator.locate<CircularDepC>() }
    }
}