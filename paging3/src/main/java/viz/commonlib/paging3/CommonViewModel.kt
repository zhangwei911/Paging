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

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.Transformations.switchMap
import androidx.paging.DataSource
import androidx.paging.PagingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import retrofit2.Call
import viz.commonlib.paging3.repository.CommonPostRepository
import java.io.Serializable

/**
 * A RecyclerView ViewHolder that displays a single reddit post.
 */
class CommonViewModel<T, E : Parcelable, Q : Parcelable>(
        private val repository: CommonPostRepository<E, Q>,
        private val pageSize: Int = 10,
        private val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {
    private val query = MutableLiveData<Q>()
    private val DEFAULT_SUBREDDIT: Q? = null

    private val clearListCh = Channel<Unit>(Channel.CONFLATED)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val posts = flowOf(
            clearListCh.consumeAsFlow().map { PagingData.empty<E>() },
            savedStateHandle?.getLiveData<Q>(KEY_SUBREDDIT)?.asFlow()?.flatMapLatest { repository.postsOfCommon(it, 30) }
                    ?: query.asFlow().flatMapLatest {
                        repository.postsOfCommon(it, 30)
                    }
    ).flattenMerge(2)

    fun shouldShowSubreddit(
            query: Q
    ) = (savedStateHandle != null && savedStateHandle.get<Q>(KEY_SUBREDDIT) != query) || (savedStateHandle == null && this.query.value != query)

    fun showQuery(query: Q): Boolean {
        if (!shouldShowSubreddit(query)) return false

        clearListCh.offer(Unit)

        this.query.value = query
        savedStateHandle?.set(KEY_SUBREDDIT, query)
        return true
    }

    fun currentQuery(): Q? = this.query.value

    init {
        if (savedStateHandle != null && !savedStateHandle.contains(KEY_SUBREDDIT)) {
            savedStateHandle.set(KEY_SUBREDDIT, DEFAULT_SUBREDDIT)
        }
    }

    companion object {
        const val KEY_SUBREDDIT = "subreddit"
        fun <K : Serializable, D : Parcelable, N : Parcelable, Q : Parcelable> getViewModel(
                fragment: Fragment,
                type: CommonPostRepository.Type,
                httpCallback: suspend (query: Q?, before: K?, after: K?, limit: Int) -> D,
                onError: ((errorEntity: ErrorEntity) -> Unit)? = null,
                formatItems: (d: D) -> DataBean<K, N>,
                subredditName: String? = null,
                insertResultIntoDb: ((subredditName: String, items: MutableList<N>) -> Unit)? = null,
                query: Q? = null,
                networkPageSize: Int = 0,
                afterMethodName: String? = null,
                dataSourceFactory: DataSource.Factory<Int, N>? = null,
                keyMethodName: String? = null,
                savedState: Boolean = false,
                pageSize: Int = 10,
                key: String
        ): CommonViewModel<D, N, Q> {
            val repo = ServiceLocator.instance(fragment.requireContext()).getCommonNewRepository(type, httpCallback, formatItems, query = query!!, keyMethodName = keyMethodName)
            return ViewModelProvider(
                    fragment,
                    if (savedState) {
                        object : AbstractSavedStateViewModelFactory(fragment, null) {
                            override fun <T : ViewModel?> create(
                                    key: String,
                                    modelClass: Class<T>,
                                    handle: SavedStateHandle
                            ): T {
                                return CommonViewModel<T, N, Q>(repo, savedStateHandle = handle) as T
                            }
                        }
                    } else {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                                return CommonViewModel<T, N, Q>(repo, pageSize) as T
                            }
                        }
                    })[key, CommonViewModel::class.java] as CommonViewModel<D, N, Q>
        }

        fun <D : Parcelable, F, K : Serializable, N : Parcelable, Q : Parcelable> getViewModel(
                f: F,
                fragment: Fragment,
                type: CommonPostRepository.Type,
                httpCallback: (f: F, query: Q?, before: K?, after: K?, limit: Int) -> D,
                onError: ((errorEntity: ErrorEntity) -> Unit)? = null,
                formatItems: (d: D) -> DataBean<K, N>,
                subredditName: String? = null,
                insertResultIntoDb: ((subredditName: String, items: MutableList<N>) -> Unit)? = null,
                query: Q? = null,
                networkPageSize: Int = 0,
                afterMethodName: String? = null,
                dataSourceFactory: DataSource.Factory<Int, N>? = null,
                keyMethodName: String? = null,
                savedState: Boolean = false,
                pageSize: Int = 10,
                key: String
        ): CommonViewModel<D, N, Q> {
            val repo = ServiceLocator.instance(fragment.requireContext())
                    .getCommonNewRepository(
                            type,
                            { q: Q?, before: K?, after: K?, limit ->
                                httpCallback.invoke(f, q, before, after, limit)
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
                                return CommonViewModel<T, N, Q>(repo, pageSize, savedStateHandle = handle) as T
                            }
                        }
                    } else {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                                return CommonViewModel<T, N, Q>(repo, pageSize) as T
                            }
                        }
                    })[key, CommonViewModel::class.java] as CommonViewModel<D, N, Q>
        }

        fun <K : Serializable, D : Parcelable, N : Parcelable, Q : Parcelable> getViewModel(
                fragmentActivity: FragmentActivity,
                type: CommonPostRepository.Type,
                httpCallback: suspend (query: Q?, before: K?, after: K?, limit: Int) -> D,
                onError: ((errorEntity: ErrorEntity) -> Unit)? = null,
                formatItems: (t: D) -> DataBean<K, N>,
                subredditName: String? = null,
                insertResultIntoDb: ((subredditName: String, items: MutableList<N>) -> Unit)? = null,
                query: Q? = null,
                networkPageSize: Int = 0,
                afterMethodName: String? = null,
                dataSourceFactory: DataSource.Factory<Int, N>? = null,
                keyMethodName: String? = null,
                savedState: Boolean = false,
                pageSize: Int = 10,
                key: String
        ): CommonViewModel<D, N, Q> {
            val repo = ServiceLocator.instance(fragmentActivity)
                    .getCommonNewRepository(
                            type,
                            httpCallback,
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
                                return CommonViewModel<T, N, Q>(repo, pageSize, savedStateHandle = handle) as T
                            }
                        }
                    } else {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                                return CommonViewModel<T, N, Q>(repo, pageSize) as T
                            }
                        }
                    })[key, CommonViewModel::class.java] as CommonViewModel<D, N, Q>
        }

        fun <K : Serializable, D : Parcelable, F, N : Parcelable, Q : Parcelable> getViewModel(
                f: F,
                fragmentActivity: FragmentActivity,
                type: CommonPostRepository.Type,
                httpCallback: suspend (f: F, query: Q?, before: K?, after: K?, limit: Int) -> D,
                onError: ((errorEntity: ErrorEntity) -> Unit)? = null,
                formatItems: (d: D) -> DataBean<K, N>,
                subredditName: String? = null,
                insertResultIntoDb: ((subredditName: String, items: MutableList<N>) -> Unit)? = null,
                query: Q? = null,
                networkPageSize: Int = 0,
                afterMethodName: String? = null,
                dataSourceFactory: DataSource.Factory<Int, N>? = null,
                keyMethodName: String? = null,
                savedState: Boolean = false,
                pageSize: Int = 10,
                key: String
        ): CommonViewModel<D, N, Q> {
            val repo = ServiceLocator.instance(fragmentActivity)
                    .getCommonNewRepository(
                            type,
                            { query: Q?, before: K?, after: K?, limit: Int ->
                                httpCallback.invoke(f, query, before, after, limit)
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
                                return CommonViewModel<D, N, Q>(repo, pageSize, savedStateHandle = handle) as T
                            }
                        }
                    } else {
                        object : ViewModelProvider.Factory {
                            override fun <D : ViewModel?> create(modelClass: Class<D>): D {
                                return CommonViewModel<D, N, Q>(repo, pageSize) as D
                            }
                        }
                    })[key, CommonViewModel::class.java] as CommonViewModel<D, N, Q>
        }
    }
}