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
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import retrofit2.Call
import java.util.concurrent.Executor

/**
 * Repository implementation that returns a Listing that loads data directly from network by using
 * the previous / next page keys returned in the query.
 */
class InMemoryByPageKeyCommonRepository<T, E : Parcelable, Q : Parcelable>(
        private val initCallback: (query: Q, limit: Int) -> Call<T>,
        private val afterCallback: (query: Q, after: String, limit: Int) -> Call<T>,
        private val formatItems: (t: T) -> DataBean<E>,
        private val networkExecutor: Executor
) : CommonPostRepository<E, Q> {
    @MainThread
    override fun postsOfCommon(query: Q, pageSize: Int): Listing<E> {
        val sourceFactory =
            CommonPageKeyedDataSourceFactory(
                initCallback,
                afterCallback,
                formatItems,
                query,
                networkExecutor
            )

        val livePagedList = LivePagedListBuilder(
            sourceFactory,
//                PagedList.Config.Builder()
//                        .setPageSize(pageSize)
//                        .setEnablePlaceholders(false)
//                        .setInitialLoadSizeHint(30)
//                        .setPrefetchDistance(10)
//                        .build()
            pageSize
        )
            // provide custom executor for network requests, otherwise it will default to
            // Arch Components' IO pool which is also used for disk access
            .setFetchExecutor(networkExecutor)
            .build()

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }
        return Listing(
            pagedList = livePagedList,
            networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                it.networkState
            },
            retry = {
                sourceFactory.sourceLiveData.value?.retryAllFailed()
            },
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }
}

