package viz.demo.paging.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * @title: InnerDataBean
 * @projectName PagingLib
 * @description:
 * @author wei
 * @date 2020-05-10 13:28
 */

@Parcelize
data class InnerDataBean(
        var name: String
) : Parcelable