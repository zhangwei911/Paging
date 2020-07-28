package viz.commonlib.paging3.byPage

import android.os.Parcelable
import androidx.paging.PagingSource
import kotlinx.coroutines.CoroutineScope
import retrofit2.Call
import viz.commonlib.paging3.DataBean
import viz.commonlib.paging3.ErrorEntity
import java.io.Serializable

/**
 * @title: PageKeyedPagingSource
 * @projectName PagingLib
 * @description:
 * @author zhangwei
 * @date 2020/7/6 14:43
 * K 上一页下一页字段
 * D 网络请求结果类
 * N 组装后的列表实体类
 * Q 网络请求字段类
 */
class PageKeyedPagingSource<K : Serializable, D : Parcelable, N : Parcelable, Q : Parcelable>(
        private val httpCallback: suspend (query: Q?, before: K?, after: K?, limit: Int) -> D,
        private val formatItems: (data: D) -> DataBean<K, N>,
        private val query: Q?=null
) : PagingSource<K, N>() {
    override suspend fun load(params: LoadParams<K>): LoadResult<K, N> {
        return try {
            val data = httpCallback.invoke(query, if (params is LoadParams.Prepend) params.key else null, if (params is LoadParams.Append) params.key else null, params.loadSize)
            val dataBean = formatItems.invoke(data)
            LoadResult.Page(
                    data = dataBean.items, // 返回获取到的数据
                    prevKey = dataBean.before, // 上一页，设置为空就没有上一页的效果，这需要注意的是，如果是第一页需要返回 null，否则会出现多次请求
                    nextKey = dataBean.after// 下一页，设置为空就没有加载更多效果，需要注意的是，如果是最后一页返回 null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            LoadResult.Error(e)
        }
    }
}