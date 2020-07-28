package viz.demo.paging.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item.view.*
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import viz.commonlib.paging3.CommonViewHolder
import viz.commonlib.paging3.OnRecycleViewerItemChildViewClickListener
import viz.commonlib.paging3.DataBean
import viz.commonlib.paging3.CommonPagingDataAdapter
import viz.commonlib.paging3.CommonViewModel
import viz.commonlib.paging3.repository.CommonPostRepository
import viz.demo.paging.HttpUtil
import viz.demo.paging.R
import viz.demo.paging.bean.InnerDataBean
import viz.demo.paging.bean.ResultBean
import viz.demo.paging.entity.QueryEntity

/**
 * @title: Paging3Fragment
 * @projectName PagingLib
 * @description:
 * @author zhangwei
 * @date 2020/7/6 14:21
 */
class Paging3Fragment : Fragment() {

    companion object {
        fun newInstance() = Paging3Fragment()
    }

    private lateinit var model: CommonViewModel<ResultBean, InnerDataBean, QueryEntity>
    private var adapter: CommonPagingDataAdapter<InnerDataBean, RecyclerView.ViewHolder>? = null
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
        model = CommonViewModel.getViewModel<String, ResultBean, InnerDataBean, QueryEntity>(
                requireActivity(),
                CommonPostRepository.Type.IN_MEMORY_BY_PAGE,
                { query, before, after, limit ->
                    HttpUtil.createHttp().getData2(currentPage, limit)
                },
                formatItems = { resultBean: ResultBean ->
                    //这里会接受最新的数据,并进行组装成你想要的数据
                    currentPage++
                    val listInner = resultBean.data
                    DataBean(items = listInner, after = if (listInner.size == 0) {
                        null
                    } else {
                        currentPage.toString()
                    })
                }, keyMethodName = "getName", key = "Paging3Fragment", savedState = true
        )
        adapter = CommonPagingDataAdapter(
                R.layout.item,
                createViewHolder,
                { adapter?.retry() }
        )

        lifecycleScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            adapter?.loadStateFlow?.collectLatest { loadStates ->
                swipeRefreshLayout.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }
        lifecycleScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            model.posts.collectLatest {
                adapter?.submitData(it)
            }
        }

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
        initSwipeToRefresh()
    }

    private fun initSwipeToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            adapter?.refresh()
        }
    }

    private val createViewHolder: (parent: ViewGroup, layoutId: Int, listener: OnRecycleViewerItemChildViewClickListener<InnerDataBean>) -> RecyclerView.ViewHolder =
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