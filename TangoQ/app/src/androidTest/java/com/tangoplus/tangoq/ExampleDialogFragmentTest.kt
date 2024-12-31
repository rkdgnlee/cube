package com.tangoplus.tangoq

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tangoplus.tangoq.dialog.MeasureSkeletonDialogFragment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ExampleDialogFragmentTest {

    @Rule
    @JvmField
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testMeasureSkeletonDialogFragment() {
        activityScenarioRule.scenario.onActivity { activity ->
            val dialogFragment = MeasureSkeletonDialogFragment()
            dialogFragment.show(activity.supportFragmentManager, "MeasureSkeletonDialogFragmentManager")
        }
        // 대화 상자가 표시되고 내용이 올바른지 확인하기 위해 여기에 어설션을 추가.
//        onView(withId(R.id.vpMSD)).check(matches(isDisplayed()))
//        onView(withId(R.id.btnMSDConfirm)).check(matches(isDisplayed()))

        // 선택적으로 대화상자와 상호작용.
//        onView(withId(R.id.btnMSDConfirm)).perform(click())



    }
}