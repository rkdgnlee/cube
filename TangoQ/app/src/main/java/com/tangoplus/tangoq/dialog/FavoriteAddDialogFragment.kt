package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.FavoriteItemVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteAddDialogBinding
import com.tangoplus.tangoq.fragment.FavoriteFragment
import com.tangoplus.tangoq.`object`.NetworkFavoriteService.insertFavoriteItemJson
import com.tangoplus.tangoq.`object`.Singleton_t_user
import org.json.JSONObject


class FavoriteAddDialogFragment : DialogFragment() {
    lateinit var binding : FragmentFavoriteAddDialogBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteAddDialogBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val t_userData = Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("data")
        val user_mobile = t_userData?.optString("user_mobile")

        binding.etFrDFName.setOnTouchListener{ v, event ->
            val DRAWABLE_RIHGT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.etFrDFName.right - binding.etFrDFName.compoundDrawables[DRAWABLE_RIHGT].bounds.width())) {

                    binding.etFrDFName.text.clear()
                    return@setOnTouchListener true
                }
            }
            false
        }
        binding.etFrDFDescript.setOnTouchListener{ v, event ->
            val DRAWABLE_RIHGT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.etFrDFDescript.right - binding.etFrDFDescript.compoundDrawables[DRAWABLE_RIHGT].bounds.width())) {

                    binding.etFrDFDescript.text.clear()
                    return@setOnTouchListener true
                }
            }
            false
        }
        binding.tvFrDFCancel.setOnClickListener { dismiss() }
        binding.tvFrDFConfirm.setOnClickListener {
            val jsonObj = JSONObject()
            jsonObj.put("favorite_name", binding.etFrDFName.text)
            jsonObj.put("favorite_description", binding.etFrDFDescript.text)
            jsonObj.put("user_mobile", user_mobile)
            Log.v("즐겨찾기JSON", "$jsonObj")

            insertFavoriteItemJson(getString(R.string.IP_ADDRESS_t_favorite), jsonObj.toString()) { responseJson ->
//                val newFavoriteItem = FavoriteItemVO(
//                    favoriteSn = responseJson!!.getInt("favorite_sn"),
//                    favoriteName = responseJson.optString("favorite_name"),
//                    favoriteExplain = responseJson.optString("favorite_description"),
//                    exercises = mutableListOf(),
//                    imgThumbnailList = mutableListOf(),
//                )
                val data = responseJson?.getJSONArray("seletedData")?.optJSONObject(0)?.optJSONObject("data")
                val newFavoriteItem = FavoriteItemVO(
                    favoriteSn = data!!.getInt("favorite_sn"),
                    favoriteName = data.optString("favorite_name"),
                    favoriteExplain = data.optString("favorite_description"),
                    exercises = mutableListOf(),
                    imgThumbnailList = mutableListOf(),
                )
//                requireActivity().runOnUiThread {
//                    val updatedList = viewModel.favoriteList.value?.toMutableList() ?: mutableListOf()
//                    updatedList.add(newFavoriteItem)
//                    viewModel.favoriteList.value = updatedList
//                    Log.v("newFavoriteItem", "newFavoriteItem: $newFavoriteItem")
//                    dismiss()
//                }
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flMain, FavoriteFragment())
                        .commit()
                }
                Log.v("newFavoriteItem", "newFavoriteItem: $newFavoriteItem")
                dismiss()
            }
        }


    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
}