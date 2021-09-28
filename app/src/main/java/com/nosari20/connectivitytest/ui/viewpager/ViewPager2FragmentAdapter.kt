package com.nosari20.connectivitytest.ui.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter


class ViewPager2FragmentAdapter(
    fragment: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragment,lifecycle) {

    private val mFragmentList: MutableList<Fragment> = ArrayList()
    private val mFragmentTitleList: MutableList<String> = ArrayList()

    override fun createFragment(position: Int): Fragment {
        val fragment = mFragmentList[position]
        return fragment
    }

    override fun getItemCount(): Int {
        return mFragmentList.size
    }

    fun addFragment(fragment: Fragment, title: String) {
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }

    fun getPageTitle(position: Int): CharSequence? {
        return mFragmentTitleList[position]
    }

    fun getFragment(title: String) : Fragment? {
        val pos = mFragmentTitleList.indexOf(title)
        if (pos >= 0 )
            return mFragmentList[pos]
        else
            return null
    }
}
