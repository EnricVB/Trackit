package dev.enric.domain.objects.remote

import java.io.Serializable

enum class DataProtocol(
    var user: String? = null,
    var password: String? = null,
    var host: String? = null,
    var port: Int? = null,
    var path: String? = null
) : Serializable {

    TRACKIT;

    override fun toString(): String {
        return when (this) {
            TRACKIT -> "trackit://$user:$password@$host:$port/$path"
        }
    }

    companion object Factory {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        fun newTrackitInstance(user: String, password: String, host: String, port: Int, path: String): DataProtocol {
            return TRACKIT.apply {
                this.user = user
                this.password = password
                this.host = host
                this.port = port
                this.path = path
            }
        }

        @JvmStatic
        fun validateRequest(request: String): MatchResult?  {
            val regex = """(.+)://(.+):(.+)@(.+):(.+)/(.*)""".toRegex()
            return regex.matchEntire(request)
        }

        @JvmStatic
        fun toDataProtocol(matchResult: MatchResult): DataProtocol {
            val (_, user, password, host, port, path) = matchResult.destructured
            return newTrackitInstance(user, password, host, port.toInt(), path)
        }
    }
}
