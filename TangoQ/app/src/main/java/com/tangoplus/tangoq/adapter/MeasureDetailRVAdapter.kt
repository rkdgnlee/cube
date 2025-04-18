package com.tangoplus.tangoq.adapter

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvMeasureDetailAnalysisItemBinding
import com.tangoplus.tangoq.dialog.PoseDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.MeasurementManager.createSeqGuideComment
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.function.MeasurementManager.judgeFrontCameraByDynamic
import com.tangoplus.tangoq.function.MeasurementManager.seqs
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.vo.AnalysisVO
import java.security.MessageDigest

class MeasureDetailRVAdapter(private val fragment: Fragment, private val data : List<AnalysisVO>, private val avm: AnalysisViewModel) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class MDAIViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMDAITitle :TextView = view.findViewById(R.id.tvMDAITitle)
        val tvMDAIExplain : TextView = view.findViewById(R.id.tvMDAIExplain)
        val ivMDAI : ImageView = view.findViewById(R.id.ivMDAI)
        val rvMDAI: RecyclerView = view.findViewById(R.id.rvMDAI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMeasureDetailAnalysisItemBinding.inflate(inflater, parent, false)
        return MDAIViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MDAIViewHolder) {
            val currentItem = data[position]

            holder.tvMDAITitle.text = seqs[currentItem.indexx]
            holder.tvMDAIExplain.text = createSeqGuideComment(currentItem.indexx)

            val isFrontCamera = setAdapter(currentItem.indexx, holder, currentItem.labels)
            Glide.with(fragment.requireContext())
                .load(currentItem.url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(
                    // 후방카메라일 때만 flip되게끔
                    if (!isFrontCamera) {
                        RequestOptions.bitmapTransform(
                            MultiTransformation(
                                CenterCrop(),
                                FlipHorizontalTransformation()
                            )
                        )
                    } else {
                        RequestOptions.bitmapTransform(
                            MultiTransformation(
                                CenterCrop(),
                            )
                        )
                    }
                )
                .into(holder.ivMDAI)

            holder.ivMDAI.setOnSingleClickListener {
                val dialog = PoseDialogFragment.newInstance(currentItem.indexx)
                dialog.show(fragment.requireActivity().supportFragmentManager, "MainPartPoseDialogFragment")
            }

        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    private fun setAdapter(partIndex: Int, holder: MDAIViewHolder, analysisVO: List<AnalysisUnitVO>) : Boolean {
        if (partIndex == 1) {
            val connections = listOf(
                15, 16, 23, 24, 25, 26 // 좌측 골반의 pose번호를 가져옴
            )
            val coordinates = extractVideoCoordinates(avm.mdMeasureResult)
            val filteredCoordinates = mutableListOf<List<Pair<Float, Float>>>()
            for (connection in connections) {
                val filteredCoordinate = mutableListOf<Pair<Float, Float>>() // 부위 하나당 몇 십 프레임의 x,y 좌표임
                for (element in coordinates) {
                    if (connection == 23) {
                        val a = element[connection]
                        val b = element[connection]
                        val midHip = Pair((a.first + b.first) / 2, (a.second + b.second) / 2)
                        filteredCoordinate.add(midHip)
                    } else {
                        filteredCoordinate.add(element[connection]) // element의 23, 24가 각각 담김
                    }
                }
                filteredCoordinates.add(filteredCoordinate)
            }
            // data 는 6개
            val isFrontCamera = judgeFrontCameraByDynamic(filteredCoordinates)
            val linearLayoutManager1 = LinearLayoutManager(fragment.requireContext(), LinearLayoutManager.VERTICAL, false)
            val dynamicAdapter = DataDynamicRVAdapter(filteredCoordinates, avm.dynamicTitles, isFrontCamera)
            holder.rvMDAI.layoutManager = linearLayoutManager1
            holder.rvMDAI.adapter = dynamicAdapter
            return isFrontCamera
        } else {
            val layoutManager = LinearLayoutManager(fragment.requireContext(), LinearLayoutManager.VERTICAL, false)
            val adapter = MainPartAnalysisRVAdapter(fragment, analysisVO)
            holder.rvMDAI.layoutManager = layoutManager
            holder.rvMDAI.adapter = adapter
            // true 로 보내야 정적측정에서는 그냥 flip
            return false
        }
    }

    inner class FlipHorizontalTransformation : BitmapTransformation() {
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update("flip_horizontal".toByteArray())
        }
        override fun transform(
            pool: BitmapPool,
            toTransform: Bitmap,
            outWidth: Int,
            outHeight: Int
        ): Bitmap {
            val matrix = Matrix().apply { postScale(-1f, 1f, toTransform.width / 2f, toTransform.height / 2f) }
            return Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
        }
    }

}