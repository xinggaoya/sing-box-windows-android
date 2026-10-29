package cn.moncn.sing_box_windows.v2.data.core

/** 内核启动结果 */
data class CoreStartResult(
    val ok: Boolean,
    val error: String? = null
)
