package com.minjeKt.exception

class CircularDependencyException(message: String) : Exception(message)
class DependencyNotRegisteredException(message: String) : Exception(message)
class PrimaryConstructorNotFoundException(message: String) : Exception(message)
class ConstructorIsNotAccessibleException(message: String) : Exception(message)