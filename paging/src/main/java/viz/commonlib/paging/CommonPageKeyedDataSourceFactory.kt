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
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import retrofit2.Call
import java.util.concurrent.Executor

/**
 * A simple data source factory which also provides a way to observe the last created data source.
 * This allows us to channel its network request status etc back to the UI. See the Listing creation
 * in the Repository class.
 */
class CommonPageKeyedDataSourceFactory<T, E : Parcelable, Q : Parcelable>(
        private val initCallback: (query: Q, limit: Int) -> Call<T>,
        private val afterCallback: (query: Q, after: String, limit: Int) -> Call<T>,
        private val onError: (errorEntity: ErrorEntity) -> Unit,
        private val formatItems: (t: T) -> DataBean<E>,
        private val query: Q,
        private val retryExecutor: Executor
) : DataSource.Factory<String, E>() {
    val sourceLiveData = MutableLiveData<PageKeyedCommonDataSource<T, E, Q>>()
    override fun create(): DataSource<String, E> {
        val source = PageKeyedCommonDataSource(
                initCallback,
                afterCallback,
                onError,
                formatItems,
                query,
                retryExecutor
        )
        sourceLiveData.postValue(source)
        return source
    }
}
