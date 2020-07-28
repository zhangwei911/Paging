package viz.commonlib.paging3

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil

/**
 * @title: DiffCallBack
 * @projectName Paging3SimpleWithNetWork
 * @description:
 * @author zhangwei
 * @date 2020/7/6 13:20
 */

class DiffCallBack<T> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem == newItem
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
}