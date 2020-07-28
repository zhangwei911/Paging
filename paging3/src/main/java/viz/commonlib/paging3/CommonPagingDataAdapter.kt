package viz.commonlib.paging3

import android.os.Parcelable
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import viz.commonlib.paging3.repository.NetworkState

/**
 * @title: CommonPagingDataAdapter
 * @projectName Paging3SimpleWithNetWork
 * @description:
 * @author zhangwei
 * @date 2020/7/6 13:34
 */
class CommonPagingDataAdapter<D : Parcelable, V : RecyclerView.ViewHolder> : PagingDataAdapter<D, V> {
    private var layoutId: Int
    private lateinit var createViewHolder: (parent: ViewGroup, layoutId: Int) -> RecyclerView.ViewHolder
    private lateinit var createViewHolderWithListener: (parent: ViewGroup, layoutId: Int, listener: OnRecycleViewerItemChildViewClickListener<D>) -> V
    private var listener: OnRecycleViewerItemChildViewClickListener<D>? = null

    private var networkState: NetworkState? = null
    var showNetworkStateItem = true
    private var retryCallback: () -> Unit

    constructor(
            layoutId: Int,
            createViewHolder: (parent: ViewGroup, layoutId: Int) -> RecyclerView.ViewHolder,
            retryCallback: () -> Unit
    ) : super(DiffCallBack<D>()) {
        this.layoutId = layoutId
        this.createViewHolder = createViewHolder
        this.retryCallback = retryCallback
    }

    constructor(
            layoutId: Int,
            createViewHolderWithListener: (parent: ViewGroup, layoutId: Int, listener: OnRecycleViewerItemChildViewClickListener<D>) -> V,
            retryCallback: () -> Unit
    ) : super(DiffCallBack<D>()) {
        this.layoutId = layoutId
        this.createViewHolderWithListener = createViewHolderWithListener
        this.retryCallback = retryCallback
    }

    override fun onBindViewHolder(holder: V, position: Int) {
        when (getItemViewType(position)) {
            layoutId -> (holder as CommonViewHolder<D>).bindTo(getItem(position), position)
            R.layout.network_state_item -> (holder as NetworkStateItemViewHolder).bindTo(
                    networkState
            )
        }
    }

    override fun onBindViewHolder(holder: V, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val item = getItem(position)
            (holder as CommonViewHolder<D>).update(item, position)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): V {
        return when (viewType) {
            layoutId -> {
                if (listener == null) {
                    createViewHolder(parent, layoutId)
                } else {
                    createViewHolderWithListener(parent, layoutId, listener!!)
                }
            }
            R.layout.network_state_item -> {
                NetworkStateItemViewHolder.create(parent, retryCallback) as V
            }
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    private fun hasExtraRow() = showNetworkStateItem && networkState != null && networkState != NetworkState.LOADED

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.network_state_item
        } else {
            layoutId
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }

    fun updateItem(position: Int) {
        notifyItemChanged(position, "payload")
    }

    fun setOnRecycleViewerItemChildViewClickListener(listener: OnRecycleViewerItemChildViewClickListener<D>) {
        this.listener = listener
    }
}