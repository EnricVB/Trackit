package dev.enric.core.handler.users

import dev.enric.core.objects.User

class UserCreationHandler(
    val name: String,
    val password: String,
    val mail: String?,
    val phone: String?,
    val roles: Array<String>,
    val reassignPassword: Boolean,
    val sudo: User?
) {
}