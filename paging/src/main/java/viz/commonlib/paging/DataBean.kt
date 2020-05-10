package viz.commonlib.paging

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * @title: DataBean
 * @projectName InsuranceDoubleRecord
 * @description:
 * @author wei
 * @date 2020-04-24 17:52
 */
@Parcelize
data class DataBean<E : Parcelable>(
    var before: String = "",
    var after: String = "",
    var items: MutableList<E> = mutableListOf()
) : Parcelable