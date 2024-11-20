package designPatterns

class ApiClient private constructor(
    var baseUrl:String,
    var connectionTimeOutInMillis: Long,
    var headers: Map<String,String>,
) {

    class Builder{
        private lateinit var baseUrl: String
        private var connectionTimeOutInMillis: Long = 500
        private lateinit var headers: Map<String,String>

        fun baseUrl(baseUrl: String) = apply {
            this.baseUrl = baseUrl
        }

        fun connectionTimeOutInMillis(connectionTimeOutInMillis: Long) = apply {
            this.connectionTimeOutInMillis = connectionTimeOutInMillis
        }

        fun headers(headers: Map<String, String>) = apply {
            this.headers = headers
        }

        fun build(): ApiClient {
            return ApiClient(
                baseUrl = this.baseUrl,
                connectionTimeOutInMillis = this.connectionTimeOutInMillis,
                headers = this.headers
            )
        }

    }
    companion object {

    }
}

fun main() {
    val client = ApiClient.Builder().baseUrl("baseurl").headers(mapOf("authorization" to "1234")).build()
    println(client.connectionTimeOutInMillis)
    println(client.headers)
}