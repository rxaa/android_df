package rxaa.df

/**
 * 将Int dp值转换为适应屏幕的px值
 */
fun Int.dp2px(): Int {
    return df.dp2px(this.toFloat())
}