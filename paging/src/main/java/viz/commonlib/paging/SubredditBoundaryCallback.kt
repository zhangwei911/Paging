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
import androidx.annotation.MainThread
import androidx.paging.PagedList
import retrofit2.Call
import retrofit2.Response
import java.util.concurrent.Executor

/**
 * This boundary callback gets notified when user reaches to the edges of the list such that the
 * database cannot provide any more data.
 * <p>
 * The boundary callback might be called multiple times for the same direction so it does its own
 * rate limiting using the PagingRequestHelper class.
 */
class SubredditBoundaryCallback<T, E : Parcelable,Q : Parcelable>(
        private val subredditName: String,
        private val initCallback: (query: Q, limit: Int) -> Call<T>,
        private val afterCallback: (query: Q, after: String, limit: Int) -> Call<T>,
        private val handleResponse: (subredditName: String, items: MutableList<E>) -> Unit,
        private val formatItems: (t: T) -> DataBean<E>,
        private val query: Q,
        private val ioExecutor: Executor,
        private val networkPageSize: Int,
        private val afterMethodName: String
) : PagedList.BoundaryCallback<E>() {

    val helper = PagingRequestHelper(ioExecutor)
    val networkState = helper.createStatusLiveData()

    /**
     * Database returned 0 items. We should query the backend for more items.
     */
    @MainThread
    override fun onZeroItemsLoaded() {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            initCallback(
                query,
                networkPageSize
            ).enqueue(VCallback<T>(onResult = { call, response, result ->
                insertItemsIntoDb(response, it)
            }, onError = { errorEntity, call, t, response ->
                it.recordFailure(t!!)
            }
            ))
        }
    }

    /**
     * every time it gets new items, boundary callback simply inserts them into the database and
     * paging library takes care of refreshing the list if necessary.
     */
    private fun insertItemsIntoDb(
        response: Response<T>,
        it: PagingRequestHelper.Request.Callback
    ) {
        ioExecutor.execute {
            val dataBean = formatItems.invoke(response.body()!!)
            val items = dataBean.items
            handleResponse(subredditName, items)
            it.recordSuccess()
        }
    }

    /**
     * User reached to the end of the list.
     */
    @MainThread
    override fun onItemAtEndLoaded(itemAtEnd: E) {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            val getErrorMethod = itemAtEnd::class.java.getMethod(afterMethodName)
            val keyMethod = getErrorMethod.invoke(itemAtEnd)
            val after = keyMethod?.toString() ?: ""
            afterCallback(
                query,
                after,
                networkPageSize
            ).enqueue(VCallback<T>(onResult = { call, response, result ->
                insertItemsIntoDb(response, it)
            }, onError = { errorEntity, call, t, response ->
                it.recordFailure(t!!)
            }
            ))
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: E) {
        // ignored, since we only ever append to what's in the DB
    }

}