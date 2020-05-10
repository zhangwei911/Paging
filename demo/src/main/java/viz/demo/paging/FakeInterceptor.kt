package viz.demo.paging

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class FakeInterceptor(private val urlStr: String, private val respJson: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var response: Response? = null
        val url = chain.request().url.toUrl()
        val path = url.path
        val isFake = urlStr.contains(path) || urlStr.contains(url.toString()) || url.toString()
            .endsWith(urlStr) || url.toString().startsWith(urlStr)
        return if (isFake) {
            response = Response.Builder()
                .code(200)
                .message(respJson)
                .protocol(Protocol.HTTP_1_0)
                .request(chain.request())
                .body(
                    ResponseBody.create(
                        "application/json".toMediaTypeOrNull(),
                        respJson.toByteArray()
                    )
                )
                .addHeader("content-type", "application/json")
                .addHeader("charset", "UTF-8")
                .build()
            response
        } else {
            chain.proceed(chain.request())
        }
    }
}