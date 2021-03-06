package viz.commonlib.paging

import android.view.LayoutInflater
import android.view.ViewGroup

abstract class CommonViewHolder<T> : androidx.recyclerview.widget.RecyclerView.ViewHolder {
    constructor(
        parent: ViewGroup,
        layoutId: Int,
        glideRequests: GlideRequests? = null,
        listener: OnRecycleViewerItemChildViewClickListener<T>? = null
    ) : super(LayoutInflater.from(parent.context).inflate(layoutId, parent, false)) {
        itemView.tag = this
    }

    abstract fun bindTo(data: T?, position: Int)
    abstract fun update(data: T?, position: Int)
}
