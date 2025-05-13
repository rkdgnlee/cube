package com.tangoplus.tangoq.adapter

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.vo.DataDynamicVO
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

            val isFrontCamera = setAdapter(currentItem.indexx, holder, currentItem)
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

    private fun setAdapter(partIndex: Int, holder: MDAIViewHolder, analysis: AnalysisVO) : Boolean {
        if (partIndex == 1) {
            return setDynamicAdapter(holder)
        } else {
            val layoutManager = LinearLayoutManager(fragment.requireContext(), LinearLayoutManager.VERTICAL, false)
            val adapter = MainPartAnalysisRVAdapter(fragment, analysis, avm)
            holder.rvMDAI.layoutManager = layoutManager
            holder.rvMDAI.adapter = adapter
            // true 로 보내야 정적측정에서는 그냥 flip
            return false
        }
    }

    private fun setDynamicAdapter(holder: MDAIViewHolder) : Boolean {

        // avm.currentPart.value을 통해 만약 dynamic이 속했으면 해당 joint만 추출
        val connections = if (avm.currentPart.value?.contains("어깨") == true) {
            listOf(15, 16)
        } else if (avm.currentPart.value?.contains("골반") == true) {
            listOf(23, 24)
        } else if (avm.currentPart.value?.contains("무릎")== true) {
            listOf(25, 26)
        } else {
            listOf(15, 16, 23, 24, 25, 26)
        }
        val titleList = if (avm.currentPart.value?.contains("어깨") == true) {
            listOf("좌측 어깨", "우측 어깨")
        } else if (avm.currentPart.value?.contains("골반") == true) {
            listOf("좌측 골반", "우측 골반")
        } else if (avm.currentPart.value?.contains("무릎")== true) {
            listOf("좌측 무릎", "우측 무릎")
        } else {
            listOf("좌측 어깨", "우측 어깨", "좌측 골반", "우측 골반", "좌측 무릎", "우측 무릎")
        }

        val coordinates = extractVideoCoordinates(avm.mdMeasureResult)
        val dataDynamicVOList = mutableListOf<DataDynamicVO>()

        for (i in connections.indices step 2) {
            val connection1 = connections[i]
            val connection2 = connections[i + 1]
            val title1 = titleList[i]
            val title2 = titleList[i + 1]

            val filteredCoordinate1 = mutableListOf<Pair<Float, Float>>()
            val filteredCoordinate2 = mutableListOf<Pair<Float, Float>>()

            for (element in coordinates) {
                // 단순히 해당 인덱스의 좌표를 가져와서 추가
                filteredCoordinate1.add(element[connection1])
                filteredCoordinate2.add(element[connection2])
            }

            val dataDynamicVO = DataDynamicVO(
                data1 = filteredCoordinate1,
                title1 = title1,
                data2 = filteredCoordinate2,
                title2 = title2
            )
            dataDynamicVOList.add(dataDynamicVO)
        }

        // data 는 6개
        val isFrontCamera = judgeFrontCameraByDynamic(coordinates)
        Log.v("쿨디네이츠", "${coordinates.size}, $isFrontCamera")
        val linearLayoutManager1 = LinearLayoutManager(fragment.requireContext(), LinearLayoutManager.VERTICAL, false)
        val dynamicAdapter = DataDynamicRVAdapter(dataDynamicVOList, isFrontCamera)
        holder.rvMDAI.layoutManager = linearLayoutManager1
        holder.rvMDAI.adapter = dynamicAdapter
        return isFrontCamera
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