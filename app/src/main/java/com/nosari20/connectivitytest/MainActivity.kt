package com.nosari20.connectivitytest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.View.OnLongClickListener
import com.google.android.material.tabs.TabLayoutMediator
import com.nosari20.connectivitytest.ui.checklist.CheckListFragment
import com.nosari20.connectivitytest.ui.dialog.AddTestDialogFragment
import com.nosari20.connectivitytest.ui.viewpager.ViewPager2FragmentAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    private val testList = Configuration
    private val activity = this


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        testList.loadManagedConfigurations(applicationContext);
        testList.loadLocalConfigurations(this)

        val adapter = ViewPager2FragmentAdapter(supportFragmentManager, lifecycle)

        val onLongClick = object:  Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {

                val  pos = Integer.parseInt(""+msg.data.get(ConnectivityTestListAdapter.ConnectivityTestViewHolder.KEY_POSITION))

                val localTests = testList.all().get("local")?.toMutableList()
                if (localTests != null) {
                    localTests.removeAt(pos)
                }
                testList.update("local", localTests as ArrayList<ConnectivityTest>)
                testList.applyLocalConfigurations(activity)
                (adapter.getFragment("Custom") as CheckListFragment).removeFromList(pos)

            }
        }


        Configuration.all().get("local")?.let { CheckListFragment(it, onLongClick) }?.let {
            adapter.addFragment(
                it,"Custom")
        }

        Configuration.all().get("managed")?.let { CheckListFragment(it, null) }?.let {
            adapter.addFragment(
                it,"AppConfig")
        }


        Configuration.all().get("google")?.let { CheckListFragment(it, null) }?.let {
            adapter.addFragment(
                it,"Android")
        }

        viewpager.adapter = adapter


        TabLayoutMediator(tabs, viewpager) { tab, position ->
            tab.text = (viewpager.adapter as ViewPager2FragmentAdapter).getPageTitle(position)
        }.attach()


        val addNewHandler = object:  Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val bundle = msg.data
                val hostname = bundle.getString(AddTestDialogFragment.KEY_HOSTNAME)+""
                val port = bundle.getInt(AddTestDialogFragment.KEY_PORT)
                val ssl = bundle.getBoolean(AddTestDialogFragment.KEY_SSL)

                val localTests = testList.all().get("local")?.toMutableList()
                if (localTests != null) {
                    val test = ConnectivityTest(
                        hostname,
                        port,
                        ssl
                    )
                    localTests.add(test)
                    testList.update("local", localTests as ArrayList<ConnectivityTest>)
                    testList.applyLocalConfigurations(activity)
                    (adapter.getFragment("Custom") as CheckListFragment).addToList(test)
                }



            }


        }
        val dialog = AddTestDialogFragment(addNewHandler)

        val fab: View = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            dialog.show(supportFragmentManager, "NoticeDialogFragment")
        }



    }

}
