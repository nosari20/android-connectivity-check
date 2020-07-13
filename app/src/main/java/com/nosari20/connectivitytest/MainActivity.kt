package com.nosari20.connectivitytest

import android.content.Context
import android.content.RestrictionsManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var testList: ArrayList<ConnectivityTest>;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        testList = arrayListOf<ConnectivityTest>();


        // Hard coded
        testList.add(ConnectivityTest("accounts.google.com", 443))
        testList.add(ConnectivityTest("accounts.google.fr", 443))
        testList.add(ConnectivityTest("clients1.google.com", 443))
        testList.add(ConnectivityTest("play.google.com", 443))
        testList.add(ConnectivityTest("fcm.googleapis.com", 443))
        testList.add(ConnectivityTest("fcm-xmpp.googleapis.com", 5228))
        testList.add(ConnectivityTest("fcm-xmpp.googleapis.com", 5229))
        testList.add(ConnectivityTest("fcm-xmpp.googleapis.com", 5230))
        testList.add(ConnectivityTest("mtalk.google.com", 5228))
        testList.add(ConnectivityTest("mtalk.google.com", 5229))
        testList.add(ConnectivityTest("mtalk.google.com", 5230))
        testList.add(ConnectivityTest("lh1.ggpht.com", 443))
        testList.add(ConnectivityTest("lh2.ggpht.com", 443))
        testList.add(ConnectivityTest("lh3.ggpht.com", 443))
        testList.add(ConnectivityTest("lh4.ggpht.com", 443))
        testList.add(ConnectivityTest("lh5.ggpht.com", 443))
        testList.add(ConnectivityTest("lh6.ggpht.com", 443))
        testList.add(ConnectivityTest("lh1.googleusercontent.com", 443))
        testList.add(ConnectivityTest("lh2.googleusercontent.com", 443))
        testList.add(ConnectivityTest("lh3.googleusercontent.com", 443))
        testList.add(ConnectivityTest("lh4.googleusercontent.com", 443))
        testList.add(ConnectivityTest("lh5.googleusercontent.com", 443))
        testList.add(ConnectivityTest("lh6.googleusercontent.com", 443))


        // Managed configurations
        var myRestrictionsMgr = this?.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        var appRestrictions: Bundle = myRestrictionsMgr.applicationRestrictions
        val parcelables: Array<out Parcelable>? = appRestrictions.getParcelableArray("test_list")
        if (parcelables?.isNotEmpty() == true) {
            // iterate parcelables and cast as bundle
            parcelables.map { it as Bundle }.forEach { testBundle ->
                // parse bundle data and store in VpnConfig array
               testList.add(
                   ConnectivityTest(
                       testBundle.getString("test_hostname").toString(),
                       testBundle.getInt("test_port")
                )
               )
            }
        }


        test_list.apply {
            layoutManager = LinearLayoutManager(this.context)
            adapter = ConnectivityTestListAdapter(testList)
        }

        swipe_layout.setOnRefreshListener {
            val thread = Thread {
                for (test: ConnectivityTest in testList) {
                    test.status = ConnectivityTest.Status.PENDING
                    val sub_thread = Thread {
                        check_tcp(test)
                    }
                    sub_thread.start()
                }

            }
            swipe_layout.setRefreshing(false);
            thread.start()
        }
    }

    fun check_tcp(test: ConnectivityTest) {
        try {
            this@MainActivity.runOnUiThread(java.lang.Runnable {
                test_list.adapter?.notifyDataSetChanged()
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
            this@MainActivity.runOnUiThread(java.lang.Runnable {
                test_list.adapter?.notifyDataSetChanged()
            })
        }
    }
}
