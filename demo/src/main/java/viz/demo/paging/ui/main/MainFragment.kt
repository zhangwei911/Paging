package viz.demo.paging.ui.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.item.*
import kotlinx.android.synthetic.main.item.view.*
import kotlinx.android.synthetic.main.main_fragment.*
import viz.commonlib.paging.CommonPostRepository
import viz.commonlib.paging.DataBean
import viz.demo.paging.entity.QueryEntity
import viz.commonlib.paging.CommonViewModel
import viz.commonlib.paging.CommonPostsAdapter
import viz.commonlib.paging.CommonViewHolder
import viz.commonlib.paging.NetworkState
import viz.commonlib.paging.OnRecycleViewerItemChildViewClickListener
import viz.demo.paging.HttpUtil
import viz.demo.paging.R
import viz.demo.paging.bean.InnerDataBean
import viz.demo.paging.bean.ResultBean

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var model: CommonViewModel<ResultBean, InnerDataBean, QueryEntity>
    private var adapter: CommonPostsAdapter<InnerDataBean>? = null
    private var currentPage = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        autoPage()
        initViews()
        model.showQuery(QueryEntity())
    }

    private fun initViews() {
        recycelView.layoutManager = LinearLayoutManager(requireContext())
        recycelView.adapter = adapter
    }

    private fun autoPage() {
        model = CommonViewModel.getViewModel(requireActivity(),
                CommonPostRepository.Type.IN_MEMORY_BY_ITEM, { query, limit ->
            currentPage = 0
            HttpUtil.createHttp().getData(currentPage, limit)
        }, { query, after, limit ->
            HttpUtil.createHttp().getData(currentPage, limit)
        }, { resultBean ->
            currentPage++
            val listInner = resultBean.data
            DataBean(items = listInner)
        }, keyMethodName = "getName", key = "MainFragment"
        )

        model.posts.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                return@Observer
            }
            if (it.size > 0) {
                adapter?.submitList(it)
            }
        })
        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter?.setNetworkState(it)
        })
        initSwipeToRefresh()

        adapter = CommonPostsAdapter(
                R.layout.item,
                createViewHolder,
                { model.retry() }
        )
        adapter?.setOnRecycleViewerItemChildViewClickListener(object :
                OnRecycleViewerItemChildViewClickListener<InnerDataBean> {
            override fun onChildViewClick(view: View, data: InnerDataBean, position: Int) {
                when (view.id) {
                    R.id.constraintLayout -> {
                        Toast.makeText(requireContext(), "点击ConstraintLayout $position ${data.name}", Toast.LENGTH_LONG).show()
                    }
                    R.id.textView -> {
                        Toast.makeText(requireContext(), "点击TextView $position ${data.name}", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                    }
                }
            }

            override fun onChildViewLongClick(
                    view: View,
                    data: InnerDataBean,
                    position: Int
            ): Boolean {
                return true
            }
        })
    }

    private fun initSwipeToRefresh() {
        model.refreshState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
        })
        swipeRefreshLayout.setOnRefreshListener {
            model.refresh()
        }
    }

    private val createViewHolder: (parent: ViewGroup, layoutId: Int, listener: OnRecycleViewerItemChildViewClickListener<InnerDataBean>) -> androidx.recyclerview.widget.RecyclerView.ViewHolder =
            { parent, layoutId, listener ->
                object : CommonViewHolder<InnerDataBean>(parent, layoutId, listener = listener) {
                    override fun bindTo(data: InnerDataBean?, position: Int) {
                        data?.let {
                            itemView.apply {
                                constraintLayout.setOnClickListener {
                                    listener.onChildViewClick(it, data, position)
                                }
                                constraintLayout.setOnLongClickListener {
                                    listener.onChildViewLongClick(it, data, position)
                                }
                                for (i in 0 until constraintLayout.childCount) {
                                    val childView = constraintLayout.getChildAt(i)
                                    childView.setOnClickListener {
                                        listener.onChildViewClick(it, data, position)
                                    }
                                    childView.setOnLongClickListener {
                                        listener.onChildViewLongClick(it, data, position)
                                    }
                                }

                                textView.text = data.name
                            }
                        }
                    }

                    override fun update(data: InnerDataBean?, position: Int) {
                    }

                }
            }
}