package viz.demo.paging

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import viz.demo.paging.bean.ResultBean
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.random.Random

interface HttpUtil {
    @POST("test")
    @FormUrlEncoded
    fun getData(
            @Field("page") page: Int,
            @Field("pageSize") pageSize: Int
    ): Call<ResultBean>

    @POST("test")
    @FormUrlEncoded
    suspend fun getData2(
            @Field("page") page: Int,
            @Field("pageSize") pageSize: Int
    ): ResultBean

    companion object {
        fun createHttp(
                url: String = "http://www.baidu.com",
                connectTimeout: Long = 60,
                readTimeout: Long = 60,
                writeTimeout: Long = 60
        ): HttpUtil {
            val gson = GsonBuilder()
                    //配置你的Gson
                    .setDateFormat("yyyy-MM-dd hh:mm:ss")
                    .create()
            val logger = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
                Log.d("API", URLDecoder.decode(it, StandardCharsets.UTF_8.name()))
            })
            logger.level = HttpLoggingInterceptor.Level.BASIC
            val builder = OkHttpClient.Builder()
                    .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                    .readTimeout(readTimeout, TimeUnit.SECONDS)
                    .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                    .addInterceptor(logger)
            val jsonObject = JSONObject()
            jsonObject.put("code", 200)
            val jsonArray = JSONArray()
            for (i in 0 until 10) {
                val jsonObjectInner = JSONObject()
                jsonObjectInner.put("name", "name$i${Random.nextInt(Int.MAX_VALUE)}")
                jsonArray.put(jsonObjectInner)
            }
            jsonObject.put("data", jsonArray)
            builder.addInterceptor(FakeInterceptor("test", jsonObject.toString()))
            return Retrofit.Builder()
                    .baseUrl(url)
                    .client(builder.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build().create(HttpUtil::class.java)
        }
    }
}