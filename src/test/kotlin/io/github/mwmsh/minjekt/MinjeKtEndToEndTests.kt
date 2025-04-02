package io.github.mwmsh.minjekt

import io.github.mwmsh.minjekt.exception.DependencyNotRegisteredException
import io.github.mwmsh.minjekt.locator.MinjeKt
import io.github.mwmsh.minjekt.locator.MinjeKtBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface IAlpha {
    val id: Int
    fun doWork(): String
}

class AlphaImpl : IAlpha {
    override val id = System.identityHashCode(this)
    override fun doWork() = "AlphaImpl working..."
}

interface IBeta {
    val id: Int
    fun doWork(): String
}

class BetaImpl(private val alpha: IAlpha) : IBeta {
    override val id = System.identityHashCode(this)
    override fun doWork() = "BetaImpl working with -> ${alpha.doWork()}"
}

interface IGamma {
    val id: Int
    fun doWork(): String
}

class GammaImpl(
    private val alpha: IAlpha,
    private val beta: IBeta
) : IGamma {
    override val id = System.identityHashCode(this)
    override fun doWork() =
        "GammaImpl combining -> [${alpha.doWork()}] & [${beta.doWork()}]"
}

interface IDelta {
    val id: Int
    fun doWork(): String
}

class DeltaImpl : IDelta {
    override val id = System.identityHashCode(this)
    override fun doWork() = "DeltaImpl working..."
}

/**
 * Epsilon just for variety. You can make as many as you like.
 */
interface IEpsilon {
    val id: Int
    fun doWork(): String
}

class EpsilonImpl : IEpsilon {
    override val id = System.identityHashCode(this)
    override fun doWork() = "EpsilonImpl working..."
}

/**
 * Zeta depends on a bunch of others for a more complex injection scenario.
 */
interface IZeta {
    val id: Int
    fun doWork(): String
}

class ZetaImpl(
    private val delta: IDelta,
    private val epsilon: IEpsilon,
    private val gamma: IGamma
) : IZeta {
    override val id = System.identityHashCode(this)
    override fun doWork(): String {
        return "ZetaImpl combining -> [${delta.doWork()}] & [${epsilon.doWork()}] & [${gamma.doWork()}]"
    }
}


class MinjeKtE2ELargeTest {

    @Test
    fun `test multiple interfaces in large end-to-end scenario`() {
        // 1) Build the locator with many registrations
        val locator: MinjeKt = MinjeKtBuilder.create()
            // Singletons
            .registerSingleton<IAlpha, AlphaImpl>()
            .registerSingleton<IDelta, DeltaImpl>()

            // Lazy singletons
            .registerLazySingleton<IBeta, BetaImpl>()
            .registerLazySingleton<IZeta, ZetaImpl>()

            // Transients
            .registerTransient<IGamma, GammaImpl>()
            .registerTransient<IEpsilon, EpsilonImpl>()

            .build()

        // 2) Validate basic retrieval
        val alpha1 = locator.locate<IAlpha>()
        assertNotNull(alpha1, "Should retrieve an instance of IAlpha")

        val beta1 = locator.locate<IBeta>()
        assertNotNull(beta1, "Should retrieve an instance of IBeta")

        val gamma1 = locator.locate<IGamma>()
        assertNotNull(gamma1, "Should retrieve an instance of IGamma")

        val delta1 = locator.locate<IDelta>()
        assertNotNull(delta1, "Should retrieve an instance of IDelta")

        val epsilon1 = locator.locate<IEpsilon>()
        assertNotNull(epsilon1, "Should retrieve an instance of IEpsilon")

        val zeta1 = locator.locate<IZeta>()
        assertNotNull(zeta1, "Should retrieve an instance of IZeta")

        // 3) Check method calls to confirm they're functional
        assertTrue(alpha1.doWork().contains("AlphaImpl"))
        assertTrue(beta1.doWork().contains("BetaImpl"))
        assertTrue(gamma1.doWork().contains("GammaImpl"))
        assertTrue(delta1.doWork().contains("DeltaImpl"))
        assertTrue(epsilon1.doWork().contains("EpsilonImpl"))
        assertTrue(zeta1.doWork().contains("ZetaImpl"))

        // 4) Check lifetime behavior in loops

        // --- IAlpha is a singleton ---
        val alpha2 = locator.locate<IAlpha>()
        assertSame(alpha1, alpha2, "IAlpha is singleton -> must be the same instance.")
        // Also IDs must match
        assertEquals(alpha1.id, alpha2.id)

        // --- IBeta is a lazy singleton -> after first creation, it must always be the same
        val beta2 = locator.locate<IBeta>()
        assertSame(beta1, beta2, "IBeta is lazy singleton -> must be same instance once initialized.")
        assertEquals(beta1.id, beta2.id)

        // --- IGamma is transient -> each retrieval should be new
        val gamma2 = locator.locate<IGamma>()
        assertNotSame(gamma1, gamma2, "IGamma is transient -> must be different each time.")
        assertNotEquals(gamma1.id, gamma2.id)

        // --- IDelta is singleton
        val delta2 = locator.locate<IDelta>()
        assertSame(delta1, delta2, "IDelta is singleton -> must be the same instance.")
        assertEquals(delta1.id, delta2.id)

        // --- IEpsilon is transient
        val epsilon2 = locator.locate<IEpsilon>()
        assertNotSame(epsilon1, epsilon2, "IEpsilon is transient -> must be different each time.")
        assertNotEquals(epsilon1.id, epsilon2.id)

        // --- IZeta is lazy singleton
        val zeta2 = locator.locate<IZeta>()
        assertSame(zeta1, zeta2, "IZeta is lazy singleton -> must be the same instance once created.")
        assertEquals(zeta1.id, zeta2.id)

        // 5) Bulk test in loops
        repeat(50) {
            // All singletons must remain the same
            assertSame(alpha1, locator.locate<IAlpha>())
            assertSame(delta1, locator.locate<IDelta>())
            assertSame(beta1, locator.locate<IBeta>())
            assertSame(zeta1, locator.locate<IZeta>())

            // All transients must be new
            val newGamma = locator.locate<IGamma>()
            assertNotSame(gamma1, newGamma)
            assertNotSame(gamma2, newGamma)

            val newEpsilon = locator.locate<IEpsilon>()
            assertNotSame(epsilon1, newEpsilon)
            assertNotSame(epsilon2, newEpsilon)
        }

        // 6) Optional concurrency test (if your library claims thread-safety)
        concurrencyTest(locator)

        // 7) (Optional) Try to locate something not registered
        assertThrows<DependencyNotRegisteredException> {
            locator.locate<String>() // Not registered
        }
    }

    /**
     * Demonstrates a simple concurrency test if your service locator is thread-safe.
     * (You can expand or remove this as needed.)
     */
    private fun concurrencyTest(locator: MinjeKt) {
        val numThreads = 8
        val iterationsPerThread = 50
        val latch = CountDownLatch(numThreads)
        val executor = Executors.newFixedThreadPool(numThreads)

        val alphaRef = locator.locate<IAlpha>() // this should always be the same

        repeat(numThreads) {
            executor.execute {
                repeat(iterationsPerThread) {
                    // Singleton check
                    val alphaLocal = locator.locate<IAlpha>()
                    assertSame(alphaRef, alphaLocal, "IAlpha must remain same in concurrency scenario.")

                    // Transient check
                    val gammaLocal1 = locator.locate<IGamma>()
                    val gammaLocal2 = locator.locate<IGamma>()
                    assertNotSame(gammaLocal1, gammaLocal2, "IGamma must be new each time, concurrency check.")
                }
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()
        assertTrue(executor.isShutdown, "Executor should have shutdown.")
    }

    @Test
    fun `expanded concurrency test`() {
        val locator = MinjeKtBuilder.create()
            .registerSingleton<IAlpha, AlphaImpl>()
            .registerLazySingleton<IBeta, BetaImpl>()
            .registerTransient<IGamma, GammaImpl>()
            .build()

        concurrencyTestSingleton(locator)
        concurrencyTestLazySingleton(locator)
        concurrencyTestTransient(locator)
    }


    private fun concurrencyTestSingleton(locator: MinjeKt) {
        val concurrencyLevel = 20
        val iterationsPerThread = 30
        val executor = Executors.newFixedThreadPool(concurrencyLevel)
        val latch = CountDownLatch(concurrencyLevel)

        // We'll capture the alpha instances we get in each thread to compare them
        val results = Collections.synchronizedList(mutableListOf<IAlpha>())

        // Grab a "reference" outside to compare
        val alphaRef = locator.locate<IAlpha>()

        repeat(concurrencyLevel) {
            executor.execute {
                repeat(iterationsPerThread) {
                    val instance = locator.locate<IAlpha>()
                    // Must always be the same reference
                    assertSame(alphaRef, instance, "Singleton must be consistent across threads.")
                    results.add(instance)
                }
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // Confirm we actually got all the calls
        val totalCalls = concurrencyLevel * iterationsPerThread
        assertEquals(totalCalls, results.size, "Should have correct number of results recorded.")

        // Now verify all references in results are the same
        val distinctInstances = results.toSet()
        assertEquals(1, distinctInstances.size, "All references should be identical for a singleton.")
    }

    private fun concurrencyTestLazySingleton(locator: MinjeKt) {
        val concurrencyLevel = 20
        val iterationsPerThread = 30
        val executor = Executors.newFixedThreadPool(concurrencyLevel)
        val latch = CountDownLatch(concurrencyLevel)

        // We'll store results from each thread
        val results = Collections.synchronizedList(mutableListOf<IBeta>())

        repeat(concurrencyLevel) {
            executor.execute {
                repeat(iterationsPerThread) {
                    // The first call across all threads may race to create the lazy instance
                    val instance = locator.locate<IBeta>()
                    results.add(instance)
                }
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        val totalCalls = concurrencyLevel * iterationsPerThread
        assertEquals(totalCalls, results.size)

        // They should all be the same single instance
        val distinctInstances = results.toSet()
        assertEquals(1, distinctInstances.size, "All references should be identical for a lazy singleton.")
    }

    private fun concurrencyTestTransient(locator: MinjeKt) {
        val concurrencyLevel = 20
        val iterationsPerThread = 30
        val executor = Executors.newFixedThreadPool(concurrencyLevel)
        val latch = CountDownLatch(concurrencyLevel)

        val results = Collections.synchronizedList(mutableListOf<IGamma>())

        repeat(concurrencyLevel) {
            executor.execute {
                repeat(iterationsPerThread) {
                    val instance = locator.locate<IGamma>()
                    results.add(instance)
                }
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // We expect every single call to yield a unique instance
        val totalCalls = concurrencyLevel * iterationsPerThread
        assertEquals(totalCalls, results.size)

        // The set of all returned references should be the same size,
        // indicating no duplicates were reused
        val distinctInstances = results.toSet()
        assertEquals(totalCalls, distinctInstances.size,
            "All references should be distinct for a transient under concurrency.")
    }


}
