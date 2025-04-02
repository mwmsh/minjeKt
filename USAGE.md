## Full code example

```kotlin
package io.github.mwmsh.minjekt.example

import io.github.mwmsh.minjekt.locator.MinjeKtBuilder
import java.util.*

interface Database {
    fun userExists(username: String, password: String): Boolean
}

data class User(val username: String, val password: String)

class LocalDB(val users: List<User> = listOf(User("UserA", "p@ssw0rd"), User("UserB", "123456789"))): Database {
    override fun userExists(username: String, password: String): Boolean {
        return users.any{it.username == username && it.password == password}
    }
}

class UserDao(val db: Database) {
    fun userExists(username: String, password: String): Boolean {
        return db.userExists(username, password)
    }
}

interface ITokenGenerator {
    fun generate(username: String, password: String): String
}

class DummyTokenGeneratorImpl: ITokenGenerator {
    override fun generate(username: String, password: String): String {
        return UUID.randomUUID().toString()
    }
}

data class LoginResult(val success: Boolean, val token: String?)

class UsersController(val dao: UserDao, val tokenGenerator: ITokenGenerator) {
    fun login(username: String, password: String): LoginResult {
        if(dao.userExists(username, password)){
            return LoginResult(true, tokenGenerator.generate(username, password))
        }

        return LoginResult(false, "Error occurred :(( ")
    }
}

interface IClient {
    fun login(username: String, password: String): LoginResult
}


class DummyClient(val controller: UsersController): IClient {
    override fun login(username: String, password: String): LoginResult {
        return controller.login(username, password)
    }
}

fun main() {
    val minjekt = MinjeKtBuilder.create()
        .registerLazySingleton<Database, LocalDB>()
        .registerTransient<UserDao, UserDao>()
        .registerTransient<UsersController, UsersController>()
        .registerTransient<ITokenGenerator, DummyTokenGeneratorImpl>()
        .registerSingleton<IClient, DummyClient>()
        .build()

    val client = minjekt.locate<IClient>()
    println(client.login("UserA", "password"))
    println(client.login("userA", "p@ssw0rd"))
}

```
The output for this code is
```text
LoginResult(success=false, token=Error occurred :(( )
LoginResult(success=true, token=fb0f3077-836b-4585-ad59-0ed7a7e60fca)
```
