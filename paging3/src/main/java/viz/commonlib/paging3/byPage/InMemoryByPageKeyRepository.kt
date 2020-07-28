package viz.commonlib.paging3.byPage

import android.os.Parcelable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import viz.commonlib.paging3.DataBean
import viz.commonlib.paging3.repository.CommonPostRepository
import java.io.Serializable

/**
 * @title: InMemoryByPageKeyRepository
 * @projectName PagingLib
 * @description:
 * @author zhangwei
 * @date 2020/7/6 15:06
 */
class InMemoryByPageKeyRepository<K : Serializable, D : Parcelable, N : Parcelable, Q : Parcelable>(
        private val httpCallback:suspend  (query: Q?, before: K?, after: K?, limit: Int) -> D,
        private val formatItems: (data: D) -> DataBean<K, N>
) : CommonPostRepository<N, Q> {
    override fun postsOfCommon(query: Q?, pageSize: Int): Flow<PagingData<N>> = Pager(PagingConfig(pageSize)) {
        PageKeyedPagingSource(
                httpCallback,
                formatItems,
                query
        )
    }.flow
}