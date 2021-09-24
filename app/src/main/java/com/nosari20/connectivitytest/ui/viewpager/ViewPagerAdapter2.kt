package com.nosari20.connectivitytest.ui.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ViewPagerAdapter2(fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {


    private val mFragmentList: MutableList<Fragment> = ArrayList()
    private val mFragmentTitleList: MutableList<String> = ArrayList()

    override fun getItem(position: Int): Fragment {
        return mFragmentList[position]
    }

    override fun getCount(): Int {
        return mFragmentList.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mFragmentTitleList[position]
    }
}