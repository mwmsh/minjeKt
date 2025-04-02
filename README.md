# MinjeKt
MinjeKt is a pure Kotlin, thread-safe dependency injection micro-framework with zero dependencies. It's designed to be minimal, readable and _very_ easy to learn and use (no annotations!).

The steps and code below are all you need to let MinjeKt manage your dependencies. Get started in less than 5 minutes!

## ⭐ Support MinjeKt
If you find MinjeKt useful, please give it a star on GitHub! Your support helps the project grow and reach more developers.

## Quick start
#### 1- Add MinjeKt to your packages

##### Gradle
```groovy
implementation 'io.github.mwmsh.minjekt:minjekt:0.1.1'
```

##### Maven
```xml
<dependency>
    <groupId>io.github.mwmsh.minjekt</groupId>
    <artifactId>minjekt</artifactId>
    <version>0.1.1</version>
</dependency>

```

#### 2- Register your dependencies
You can register your dependencies like this example

```kotlin
@Test
fun localIntegTest(){
    val minject = MinjektServiceLocatorBuilder.create()
        .registerSingleton<UserDao, UserDao>()
        .registerLazySingleton<Controller, LoginController>()
        .registerTransient<Database, MockDatabaseImpl>()
        .registerSingleton<Client>(LocalClient())
        .build()

    val client = minject.locate<Client>()
    val result = client.login("testUser", "testPassword")
    
    assertTrue(result.success)
}
```

## Why MinjeKt?
### Yet another way to do dependency injection in Kotlin?
Aaah, yes.

I built MinjeKt because I needed a dependency injection solution but found existing Kotlin DI frameworks too complicated and overkill for small to medium projects. Some of these tools are also inherited from java and are clunky to use with Kotlin.

In more specific terms, MinjeKt is well-suited (and not limited to):

#### 💡 Small and medium-sized projects

You want to write **maintainable**, **readable**, **extensible** and **performant** code while keeping your project simple and your dependencies minimal

#### 📦 Libraries

You are building a library, and you want **lightweight dependency injection** without forcing your users to live with your choice.

#### 🧪 Testing (unit, integration, end-to-end)
MinjeKt makes testing easy by allowing **quick swaps between real implementations and mocks**. This keeps tests modular and flexible without complex setup.

#### 📱 Small Android apps and prototypes
Where libraries like Hilt could be overkill.

#### 🛠️ Microservices or CLI tools
Where simplicity and minimal dependencies are often necessary.


## Performance Considerations
While I cannot make concrete claims in terms of performance yet, MinjeKt uses reflection only when it is necessary. Namely when traversing the service graph during `build()` phase and when constructing **lazy singletons** and **transients** during runtime.

I will publish more details here once I have the numbers. Please let me know if you are interested in performance in particular scenarios in [this thread](https://github.com/mwmsh/minjeKt/issues/1).

 
## Registering dependencies
MinjeKt offers three main ways to register dependencies: singletons, lazy singletons and transients.

**Singletons** are eagerly constructed when the build() method is invoked
```kotlin
 val minjekt = MinjeKtServiceLocatorBuilder.create()
.registerSingleton<Interface, InterfaceImpl>()
.registerSingleton<ClassImpl, ClassImpl>()
.registerSingleton<Interface2>(Interface2Impl())
.build() //This is when the singletons are validated and constructed

val instance = minjekt.locate<ClassImpl>()
```

**Lazy Singletons** are constructed lazily whenever they are first requested
```kotlin
val minjekt = MinjektServiceLocatorBuilder.create()
.registerLazySingleton<Interface, InterfaceImpl>()
.registerLazySingleton<ClassImpl, ClassImpl>()
.build() //Only validation of lazy singletons happens at this point


val instance = minjekt.locate<Interface>() //construction of lazy singletons 
// happens here (in addition to transient/lazy singleton dependencies) 
```

**Transients** are constructed whenever they are requested
```kotlin
val minjekt = MinjeKtServiceLocatorBuilder.create()
    .registerTransient<TransientClass, TransientClass>()
    .registerTransient<Interface, InterfaceImpl>()
    .build() //Only validation of transients happens here

val instance = minjekt.locate<Interface>()
```

## Validation and errors
MinjeKt does validation during `build()`. In general, it checks that: 
1) All registered services (and their dependencies) have registered implementations
2) All implementations have constructors
3) All their constructors are public

The `build()` method is also when MinjeKt verifies that the registered services can be constructed by ensuring the
service graph has no circular dependencies.

If any of the above conditions are not met, exceptions are thrown during `build()`. You can find the list of exceptions here. The names are self-documenting.

## Examples
Looking for more code examples? Check out the [full working example](USAGE.md)

## FAQ
You can find the frequently asked questions [here](FAQ.md). 

## ❤️ Enjoying MinjeKt?
If MinjeKt made your development smoother, don’t forget to star the project!  

