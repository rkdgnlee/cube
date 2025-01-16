package com.tangoplus.tangoq.function

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tangoplus.tangoq.R
import io.github.douglasjunior.androidSimpleTooltip.OverlayView
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip

object TooltipManager {

    fun createGuide(
        context: Context,
        text: String,
        anchor: View,
        gravity: Int,
        dismiss: () -> Unit
    ) {
        SimpleTooltip.Builder(context).apply {
            anchorView(anchor)
            backgroundColor(ContextCompat.getColor(context, R.color.mainColor))
            arrowColor(Color.parseColor("#00FFFFFF"))
            gravity(gravity)
            animated(true)
            transparentOverlay(false)
            contentView(R.layout.tooltip)
            highlightShape( OverlayView.HIGHLIGHT_SHAPE_RECTANGULAR_ROUNDED)

            onShowListener {
                val tooltipTextView: TextView = it.findViewById(R.id.tooltip_instruction)
                tooltipTextView.text = text
            }
            onDismissListener {
                dismiss()
            }
            build()
                .show()
        }

    }
}