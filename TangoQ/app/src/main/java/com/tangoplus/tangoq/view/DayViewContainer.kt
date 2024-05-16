package com.tangoplus.tangoq.view

import android.view.View
import com.kizitonwose.calendar.view.ViewContainer
import com.tangoplus.tangoq.databinding.CalendarDayLayoutBinding

class DayViewContainer(view: View) : ViewContainer(view){
    var date = CalendarDayLayoutBinding.bind(view).calendarDayText

}