package com.example.mhg

import android.graphics.Canvas
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.Adapter.AlarmRecyclerViewAdapter
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import kotlin.math.min

class ItemTouchCallback(private val listener: AddItemTouchListener): ItemTouchHelper.Callback() {
    interface AddItemTouchListener {
        fun onItemMoved(from: Int, to: Int)
        fun onItemSwiped(position: Int)
    }
    private var currentPosition: Int? = null
    private var previousPosition: Int? = null
    private var currentDx = 0f
    private var clamp = 0f
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // 드래그 방향
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        // 스와이프 방향
        val swipeFlags = ItemTouchHelper.LEFT
        // 드래그 이동을 만드는 함수
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        listener.onItemMoved(viewHolder.adapterPosition, target.adapterPosition)

        return false // 드래그 앤 드롭 동작이 성공적으로 완료됐을 때 true를 반환해야 함.
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onItemSwiped(viewHolder.adapterPosition)
    }
//    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
//        currentDx = 0f
//        previousPosition = viewHolder.adapterPosition
//        getDefaultUIUtil().clearView(getView(viewHolder))
//    }
//    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
//        viewHolder?.let {
//            currentPosition = viewHolder.adapterPosition
//            getDefaultUIUtil().onSelected(getView(it))
//        }
//    }
//
//    override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float {
//        val isClamped = getTag(viewHolder)
//        // 현재 View가 고정되어있지 않고 사용자가 -clamp 이상 swipe시 isClamped true로 변경 아닐시 false로 변경
//        setTag(viewHolder, !isClamped && currentDx <= -clamp)
//        return 2f
//    }
//    override fun onChildDraw(
//        c: Canvas,
//        recyclerView: RecyclerView,
//        viewHolder: RecyclerView.ViewHolder,
//        dX: Float,
//        dY: Float,
//        actionState: Int,
//        isCurrentlyActive: Boolean
//    ) {
//        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
//            val view = getView(viewHolder)
//            val isClamped = getTag(viewHolder)      // 고정할지 말지 결정, true : 고정함 false : 고정 안 함
//            val newX = clampViewPositionHorizontal(dX, isClamped, isCurrentlyActive)  // newX 만큼 이동(고정 시 이동 위치/고정 해제 시 이동 위치 결정)
//
//            // 고정시킬 시 애니메이션 추가
//            if (newX == -clamp) {
//                getView(viewHolder).animate().translationX(-clamp).setDuration(80L).start()
//                return
//            }
//            if (viewHolder.adapterPosition != previousPosition) {
//                removePreviousClamp(recyclerView)
//            }
//            currentDx = newX
//            getDefaultUIUtil().onDraw(
//                c,
//                recyclerView,
//                view,
//                newX,
//                dY,
//                actionState,
//                isCurrentlyActive
//            )
//        }
//    }
//    // swipe_view 를 swipe 했을 때 <삭제> 화면이 보이도록 고정
//    private fun clampViewPositionHorizontal(
//        dX: Float,
//        isClamped: Boolean,
//        isCurrentlyActive: Boolean
//    ) : Float {
//        // RIGHT 방향으로 swipe 막기
//        val max = 0f
//
//        // 고정할 수 있으면
//        val newX = if (isClamped) {
//            // 현재 swipe 중이면 swipe되는 영역 제한
//            if (isCurrentlyActive)
//            // 오른쪽 swipe일 때
//                if (dX < 0) dX / 3 - clamp
//                // 왼쪽 swipe일 때
//                else dX / 3 - clamp
//            // swipe 중이 아니면 고정시키기
//            else -clamp
//        }
//        // 고정할 수 없으면 newX는 스와이프한 만큼
//        else dX / 3
//
//        // newX가 0보다 작은지 확인
//        return min(newX, max)
//    }
//    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
//        return defaultValue * 10
//    }
//
//    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
//        setTag(viewHolder, currentDx <= -clamp)
//        return 2f
//    }
//
//    private fun setTag(viewHolder: RecyclerView.ViewHolder, isClamped: Boolean) { viewHolder.itemView.tag = isClamped }
//    private fun getTag(viewHolder: RecyclerView.ViewHolder) : Boolean =  viewHolder.itemView.tag as? Boolean ?: false
//
//    fun setClamp(clamp: Float) {
//        this.clamp = clamp
//    }
//
//    private fun getView(viewHolder: RecyclerView.ViewHolder): View {
//        return (viewHolder as HomeVerticalRecyclerViewAdapter.addViewHolder).itemView.findViewById(R.id.clAdd)
//    }
//
//    fun removePreviousClamp(recyclerView: RecyclerView) {
//        if (currentPosition == previousPosition)
//            return
//        previousPosition?.let {
//            val viewHolder = recyclerView.findViewHolderForAdapterPosition(it) ?: return
//            getView(viewHolder).translationX = 0f
//            setTag(viewHolder, false)
//            previousPosition = null
//        }
//    }
}