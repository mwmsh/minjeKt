package io.github.mwmsh.minjekt.cycles

import io.github.mwmsh.minjekt.exception.CircularDependencyException
import io.github.mwmsh.minjekt.exception.ConstructorIsNotAccessibleException
import io.github.mwmsh.minjekt.exception.DependencyNotRegisteredException
import io.github.mwmsh.minjekt.exception.PrimaryConstructorNotFoundException
import io.github.mwmsh.minjekt.store.ServiceStore

class ServiceGraphSanityChecker(val store: ServiceStore) {
    fun ensureSaneServiceGraphOrThrowErrors() {
        val allServices = store.getAllServices()

        for (service in allServices) {
            if (!service.hasRegisteredImplementation()) {
                throw DependencyNotRegisteredException("No registered implementation for ${service.getServiceClass()}")
            }

            if (!service.impl.hasPrimaryConstructor()) {
                throw PrimaryConstructorNotFoundException("Could not locate primary constructor for type ${service.impl.getImplClass()}")
            }

            if (!service.impl.isPrimaryConstructorPublic()) {
                throw ConstructorIsNotAccessibleException("Primary constructor for type ${service.impl.getImplClass()} is not public")
            }

            if (!service.isServiceConstructable()) {
                throw CircularDependencyException("Could not initialize service: ${service.getServiceClass()}, implementation: ${service.impl.getImplClass()}. This is likely due to a circular dependency")
            }
        }
    }
}