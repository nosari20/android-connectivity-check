package com.nosari20.connectivitytest.ui.checklist


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nosari20.connectivitytest.ConnectivityTest
import com.nosari20.connectivitytest.ConnectivityTestListAdapter
import com.nosari20.connectivitytest.R
import java.net.Socket


class CheckListFragment(private val list: List<ConnectivityTest>) : Fragment() {


    private lateinit var testlist: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)

        val root: View = inflater.inflate(R.layout.fragment_checklist, container, false)

        testlist = root.findViewById<RecyclerView>(R.id.test_list)
        testlist.apply {
            layoutManager = LinearLayoutManager(this.context)
            adapter = ConnectivityTestListAdapter(list)
        }


        val swipeLayout =  root.findViewById<SwipeRefreshLayout>(R.id.swipe_layout)

        swipeLayout.setOnRefreshListener {
            val thread = Thread {
                for (test: ConnectivityTest in list) {
                    test.status = ConnectivityTest.Status.PENDING
                    val sub_thread = Thread {
                        check_tcp(test)
                    }
                    sub_thread.start()
                }

            }
            swipeLayout.setRefreshing(false);
            thread.start()
        }

        return root
    }


    fun check_tcp(test: ConnectivityTest) {
        try {
            getActivity()?.runOnUiThread(java.lang.Runnable {
                testlist.adapter?.notifyDataSetChanged()
            })
            val before = System.currentTimeMillis()
            val client = Socket(test.host, test.port)
            client.soTimeout = 5
            client.close()
            val after = System.currentTimeMillis()
            test.status = ConnectivityTest.Status.OK
            test.info = "Time: " + (after-before) + "ms (" + client.inetAddress.hostAddress+")"
        } catch(e: Exception) {
            test.status = ConnectivityTest.Status.KO
            test.info = e.localizedMessage
        } finally {
            getActivity()?.runOnUiThread(java.lang.Runnable {
                testlist.adapter?.notifyDataSetChanged()
            })
        }
    }
}