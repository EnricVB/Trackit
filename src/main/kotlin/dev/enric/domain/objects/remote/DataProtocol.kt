package dev.enric.domain.objects.remote

import java.io.Serializable

enum class DataProtocol(
    var user: String? = null,
    var host: String? = null,
    var port: Int? = null,
    var path: String? = null
) : Serializable {

    SSH,
    LOCAL;

    override fun toString(): String {
        return when (this) {
            SSH -> "SSH://$user@$host:$port/$path"
            LOCAL -> "LOCAL://$user:/$path"
        }
    }

    companion object Factory {
        @JvmStatic
        fun newSSHInstance(user: String, host: String, port: Int, path: String): DataProtocol {
            return SSH.apply {
                this.user = user
                this.host = host
                this.port = port
                this.path = path
            }
        }

        @JvmStatic
        fun newLocalInstance(user: String, path: String): DataProtocol {
            return LOCAL.apply {
                this.user = user
                this.path = path
            }
        }
    }
}
