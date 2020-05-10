/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package viz.commonlib.paging

import android.view.ViewGroup
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * A simple adapter implementation that shows Reddit posts.
 */
class CommonPostsAdapter<T>
    : PagedListAdapter<T, RecyclerView.ViewHolder> {
    private var layoutId: Int
    private lateinit var createViewHolder: (parent: ViewGroup, glide: GlideRequests, layoutId: Int) -> RecyclerView.ViewHolder
    private lateinit var createViewHolder2: (parent: ViewGroup, layoutId: Int) -> RecyclerView.ViewHolder
    private lateinit var createViewHolderWithListener: (parent: ViewGroup, glide: GlideRequests, layoutId: Int, listener: OnRecycleViewerItemChildViewClickListener<T>) -> RecyclerView.ViewHolder
    private lateinit var createViewHolderWithListener2: (parent: ViewGroup, layoutId: Int, listener: OnRecycleViewerItemChildViewClickListener<T>) -> RecyclerView.ViewHolder
    private var glide: GlideRequests? = null
    private var retryCallback: () -> Unit

    constructor(
        layoutId: Int,
        createViewHolder: (parent: ViewGroup, glide: GlideRequests, layoutId: Int) -> RecyclerView.ViewHolder,
        glide: GlideRequests,
        retryCallback: () -> Unit
    ) : super(DiffCallBack<T>()) {
        this.layoutId = layoutId
        this.createViewHolder = createViewHolder
        this.glide = glide
        this.retryCallback = retryCallback
    }

    constructor(
        layoutId: Int,
        createViewHolder: (parent: ViewGroup, layoutId: Int) -> RecyclerView.ViewHolder,
        retryCallback: () -> Unit
    ) : super(DiffCallBack<T>()) {
        this.layoutId = layoutId
        this.createViewHolder2 = createViewHolder
        this.retryCallback = retryCallback
    }

    constructor(
            layoutId: Int,
            createViewHolderWithListener: (parent: ViewGroup, glide: GlideRequests, layoutId: Int, listener: OnRecycleViewerItemChildViewClickListener<T>) -> RecyclerView.ViewHolder,
            glide: GlideRequests,
            retryCallback: () -> Unit
    ) : super(DiffCallBack<T>()) {
        this.layoutId = layoutId
        this.createViewHolderWithListener = createViewHolderWithListener
        this.glide = glide
        this.retryCallback = retryCallback
    }

    constructor(
            layoutId: Int,
            createViewHolderWithListener2: (parent: ViewGroup, layoutId: Int, listener: OnRecycleViewerItemChildViewClickListener<T>) -> RecyclerView.ViewHolder,
            retryCallback: () -> Unit
    ) : super(DiffCallBack<T>()) {
        this.layoutId = layoutId
        this.createViewHolderWithListener2 = createViewHolderWithListener2
        this.retryCallback = retryCallback
    }

    private var networkState: NetworkState? = null
    private var listener: OnRecycleViewerItemChildViewClickListener<T>? = null

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            layoutId -> (holder as CommonViewHolder<T>).bindTo(getItem(position), position)
            R.layout.network_state_item -> (holder as NetworkStateItemViewHolder).bindTo(
                networkState
            )
        }
    }


    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val item = getItem(position)
            (holder as CommonViewHolder<T>).update(item, position)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            layoutId -> if (listener == null) {
                if (glide != null) {
                    createViewHolder(parent, glide!!, layoutId)
                } else {
                    createViewHolder2(parent, layoutId)
                }
            } else {
                if (glide != null) {
                    createViewHolderWithListener(parent, glide!!, layoutId, listener!!)
                } else {
                    createViewHolderWithListener2(parent, layoutId, listener!!)
                }
            }
            R.layout.network_state_item -> NetworkStateItemViewHolder.create(parent, retryCallback)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    private fun hasExtraRow() = networkState != null && networkState != NetworkState.LOADED

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.network_state_item
        } else {
            layoutId
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }

//    private fun glideClear(view: View) {
//        if (view is ViewGroup && view !is AdapterView<*>) {
//            for (i in 0 until view.childCount) {
//                glideClear(view.getChildAt(i))
//            }
//        } else if (view is ImageView) {
//            glide.clear(view)
//        }
//    }
//
//    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
//        super.onViewDetachedFromWindow(holder)
//        if (holder is CommonViewHolder<*>) {
//            val itemView = (holder as CommonViewHolder<T>).itemView as ViewGroup
//            glideClear(itemView)
//        }
//    }

    fun setNetworkState(newNetworkState: NetworkState?) {
        val previousState = this.networkState
        val hadExtraRow = hasExtraRow()
        this.networkState = newNetworkState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount())
            } else {
                notifyItemInserted(super.getItemCount())
            }
        } else if (hasExtraRow && previousState != newNetworkState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    fun updateItem(position: Int) {
        notifyItemChanged(position, "payload")
    }

    fun setOnRecycleViewerItemChildViewClickListener(listener: OnRecycleViewerItemChildViewClickListener<T>) {
        this.listener = listener
    }
}
