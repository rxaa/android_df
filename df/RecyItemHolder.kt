package rxaa.df

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import org.jetbrains.annotations.NotNull

class RecyItemHolder(val view: ViewEx) : RecyclerView.ViewHolder(view.getView()) {


}

class RecyAdapter(val list: ListViewEx<*>) : RecyclerView.Adapter<RecyItemHolder>() {

    override fun getItemCount(): Int {
        return list.count() + list.headViewList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyItemHolder {
        df.catchLog {
            if (viewType <= list.headViewType)
                return RecyItemHolder(list.headViewList[list.headViewType - viewType])
        }
        return RecyItemHolder(list.onCreateView!!(viewType))
    }

    override fun getItemViewType(position: Int): Int {
        df.catchLog {
            if (list.headViewList.size > 0 && position < list.headViewList.size) {
                return list.headViewType - position;
            }

            return list.getViewType(position - list.headViewList.size)
        }
        return 0
    }

    override fun onBindViewHolder(holder: RecyItemHolder, position: Int) {
        df.catchLog {
            if (position >= list.headViewList.size) {
                list.onBindView(holder.view, position - list.headViewList.size)
            } else {

            }
        }
    }

}