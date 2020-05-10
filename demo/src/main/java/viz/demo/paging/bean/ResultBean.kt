package viz.demo.paging.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * @title: ResultBean
 * @projectName PagingLib
 * @description:
 * @author wei
 * @date 2020-05-10 13:26
 */
@Parcelize
data class ResultBean(
        var code: Int,
        var data: MutableList<InnerDataBean>
) : Parcelable