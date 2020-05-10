package viz.commonlib.paging

import android.view.View

interface OnRecycleViewerItemChildViewClickListener<T> {
    fun onChildViewClick(view: View, data: T, position: Int)
    fun onChildViewLongClick(view: View, data: T, position: Int): Boolean
}