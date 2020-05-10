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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import retrofit2.Call
import java.util.concurrent.Executor

/**
 * Repository implementation that returns a Listing that loads data directly from network by using
 * the previous / next page keys returned in the query.
 */
class InMemoryByDBCommonRepository<T, E : Parcelable, Q : Parcelable>(
        private val subredditName: String,
        private val initCallback: (query: Q, limit: Int) -> Call<T>,
        private val afterCallback: (query: Q, after: String, limit: Int) -> Call<T>,
        private val formatItems: (t: T) -> DataBean<E>,
        private val insertResultIntoDb: (subredditName: String, items: MutableList<E>) -> Unit,
        private val ioExecutor: Executor,
        private val networkPageSize: Int,
        private val afterMethodName: String,
        private val dataSourceFactory: DataSource.Factory<Int, E>
) : CommonPostRepository<E, Q> {
    /**
     * When refresh is called, we simply run a fresh network request and when it arrives, clear
     * the database table and insert all new items in a transaction.
     * <p>
     * Since the PagedList already uses a database bound data source, it will automatically be
     * updated after the database transaction is finished.
     */
    @MainThread
    private fun refresh(query: Q): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>()
        networkState.value = NetworkState.LOADING
        initCallback(
                query,
                networkPageSize
        ).enqueue(VCallback<T>(onResult = { call, response, result ->
            ioExecutor.execute {
                val dataBean = formatItems.invoke(result)
                val items = dataBean.items
                insertResultIntoDb(subredditName, items)
                // since we are in bg thread now, post the result.
                networkState.postValue(NetworkState.LOADED)
            }
        }, onError = { errorEntity, call, t, response ->
            networkState.value = NetworkState.error(t!!.message)
        }
        ))
        return networkState
    }

    @MainThread
    override fun postsOfCommon(query: Q, pageSize: Int): Listing<E> {
        // create a boundary callback which will observe when the user reaches to the edges of
        // the list and update the database with extra data.
        val boundaryCallback = SubredditBoundaryCallback<T, E, Q>(
                subredditName = subredditName,
                initCallback = initCallback,
                afterCallback = afterCallback,
                handleResponse = insertResultIntoDb,
                formatItems = formatItems,
                query = query,
                ioExecutor = ioExecutor,
                networkPageSize = networkPageSize,
                afterMethodName = afterMethodName
        )
        // create a data source factory from Room
        val builder = LivePagedListBuilder(dataSourceFactory, pageSize)
                .setBoundaryCallback(boundaryCallback)

        // we are using a mutable live data to trigger refresh requests which eventually calls
        // refresh method and gets a new live data. Each refresh request by the user becomes a newly
        // dispatched data in refreshTrigger
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger, {
            refresh(query)
        })

        return Listing(
                pagedList = builder.build(),
                networkState = boundaryCallback.networkState,
                retry = {
                    boundaryCallback.helper.retryAllFailed()
                },
                refresh = {
                    refreshTrigger.value = null
                },
                refreshState = refreshState
        )
    }
}

