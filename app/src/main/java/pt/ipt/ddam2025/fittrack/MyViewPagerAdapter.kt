package pt.ipt.ddam2025.fittrack

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import pt.ipt.ddam2025.fittrack.fragments.Profile
import pt.ipt.ddam2025.fittrack.fragments.Start
import pt.ipt.ddam2025.fittrack.fragments.Training

class MyViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 3
    }
    override fun createFragment(position: Int): Fragment {
        when(position) {
            0 -> return Start()
            1 -> return Training()
            2 -> return Profile()
            else -> return Start()
        }
    }
}