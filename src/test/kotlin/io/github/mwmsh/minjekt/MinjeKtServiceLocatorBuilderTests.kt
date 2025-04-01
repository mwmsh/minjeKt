package io.github.mwmsh.minjekt

import io.github.mwmsh.minjekt.exception.CircularDependencyException
import io.github.mwmsh.minjekt.exception.ConstructorIsNotAccessibleException
import io.github.mwmsh.minjekt.exception.DependencyNotRegisteredException
import io.github.mwmsh.minjekt.exception.PrimaryConstructorNotFoundException
import org.junit.jupiter.api.assertThrows
import org.minjekt.locator.MinjeKtServiceLocator
import org.minjekt.locator.MinjeKtServiceLocatorBuilder
import kotlin.test.*


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

class SingletonDep() {}
class TransientDep(val x: SingletonDep) {}

class TransientDep2()
class SingletonDep2(val x: TransientDep2)

class EagerSingletonDep(val x: LazySingletonDep)
class LazySingletonDep(val x: TransientDepX)
class TransientDepX(val x: EagerSingletonDep)

class ClassWithPrivateConstructor private constructor() {}


interface A {}

class AImpl: A {}

interface B{}

class BImpl(val a: A): B{}

interface C{}

class CImpl(a: A, b: B): C {}

class MinjeKtServiceLocatorBuilderTests {
    @Test
    fun whenAnUnregisteredClassIsLocated_AnExceptionIsThrown() {
        assertThrows<DependencyNotRegisteredException> {
            MinjeKtServiceLocatorBuilder.create().build().locate<DummyTestClass>()
        }
    }

    @Test
    fun whenARegisteredSingletonClassIsLocated_NoExceptionIsThrown() {
        val locator = MinjeKtServiceLocatorBuilder.create()
            .registerLazySingleton<DummyTestClass, DummyTestClass>()

        val dummyObj = locator.build().locate<DummyTestClass>()

        assertEquals("DummyTestClass", dummyObj::class.simpleName)
    }

    @Test
    fun whenASingletonInterfaceIsLocated_AnImplementationIsReturned() {
        val locator = MinjeKtServiceLocatorBuilder.create()
            .registerLazySingleton<DummyInterface, DummyImpl>()
            .build()

        val dummyObj = locator.locate<DummyInterface>()

        assertEquals("DummyImpl", dummyObj::class.simpleName)
    }

    @Test
    fun whenSingletonTwoClassCircularDepIsEncountered_AnExceptionIsThrown() {
        val builder = MinjeKtServiceLocatorBuilder.create()
            .registerLazySingleton<CircularDep1, CircularDep1>()
            .registerLazySingleton<CircularDep2, CircularDep2>()

        assertThrows<CircularDependencyException> { builder.build() }
        assertThrows<CircularDependencyException> { builder.build() }
    }

    @Test
    fun whenOneClassCircularDepIsEncountered_AnExceptionIsThrown() {
        class CircularDep(val x: CircularDep)

        val builder = MinjeKtServiceLocatorBuilder.create()
            .registerLazySingleton<CircularDep, CircularDep>()

        assertThrows<CircularDependencyException> { builder.build() }
    }

    @Test
    fun whenThreeClassCircularDependencyIsEncountered_AnExceptionIsThrown() {
        val builder = MinjeKtServiceLocatorBuilder.create()
            .registerLazySingleton<CircularDepA, CircularDepA>()
            .registerLazySingleton<CircularDepB, CircularDepB>()
            .registerLazySingleton<CircularDepC, CircularDepC>()

        assertThrows<CircularDependencyException> { builder.build() }
        assertThrows<CircularDependencyException> { builder.build() }
        assertThrows<CircularDependencyException> { builder.build() }
    }

    @Test
    fun whenASingletonDependencyIsLocatedMultipleTimes_ItIsAlwaysTheSameObject() {
        val locator = MinjeKtServiceLocatorBuilder.create()
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
        val locator = MinjeKtServiceLocatorBuilder.create()
            .registerTransient<DummyTestClass, DummyTestClass>()
            .build()

        val dummyObj = locator.locate<DummyTestClass>()

        assertEquals("DummyTestClass", dummyObj::class.simpleName)
    }

    @Test
    fun whenATransientInterfaceIsLocated_AnImplementationIsReturned() {
        val locator = MinjeKtServiceLocatorBuilder.create()
            .registerTransient<DummyInterface, DummyImpl>()
            .build()

        val dummyObj = locator.locate<DummyInterface>()

        assertEquals("DummyImpl", dummyObj::class.simpleName)
    }

    @Test
    fun whenTransientTwoClassCircularDepIsEncountered_AnExceptionIsThrown() {
        val builder = MinjeKtServiceLocatorBuilder.create()
            .registerTransient<CircularDep1, CircularDep1>()
            .registerTransient<CircularDep2, CircularDep2>()

        assertThrows<CircularDependencyException> { builder.build() }
        assertThrows<CircularDependencyException> { builder.build() }
    }

    @Test
    fun whenTransientAndSingletonClassesCircularDepIsEncountered_AnExceptionIsThrown() {
        val builder = MinjeKtServiceLocatorBuilder.create()
            .registerTransient<CircularDep1, CircularDep1>()
            .registerLazySingleton<CircularDep2, CircularDep2>()

        assertThrows<CircularDependencyException> { builder.build() }
        assertThrows<CircularDependencyException> { builder.build() }
    }

    @Test
    fun whenATransientDependencyIsLocatedMultipleTimes_ItIsAlwaysADifferentObject() {
        val locator = MinjeKtServiceLocatorBuilder.create()
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
    fun whenATransientClassReferencesASingletonClass_NoErrorsOccur() {
        val locator = MinjeKtServiceLocatorBuilder.create()
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
    fun whenASingletonClassReferencesATransientClass_NoErrorsOccur() {
        val locator = MinjeKtServiceLocatorBuilder.create()
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

    @Test
    fun whenAnEagerSingletonServiceIsRegistered_ItIsLocatedSuccessfully() {
        val locator = MinjeKtServiceLocatorBuilder.create()
            .registerSingleton<DummyInterface, DummyImpl>()
            .build()

        val singleton1: DummyInterface = locator.locate<DummyInterface>()
        val singleton2: DummyInterface = locator.locate<DummyInterface>()

        assertSame(singleton1, singleton2)
        assertEquals("DummyImpl", singleton1::class.simpleName)
    }

    @Test
    fun whenADependencyCycleContainingEagerSingletonLazySingletonTransientOccurs_ItThrowsAnError() {
        val builder = MinjeKtServiceLocatorBuilder.create()
            .registerTransient<TransientDepX, TransientDepX>()
            .registerLazySingleton<LazySingletonDep, LazySingletonDep>()
            .registerSingleton<EagerSingletonDep, EagerSingletonDep>()

        assertThrows<CircularDependencyException> {
            builder.build()
        }
    }

    @Test
    fun whenAnEagerSingletonInstanceIsRegistered_ItIsLocatedSuccessfully() {
        val impl = DummyImpl()
        val locator = MinjeKtServiceLocatorBuilder.create()
            .registerSingleton<DummyInterface>(impl)
            .build()

        val located = locator.locate<DummyInterface>()

        assertSame(impl, located)
    }

    @Test
    fun whenALazySingletonInstanceIsRegistered_ItIsLocatedSuccessfully() {
        val impl = DummyImpl()
        val locator = MinjeKtServiceLocatorBuilder.create()
            .registerLazySingleton<DummyInterface>(impl)
            .build()

        val located = locator.locate<DummyInterface>()

        assertSame(impl, located)
    }

    @Test
    fun whenATypeWithNoPrimaryConstructorIsRegistered_AnExceptionIsThrown() {
        val builder = MinjeKtServiceLocatorBuilder.create()
            .registerSingleton<DummyInterface, DummyInterface>()

        assertThrows<PrimaryConstructorNotFoundException> {
            builder.build()
        }
    }

    @Test
    fun whenAClassWithAPrivateConstructorIsRegistered_AnExceptionIsThrown() {
        val builder = MinjeKtServiceLocatorBuilder.create()
            .registerSingleton<ClassWithPrivateConstructor, ClassWithPrivateConstructor>()

        assertThrows<ConstructorIsNotAccessibleException> {
            builder.build()
        }
    }

    @Test
    fun whenAClassWithADefaultParameterIsRegistered_DefaultParametersAreRespected() {
        class DefClass(val param: List<String> = listOf("hello"))

        val locator = MinjeKtServiceLocatorBuilder.create()
            .registerSingleton<DefClass, DefClass>()
            .build()

        val result: DefClass = locator.locate<DefClass>()

        assertEquals(result.param, listOf("hello"))
    }

    @Test
    fun whenAClassWithDefaultAndNonDefaultParametersIsRegistered_DefaultParametersAreRespected() {
        class TestClass1(val param: Set<String> = setOf("test class1")) {}
        class TestClass2 {}

        class DefClass(
            val class1: TestClass1,
            val param: List<String> = listOf("hello", "world"),
            val class2: TestClass2
        )

        val locator: MinjeKtServiceLocator = MinjeKtServiceLocatorBuilder.create()
            .registerSingleton<TestClass1, TestClass1>()
            .registerSingleton<TestClass2, TestClass2>()
            .registerSingleton<DefClass, DefClass>()
            .build()

        val obj = locator.locate<DefClass>()
        assertEquals(obj.param, listOf("hello", "world"))
        assertEquals(obj.class1.param, setOf("test class1"))
    }

    @Test
    fun whenABunchOfClassesHaveNoCycles_NoErrorsShouldOccur(){
        val locator = MinjeKtServiceLocatorBuilder.create()
            .registerSingleton<A, AImpl>()
            .registerSingleton<B, BImpl>()
            .registerSingleton<C, CImpl>()
            .build()


        val a = locator.locate<A>()
        val b = locator.locate<B>()

        assertEquals("AImpl", a::class.simpleName)
        assertEquals("BImpl", b::class.simpleName)
    }

    @Ignore
    fun whenATemplateClassIsRegistered_ItIsLocatedSuccessfully() {
        class Box<T>(val item: T) {
            fun show() = item.toString()
        }

        val locator = MinjeKtServiceLocatorBuilder.create()
            .registerSingleton<Box<String>>(Box("Hello!"))
            .registerLazySingleton<Box<Int>>(Box(42))
            .registerSingleton<Box<Boolean>>(Box(true))
            .registerLazySingleton<Box<Box<Int>>>(Box(Box(123)))
            .build()

        val boxOfString = locator.locate<Box<String>>()
        val boxOfInt = locator.locate<Box<Int>>()
        val boxOfBoolean = locator.locate<Box<Boolean>>()
        val boxOfBoxOfInt = locator.locate<Box<Box<Int>>>()

        assertEquals("Hello!", boxOfString.item)
        assertEquals(42, boxOfInt.item)
        assertEquals(true, boxOfBoolean.item)
        assertEquals(123, boxOfBoxOfInt.item.item)
    }

}