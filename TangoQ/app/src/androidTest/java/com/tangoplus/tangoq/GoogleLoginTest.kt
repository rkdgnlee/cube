package com.tangoplus.tangoq

import android.widget.Button
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleLoginTest {

    @Rule
    @JvmField
    val activityScenarioRule = ActivityScenarioRule(IntroActivity::class.java)

    @Test
    fun testGoogleLoginIntroActivity() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.findViewById<Button>(R.id.ibtnGoogleLogin_)

        }

    }
}