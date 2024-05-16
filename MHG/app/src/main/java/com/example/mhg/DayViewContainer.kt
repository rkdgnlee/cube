package com.example.mhg

import android.view.View
import com.example.mhg.databinding.CalendarDayLayoutBinding
import com.kizitonwose.calendar.view.ViewContainer

class DayViewContainer(view: View) : ViewContainer(view){
    var date = CalendarDayLayoutBinding.bind(view).calendarDayText

}