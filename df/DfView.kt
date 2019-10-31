package rxaa.df

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.widget.LinearLayout
import java.nio.charset.CodingErrorAction.REPLACE
import android.graphics.RectF
import android.graphics.Region
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



open class DfView : LinearLayout {

    /**
     * 在xml布局文件中使用时自动调用
     * @param context
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        // TODO Auto-generated constructor stub

    }

    /**
     * 在java代码里new时调用
     * @param context
     */
    constructor(context: Context) : super(context) {
        // TODO Auto-generated constructor stub
    }



}