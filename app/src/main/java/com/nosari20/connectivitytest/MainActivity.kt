package com.nosari20.connectivitytest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.tabs.TabLayoutMediator
import com.nosari20.connectivitytest.ui.checklist.CheckListFragment
import com.nosari20.connectivitytest.ui.viewpager.ViewPager2FragmentAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    private val testList = Configuration


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testList.loadManagedConfigurations(applicationContext);

        val adapter = ViewPager2FragmentAdapter(supportFragmentManager, lifecycle)


        Configuration.all().get("custom")?.let { CheckListFragment(it) }?.let {
            adapter.addFragment(
                it,"Custom")
        }


        Configuration.all().get("google")?.let { CheckListFragment(it) }?.let {
            adapter.addFragment(
                it,"Android")
        }

        Configuration.all().get("knox")?.let { CheckListFragment(it) }?.let {
            adapter.addFragment(
                it,"Knox")
        }

        Configuration.all().get("apple")?.let { CheckListFragment(it) }?.let {
            adapter.addFragment(
                it,"Apple")
        }

        Configuration.all().get("tests")?.let { CheckListFragment(it) }?.let {
            adapter.addFragment(
                it,"Tests")
        }


        viewpager.adapter = adapter


        TabLayoutMediator(tabs, viewpager) { tab, position ->
            tab.text = (viewpager.adapter as ViewPager2FragmentAdapter).getPageTitle(position)
        }.attach()



    }
}
