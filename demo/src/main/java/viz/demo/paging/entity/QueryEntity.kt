package viz.demo.paging.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class QueryEntity(
    var path: String = "",
    var kw: String = "",
    var location: String = "",
    var uid: String = "",
    var ak: String = "",
    var type: Int = -1
) : Parcelable