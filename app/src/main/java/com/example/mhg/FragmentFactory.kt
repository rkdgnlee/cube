package com.example.mhg

import androidx.fragment.app.Fragment

class FragmentFactory {
    companion object {
        val FRAGMENT_HOME_BEGINNER = "home_beginner"
        val FRAGMENT_HOME_EXPERT = "home_expert"
        val FRAGMENT_HOME_INTERMEDIATE = "home_intermediate"
        val FRAGMENT_REPORT_SKELETON = "report_skeleton"
        val FRAGMENT_REPORT_DETAIL = "report_detail"
        val FRAGMENT_REPORT_GOAL = "report_goal"
        val FRAGMENT_PICK = "pick"
        val FRAGMENT_PICK_DETAIL = "pick_detail"
        val FRAGMENT_PROFILE = "profile"

        fun createFragmentById(id: String): Fragment {
            return when (id) {
                FRAGMENT_HOME_BEGINNER -> HomeBeginnerFragment()
                FRAGMENT_HOME_INTERMEDIATE -> HomeIntermediateFragment()
                FRAGMENT_HOME_EXPERT -> HomeExpertFragment()
                FRAGMENT_REPORT_SKELETON -> ReportSkeletonFragment()
                FRAGMENT_REPORT_DETAIL -> ReportDetailFragment()
                FRAGMENT_REPORT_GOAL -> ReportGoalFragment()
                FRAGMENT_PICK -> PickFragment()
                FRAGMENT_PICK_DETAIL -> PickDetailFragment()
                FRAGMENT_PROFILE -> ProfileFragment()
                else -> throw IllegalArgumentException("Unknown fragment ID: $id")
            }
        }
    }

}