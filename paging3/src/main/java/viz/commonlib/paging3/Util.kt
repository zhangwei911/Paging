package viz.commonlib.paging3

import android.os.Parcelable
import android.util.Log

/**
 * @title: Util
 * @projectName PagingLib
 * @description:
 * @author zhangwei
 * @date 2020/7/6 15:32
 */
fun <E : Parcelable> getValue(item: E?, keyMethodName: String): String {
    try {
        if (item == null) {
            return ""
        }
        val method = item::class.java.getMethod(keyMethodName)
        val keyMethod = method.invoke(item)
        if (keyMethod != null) {
            return keyMethod.toString()
        }
    } catch (e: Exception) {
        Log.e("getValue", e.message ?: "")
    }
    return ""
}