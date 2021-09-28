package com.nosari20.connectivitytest

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ConnectivityTestListAdapter(private val list: List<ConnectivityTest>, private  val onLongClick : Handler?)
    : RecyclerView.Adapter<ConnectivityTestListAdapter.ConnectivityTestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectivityTestViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = ConnectivityTestViewHolder(
            inflater,
            parent
        )
        return view
    }

    override fun onBindViewHolder(holder: ConnectivityTestViewHolder, position: Int) {
        val test: ConnectivityTest = list[position]
        if (onLongClick != null)
            holder.setOnLongClickHandler(onLongClick)
        holder.bind(test)
    }

    override fun getItemCount(): Int = list.size


    class ConnectivityTestViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.list_item_test, parent, false)) {


        companion object {
            val KEY_POSITION: String = "position"
        }
        private var mTitleView: TextView? = null
        private var mStatusView: TextView? = null
        private var mStatusOKView: ImageView? = null
        private var mStatusKOView: ImageView? = null
        private var mStatusPENDINGView: ProgressBar? = null
        private var mStatusUNKNOWNView: ImageView? = null


        init {
            mTitleView = itemView.findViewById(R.id.list_item_title)
            mStatusView = itemView.findViewById(R.id.list_item_status)
            mStatusOKView = itemView.findViewById(R.id.list_item_ok)
            mStatusKOView = itemView.findViewById(R.id.list_item_ko)
            mStatusPENDINGView = itemView.findViewById(R.id.list_item_loading)
            mStatusUNKNOWNView = itemView.findViewById(R.id.list_item_unknown)
        }

        fun bind(test: ConnectivityTest) {
            mTitleView?.text = test.host + ":" + test.port
            mStatusView?.text = test.info



            when (test.status) {
                ConnectivityTest.Status.OK -> {
                    mStatusPENDINGView?.visibility = View.GONE;
                    mStatusKOView?.visibility = View.GONE;
                    mStatusOKView?.visibility = View.VISIBLE;
                    mStatusUNKNOWNView?.visibility = View.GONE;
                }
                ConnectivityTest.Status.KO -> {
                    mStatusPENDINGView?.visibility = View.GONE;
                    mStatusOKView?.visibility = View.GONE;
                    mStatusKOView?.visibility = View.VISIBLE;
                    mStatusUNKNOWNView?.visibility = View.GONE;
                }
                ConnectivityTest.Status.PENDING -> {
                    mStatusPENDINGView?.visibility = View.VISIBLE;
                    mStatusOKView?.visibility = View.GONE;
                    mStatusKOView?.visibility = View.GONE;
                    mStatusUNKNOWNView?.visibility = View.GONE;
                }
                else -> {
                    mStatusPENDINGView?.visibility = View.GONE;
                    mStatusOKView?.visibility = View.GONE;
                    mStatusKOView?.visibility = View.GONE;
                    mStatusUNKNOWNView?.visibility = View.VISIBLE;

                }
            }
        }

        fun setOnLongClickHandler(onLongClick: Handler) {
            this.itemView.setOnLongClickListener(View.OnLongClickListener {
                val bundle = Bundle()
                bundle.putString(ConnectivityTestListAdapter.ConnectivityTestViewHolder.KEY_POSITION,""+absoluteAdapterPosition)
                val message = Message()
                message.data = bundle
                onLongClick.dispatchMessage(message)
                true
            })
        }
    }

}