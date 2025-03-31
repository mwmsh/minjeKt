package io.github.mwmsh.minjekt.cycles

import io.github.mwmsh.minjekt.exception.CircularDependencyException
import io.github.mwmsh.minjekt.exception.DependencyNotRegisteredException
import io.github.mwmsh.minjekt.exception.PrimaryConstructorNotFoundException
import io.github.mwmsh.minjekt.store.ServiceStore
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

// Traversing the whole dependency tree to look for potential cycles and display proper errors (instead of cryptic
// stack overflows is expensive and does not happen often.
// This could either be:
// 1- Done once for the whole dependency graph
// 2- Done multiple times for different objects with caching to avoid repeated work
// 3- Allow users to completely disable it to save time (or allow them to opt in for expensive logging/errors)
// 4- Completely disable it and catch and rethrow stack overflow
// 5- Any combination of the above

class CycleDetector {
    companion object {

        //O(n^2), can be linearized with caching
        fun ensureSaneDependencyGraph(store: ServiceStore) {
            store.getAllServices().forEach { service -> ensureSaneDependencyGraph(service, store) }
        }

        private fun ensureSaneDependencyGraph(service: KClass<*>, store: ServiceStore) {
            val visited = HashSet<KClass<*>>()

            val stack = Stack<KClass<*>>()
            stack.push(service)

            while (!stack.empty()) {
                val curr = stack.pop()

                if (visited.contains(curr)) {
                    throw CircularDependencyException("Could not initialize ${curr::class}. This is likely due to a circular dependency")
                }

                visited.add(curr)
                val implClass = store.findServiceImpl(curr)

                if (implClass == null) {
                    throw DependencyNotRegisteredException("No registered service for ${curr::class.qualifiedName}")
                }

                val primaryConstructor = implClass.primaryConstructor

                if (primaryConstructor == null) {
                    throw PrimaryConstructorNotFoundException("Could not locate primary constructor for type $implClass")
                }

                val children = primaryConstructor.parameters.map { it.type.classifier as KClass<*> }

                stack.addAll(children)
            }
        }
    }
}