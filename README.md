# Paging
Android Jetpack Paging封装[![Download](https://api.bintray.com/packages/viz/VCommon/paging/images/download.svg)](https://bintray.com/viz/VCommon/paging/_latestVersion)

## gradle集成
```
allprojects {
    repositories {
        jcenter()
        //审核中,使用以下地址
        maven{
            url "https://dl.bintray.com/viz/VCommon"
        }
    }
}

dependencies {
    implementation "viz.commonlib:paging:1.0.0"
    //项目中包含了okhttp3,gson,retrofit2,如果想使用其他版本,请使用以下方式
//    implementation("viz.commonlib:paging:1.0.0"){
//    	exclude group: 'com.squareup.okhttp3',module:'okhttp'
//    	exclude group: 'com.squareup.okhttp3',module:'logging-interceptor'
//    	exclude group: 'com.squareup.retrofit2',module:'retrofit'
//    	exclude group: 'com.squareup.retrofit2',module:'converter-gson'
//    	exclude group: 'com.google.code.gson',module:'gson'
//    }
}
```

## 使用方法
参照demo中MainFragment类

```
private lateinit var model: CommonViewModel<ResultBean, InnerDataBean, QueryEntity>
private var adapter: CommonPostsAdapter<InnerDataBean>? = null
private var currentPage = 0
    
//实例化ViewModel
model = CommonViewModel.getViewModel(requireActivity(),
    CommonPostRepository.Type.IN_MEMORY_BY_ITEM, { query, limit ->
        //请求首页
        currentPage = 0
        HttpUtil.createHttp().getData(currentPage, limit)
    }, { query, after, limit ->
        //请求下一页
        HttpUtil.createHttp().getData(currentPage, limit)
    },{ errorEntity ->
        Log.e("TAG",errorEntity.toString())
    }, { resultBean ->
        //resultBean为请求返回的数据
        currentPage++
        //对列表需要使用的数据进行处理,这里为了方便直接使用的InnerDataBean
        val listInner = resultBean.data
        DataBean(items = listInner)
    },
    //对after进行赋值的get方法名,这里使用的是InnerDataBean.getName
    keyMethodName = "getName", 
    //用来区分ViewModel,不同Fragment中使用应不同,除非共享数据
    key = "MainFragment"
    )
       
//列表数据绑定设置
model.posts.observe(viewLifecycleOwner, Observer {
    adapter?.submitList(it)
})
        
//网络状态设置
model.networkState.observe(viewLifecycleOwner, Observer {
    adapter?.setNetworkState(it)
})
initSwipeToRefresh()

//实例化监听器
adapter = CommonPostsAdapter(
    R.layout.item,
    createViewHolder,
    { model.retry() }
)
//列表子项点击事件监听器
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

//初始化swipeRefreshLayout
private fun initSwipeToRefresh() {
    model.refreshState.observe(this, Observer {
        swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
    })
    swipeRefreshLayout.setOnRefreshListener {
        model.refresh()
    }
}

//创建ViewHolder
private val createViewHolder: (parent: ViewGroup, layoutId: Int, listener: OnRecycleViewerItemChildViewClickListener<InnerDataBean>) -> androidx.recyclerview.widget.RecyclerView.ViewHolder =
{ parent, layoutId, listener ->
    object : CommonViewHolder<InnerDataBean>(parent, layoutId, listener = listener) {
        override fun bindTo(data: InnerDataBean?, position: Int) {
            //子项数据设置
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
        //子项数据更新时调用,即调用adapter.notifyItemChanged(int position, @Nullable Object payload)时
        }

    }
}
```
