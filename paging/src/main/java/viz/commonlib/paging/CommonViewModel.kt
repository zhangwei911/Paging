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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.Transformations.switchMap
import androidx.paging.DataSource
import retrofit2.Call

/**
 * A RecyclerView ViewHolder that displays a single reddit post.
 */
class CommonViewModel<T, E : Parcelable, Q : Parcelable>(
        private val repository: CommonPostRepository<E, Q>,
        private val pageSize: Int = 10,
        handle: SavedStateHandle? = null
) : ViewModel() {
    private val query = MutableLiveData<Q>()
    private val repoResult = map(query) {
        repository.postsOfCommon(it, pageSize)
    }
    val posts = switchMap(repoResult) { it.pagedList }
    val networkState = switchMap(repoResult) { it.networkState }
    val refreshState = switchMap(repoResult) { it.refreshState }

    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    fun showQuery(query: Q): Boolean {
        if (this.query.value == query) {
            return false
        }
        this.query.value = query
        return true
    }

    fun retry() {
        val listing = repoResult?.value
        listing?.retry?.invoke()
    }

    fun currentQuery(): Q? = this.query.value

    companion object {
        fun <T, E : Parcelable, Q : Parcelable> getViewModel(
                fragment: Fragment,
                type: CommonPostRepository.Type,
                initCallback: (query: Q, limit: Int) -> Call<T>,
                afterCallback: (query: Q, after: String, limit: Int) -> Call<T>,
                formatItems: (t: T) -> DataBean<E>,
                subredditName: String? = null,
                insertResultIntoDb: ((subredditName: String, items: MutableList<E>) -> Unit)? = null,
                query: Q? = null,
                networkPageSize: Int = 0,
                afterMethodName: String? = null,
                dataSourceFactory: DataSource.Factory<Int, E>? = null,
                keyMethodName: String? = null,
                savedState: Boolean = false,
                pageSize: Int = 10,
                key: String
        ): CommonViewModel<T, E, Q> {
            val repo = ServiceLocator.instance(fragment.requireContext())
                    .getCommonNewRepository(
                            type,
                            { query, limit ->
                                initCallback.invoke(query, limit)
                            },
                            { query, after, limit ->
                                afterCallback.invoke(query, after, limit)
                            },
                            formatItems,
                            subredditName,
                            insertResultIntoDb,
                            query,
                            networkPageSize,
                            afterMethodName,
                            dataSourceFactory,
                            keyMethodName
                    )
            return ViewModelProvider(
                    fragment,
                    if (savedState) {
                        object : AbstractSavedStateViewModelFactory(fragment, null) {
                            override fun <T : ViewModel?> create(
                                    key: String,
                                    modelClass: Class<T>,
                                    handle: SavedStateHandle
                            ): T {
                                return CommonViewModel<T, E, Q>(repo, handle = handle) as T
                            }
                        }
                    } else {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                                return CommonViewModel<T, E, Q>(repo, pageSize) as T
                            }
                        }
                    })[key, CommonViewModel::class.java] as CommonViewModel<T, E, Q>
        }

        fun <T, F, E : Parcelable, Q : Parcelable> getViewModel(
                f: F,
                fragment: Fragment,
                type: CommonPostRepository.Type,
                initCallback: (f: F, query: Q, limit: Int) -> Call<T>,
                afterCallback: (f: F, query: Q, after: String, limit: Int) -> Call<T>,
                formatItems: (t: T) -> DataBean<E>,
                subredditName: String? = null,
                insertResultIntoDb: ((subredditName: String, items: MutableList<E>) -> Unit)? = null,
                query: Q? = null,
                networkPageSize: Int = 0,
                afterMethodName: String? = null,
                dataSourceFactory: DataSource.Factory<Int, E>? = null,
                keyMethodName: String? = null,
                savedState: Boolean = false,
                pageSize: Int = 10,
                key: String
        ): CommonViewModel<T, E, Q> {
            val repo = ServiceLocator.instance(fragment.requireContext())
                    .getCommonNewRepository(
                            type,
                            { query, limit ->
                                initCallback.invoke(f, query, limit)
                            }, { query, after, limit ->
                        afterCallback.invoke(f, query, after, limit)
                    },
                            formatItems,
                            subredditName,
                            insertResultIntoDb,
                            query,
                            networkPageSize,
                            afterMethodName,
                            dataSourceFactory,
                            keyMethodName
                    )
            return ViewModelProvider(fragment,
                    if (savedState) {
                        object : AbstractSavedStateViewModelFactory(fragment, null) {
                            override fun <T : ViewModel?> create(
                                    key: String,
                                    modelClass: Class<T>,
                                    handle: SavedStateHandle
                            ): T {
                                return CommonViewModel<T, E, Q>(repo, pageSize, handle = handle) as T
                            }
                        }
                    } else {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                                return CommonViewModel<T, E, Q>(repo, pageSize) as T
                            }
                        }
                    })[key, CommonViewModel::class.java] as CommonViewModel<T, E, Q>
        }

        fun <T, E : Parcelable, Q : Parcelable> getViewModel(
                fragmentActivity: FragmentActivity,
                type: CommonPostRepository.Type,
                initCallback: (query: Q, limit: Int) -> Call<T>,
                afterCallback: (query: Q, after: String, limit: Int) -> Call<T>,
                formatItems: (t: T) -> DataBean<E>,
                subredditName: String? = null,
                insertResultIntoDb: ((subredditName: String, items: MutableList<E>) -> Unit)? = null,
                query: Q? = null,
                networkPageSize: Int = 0,
                afterMethodName: String? = null,
                dataSourceFactory: DataSource.Factory<Int, E>? = null,
                keyMethodName: String? = null,
                savedState: Boolean = false,
                pageSize: Int = 10,
                key: String
        ): CommonViewModel<T, E, Q> {
            val repo = ServiceLocator.instance(fragmentActivity)
                    .getCommonNewRepository(
                            type, { query, limit ->
                        initCallback.invoke(query, limit)
                    }, { query, after, limit ->
                        afterCallback.invoke(query, after, limit)
                    },
                            formatItems,
                            subredditName,
                            insertResultIntoDb,
                            query,
                            networkPageSize,
                            afterMethodName,
                            dataSourceFactory,
                            keyMethodName
                    )
            return ViewModelProvider(fragmentActivity,
                    if (savedState) {
                        object : AbstractSavedStateViewModelFactory(fragmentActivity, null) {
                            override fun <T : ViewModel?> create(
                                    key: String,
                                    modelClass: Class<T>,
                                    handle: SavedStateHandle
                            ): T {
                                return CommonViewModel<T, E, Q>(repo, pageSize, handle = handle) as T
                            }
                        }
                    } else {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                                return CommonViewModel<T, E, Q>(repo, pageSize) as T
                            }
                        }
                    })[key, CommonViewModel::class.java] as CommonViewModel<T, E, Q>
        }

        fun <T, F, E : Parcelable, Q : Parcelable> getViewModel(
                f: F,
                fragmentActivity: FragmentActivity,
                type: CommonPostRepository.Type,
                initCallback: (f: F, query: Q, limit: Int) -> Call<T>,
                afterCallback: (f: F, query: Q, after: String, limit: Int) -> Call<T>,
                formatItems: (t: T) -> DataBean<E>,
                subredditName: String? = null,
                insertResultIntoDb: ((subredditName: String, items: MutableList<E>) -> Unit)? = null,
                query: Q? = null,
                networkPageSize: Int = 0,
                afterMethodName: String? = null,
                dataSourceFactory: DataSource.Factory<Int, E>? = null,
                keyMethodName: String? = null,
                savedState: Boolean = false,
                pageSize: Int = 10,
                key: String
        ): CommonViewModel<T, E, Q> {
            val repo = ServiceLocator.instance(fragmentActivity)
                    .getCommonNewRepository(
                            type, { query, limit ->
                        initCallback.invoke(f, query, limit)
                    }, { query, after, limit ->
                        afterCallback.invoke(f, query, after, limit)
                    },
                            formatItems,
                            subredditName,
                            insertResultIntoDb,
                            query,
                            networkPageSize,
                            afterMethodName,
                            dataSourceFactory,
                            keyMethodName
                    )
            return ViewModelProvider(fragmentActivity,
                    if (savedState) {
                        object : AbstractSavedStateViewModelFactory(fragmentActivity, null) {
                            override fun <T : ViewModel?> create(
                                    key: String,
                                    modelClass: Class<T>,
                                    handle: SavedStateHandle
                            ): T {
                                return CommonViewModel<T, E, Q>(repo, pageSize, handle = handle) as T
                            }
                        }
                    } else {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                                return CommonViewModel<T, E, Q>(repo, pageSize) as T
                            }
                        }
                    })[key, CommonViewModel::class.java] as CommonViewModel<T, E, Q>
        }
    }
}