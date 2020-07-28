package viz.commonlib.paging3

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.io.Serializable

/**
 * @title: DataBean
 * @projectName InsuranceDoubleRecord
 * @description:
 * @author wei
 * @date 2020-04-24 17:52
 */
@Parcelize
data class DataBean<K : Serializable, N : Parcelable>(
        var before: K? = null,
        var after: K? = null,
        var items: MutableList<N> = mutableListOf()
) : Parcelable