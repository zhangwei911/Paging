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

package viz.commonlib.paging3

import android.app.Application
import android.content.Context
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.paging.DataSource
import viz.commonlib.paging3.byItem.InMemoryByItemKeyRepository
import viz.commonlib.paging3.byPage.InMemoryByPageKeyRepository
import viz.commonlib.paging3.repository.CommonPostRepository
import java.io.Serializable
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Super simplified service locator implementation to allow us to replace default implementations
 * for testing.
 */
interface ServiceLocator {
    companion object {
        private val LOCK = Any()
        private var instance: ServiceLocator? = null
        fun instance(context: Context): ServiceLocator {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = DefaultServiceLocator(
                            app = context.applicationContext as Application,
                            useInMemoryDb = false
                    )
                }
                return instance!!
            }
        }

        /**
         * Allows tests to replace the default implementations.
         */
        @VisibleForTesting
        fun swap(locator: ServiceLocator) {
            instance = locator
        }
    }

    fun <K : Serializable, D : Parcelable, N : Parcelable, Q : Parcelable> getCommonNewRepository(
            type: CommonPostRepository.Type,
            httpCallback: suspend (query: Q?, before: K?, after: K?, limit: Int) -> D,
            formatItems: (data: D) -> DataBean<K, N>,
            subredditName: String? = null,
            insertResultIntoDb: ((subredditName: String, items: MutableList<N>) -> Unit)? = null,
            networkPageSize: Int = 0,
            afterMethodName: String? = null,
            dataSourceFactory: DataSource.Factory<Int, N>? = null,
            keyMethodName: String? = null
    ): CommonPostRepository<N, Q>

    fun getNetworkExecutor(): Executor

    fun getDiskIOExecutor(): Executor
}

/**
 * default implementation of ServiceLocator that uses production endpoints.
 */
open class DefaultServiceLocator(val app: Application, val useInMemoryDb: Boolean) :
        ServiceLocator {
    // thread pool used for disk access
    @Suppress("PrivatePropertyName")
    private val DISK_IO = Executors.newSingleThreadExecutor()

    // thread pool used for network requests
    @Suppress("PrivatePropertyName")
    private val NETWORK_IO = Executors.newFixedThreadPool(5)

    override fun <K : Serializable, D : Parcelable, N : Parcelable, Q : Parcelable> getCommonNewRepository(
            type: CommonPostRepository.Type,
            httpCallback: suspend (query: Q?, before: K?, after: K?, limit: Int) -> D,
            formatItems: (d: D) -> DataBean<K, N>,
            subredditName: String?,
            insertResultIntoDb: ((subredditName: String, items: MutableList<N>) -> Unit)?,
            networkPageSize: Int,
            afterMethodName: String?,
            dataSourceFactory: DataSource.Factory<Int, N>?,
            keyMethodName: String?
    ): CommonPostRepository<N, Q> {
        return when (type) {
            CommonPostRepository.Type.IN_MEMORY_BY_PAGE -> InMemoryByPageKeyRepository(
                    httpCallback,
                    formatItems
            )
//            CommonPostRepository.Type.IN_MEMORY_BY_ITEM
            else -> InMemoryByItemKeyRepository(
                    httpCallback,
                    formatItems,
                    keyMethodName!!
            )
//            else -> InMemoryByDBCommonRepository(
//                    subredditName!!,
//                    initCallback,
//                    afterCallback,
//                    formatItems,
//                    insertResultIntoDb!!,
//                    getDiskIOExecutor(),
//                    networkPageSize,
//                    afterMethodName!!,
//                    dataSourceFactory!!
//            )
        }
    }

    override fun getNetworkExecutor(): Executor = NETWORK_IO

    override fun getDiskIOExecutor(): Executor = DISK_IO
}