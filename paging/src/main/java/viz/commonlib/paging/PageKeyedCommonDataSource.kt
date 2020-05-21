/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package viz.commonlib.paging

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import okhttp3.internal.toImmutableList
import retrofit2.Call
import java.io.IOException
import java.util.*
import java.util.concurrent.Executor

/**
 * A data source that uses the before/after keys returned in page requests.
 * <p>
 * See ItemKeyedSubredditDataSource
 */
class PageKeyedCommonDataSource<T, E : Parcelable, Q : Parcelable>(
        private val initCallback: (query: Q, limit: Int) -> Call<T>,
        private val afterCallback: (query: Q, after: String, limit: Int) -> Call<T>,
        private val onError: (errorEntity: ErrorEntity) -> Unit,
        private val formatItems: (t: T) -> DataBean<E>,
        private val query: Q,
        private val retryExecutor: Executor
) : PageKeyedDataSource<String, E>() {

    // keep a function reference for the retry event
    private var retry: (() -> Any)? = null

    /**
     * There is no sync on the state because paging will always call loadInitial first then wait
     * for it to return some success value before calling loadAfter.
     */
    val networkState = MutableLiveData<NetworkState>()

    val initialLoad = MutableLiveData<NetworkState>()

    fun retryAllFailed() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
            retryExecutor.execute {
                it.invoke()
            }
        }
    }

    override fun loadBefore(
            params: LoadParams<String>,
            callback: LoadCallback<String, E>
    ) {
        // ignored, since we only ever append to our initial load
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<String, E>) {
        networkState.postValue(NetworkState.LOADING)
        Log.i("la", params.key)
        afterCallback(
                query,
                params.key,
                params.requestedLoadSize
        ).enqueue(
                VCallback<T>(onResult = { call, response, result ->
                    if (result != null) {
                        val dataBean = formatItems.invoke(result)
                        val items = dataBean.items.toImmutableList()
                        Log.i("la", items.size.toString())
                        retry = null
                        networkState.postValue(NetworkState.LOADED)
                        callback.onResult(items, dataBean.after)
                    } else {
                        val errorBody = response.errorBody()
                        val error = NetworkState.error(errorBody?.string())
                        if (null != response.errorBody()) {
                            //解析后台返回的错误信息
                            var errorEntity = ErrorEntity()
                            try {
                                errorEntity = ErrorEntity(response.errorBody()!!.string())
                                if (errorEntity.message.isNullOrEmpty()) {
                                    errorEntity.message = response.message()
                                    errorEntity.error = response.message()
                                    errorEntity.status = response.code()
                                    errorEntity.timestamp = Date().toString()
                                    errorEntity.path = response.raw().request.url.encodedPath
                                }
                                errorEntity.url = response.raw().request.url.toString()
                            } catch (e: IOException) {
                                Log.e("ErrorEntityTag", "ErrorEntity解析错误:" + e.message)
                                errorEntity.message = "解析错误"
                                errorEntity.error = response.message()
                                errorEntity.status = response.code()
                                errorEntity.timestamp = Date().toString()
                                errorEntity.path = response.raw().request.url.encodedPath
                                errorEntity.url = response.raw().request.url.toString()
                            }
                            onError.invoke(errorEntity)
                            error.code = errorEntity.status
                            error.error = errorEntity.error
                            error.message = errorEntity.message
                            error.path = errorEntity.path
                            error.url = errorEntity.url
                            error.timestamp = errorEntity.timestamp
                        }
                        retry = {
                            loadAfter(params, callback)
                        }
                        networkState.postValue(error)
                        initialLoad.postValue(error)
                    }
                }, onError = { errorEntity, call, t, response ->
                    onError.invoke(errorEntity)
                    retry = {
                        loadAfter(params, callback)
                    }
                    networkState.postValue(NetworkState.error(t?.message ?: "unknown err"))
                })
        )
    }

    override fun loadInitial(
            params: LoadInitialParams<String>,
            callback: LoadInitialCallback<String, E>
    ) {
        val request = initCallback(
                query,
                params.requestedLoadSize
        )
        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)

        // triggered by a refresh, we better execute sync
        try {
            val response = request.execute()
            val body = response.body()
            if (body != null) {
                val dataBean = formatItems.invoke(body)
                val items = dataBean.items.toImmutableList()
                retry = null
                networkState.postValue(NetworkState.LOADED)
                initialLoad.postValue(NetworkState.LOADED)
                callback.onResult(items, dataBean.before, dataBean.after)
            } else {
                val errorBody = response.errorBody()
                val error = NetworkState.error(errorBody?.string())
                if (null != response.errorBody()) {
                    //解析后台返回的错误信息
                    var errorEntity = ErrorEntity()
                    try {
                        errorEntity = ErrorEntity(response.errorBody()!!.string())
                        if (errorEntity.message.isNullOrEmpty()) {
                            errorEntity.message = response.message()
                            errorEntity.error = response.message()
                            errorEntity.status = response.code()
                            errorEntity.timestamp = Date().toString()
                            errorEntity.path = response.raw().request.url.encodedPath
                        }
                        errorEntity.url = response.raw().request.url.toString()
                    } catch (e: IOException) {
                        Log.e("ErrorEntityTag", "ErrorEntity解析错误:" + e.message)
                        errorEntity.message = "解析错误"
                        errorEntity.error = response.message()
                        errorEntity.status = response.code()
                        errorEntity.timestamp = Date().toString()
                        errorEntity.path = response.raw().request.url.encodedPath
                        errorEntity.url = response.raw().request.url.toString()
                    }
                    onError.invoke(errorEntity)
                    error.code = errorEntity.status
                    error.error = errorEntity.error
                    error.message = errorEntity.message
                    error.path = errorEntity.path
                    error.url = errorEntity.url
                    error.timestamp = errorEntity.timestamp
                }
                retry = {
                    loadInitial(params, callback)
                }
                networkState.postValue(error)
                initialLoad.postValue(error)
            }
        } catch (ioException: IOException) {
            retry = {
                loadInitial(params, callback)
            }
            val error = NetworkState.error(ioException.message ?: "unknown error")
            networkState.postValue(error)
            initialLoad.postValue(error)
        }
    }
}
