package com.tangoplus.tangoq.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "t_measure_info")
data class MeasureInfo(
    @PrimaryKey(autoGenerate = true) val mobile_info_sn: Int = 0, // 실제 기기의 measureSn 과 같은 역할 (로컬 기기의 measureSn)
    val sn: Int? = null, // 132받아옴 -> 현재 132측정 1개 -> 모바일에서 측정함 -> null -> server로 업로드 -> sn 수정 ->
    val device_sn: Int = 0,
    val measure_sn: Int = 0,
    val user_uuid: String? = "",
    val mobile_device_uuid : String? = "",
    val user_sn: Int,
    val user_name: String? = "",
    val measure_date: String? = "",
    val elapsed_time: String? = "",
    val measure_seq: Int,

    val pain_part_neck: String? = "0",
    val pain_part_left_shoulder: String? = "0",
    val pain_part_right_shoulder: String? = "0",
    val pain_part_left_elbow: String? = "0",
    val pain_part_right_elbow: String? = "0",
    val pain_part_left_wrist: String? = "0",
    val pain_part_right_wrist: String? = "0",
    val pain_part_waist: String? = "0",
    val pain_part_left_hip_joint: String? = "0",
    val pain_part_right_hip_joint: String? = "0",
    val pain_part_left_knee: String? = "0",
    val pain_part_right_knee: String? = "0",
    val pain_part_left_ankle: String? = "0",
    val pain_part_right_ankle: String? = "0",
    var uploaded: String? = "0",
    var upload_date: String? = getCurrentDateTime(),
    val used: String? = "0",
    val modify_date: String? = getCurrentDateTime(),
    val t_score: String? = "0",
    var risk_neck: String? = "0",
    var risk_shoulder_left: String? = "0",
    var risk_shoulder_right: String? = "0",
    var risk_elbow_left: String? = "0",
    var risk_elbow_right: String? = "0",
    var risk_wrist_left: String? = "0",
    var risk_wrist_right: String? = "0",
    var risk_hip_left: String? = "0",
    var risk_hip_right: String? = "0",
    var risk_knee_left: String? = "0",
    var risk_knee_right: String? = "0",
    var risk_ankle_left: String? = "0",
    var risk_ankle_right: String? = "0",
    var risk_result_ment: String? = ""
    ) {
    companion object {
        fun getCurrentDateTime(): String =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

//    // TODO 랜덤으로 부위 넣는 함수는 분석부분이 만들어지면 삭제 예정
//    fun setRandomRiskValues() {
//        val riskFields = mutableListOf(
//            "risk_neck",
//            "risk_shoulder_left",
//            "risk_shoulder_right",
//            "risk_elbow_left",
//            "risk_elbow_right",
//            "risk_wrist_left",
//            "risk_wrist_right",
//            "risk_hip_left",
//            "risk_hip_right",
//            "risk_knee_left",
//            "risk_knee_right",
//            "risk_ankle_left",
//            "risk_ankle_right"
//        )
//
//        // 랜덤으로 2~4개 선택
//        val randomCount = (2..4).random()
//        val selectedFields = riskFields.shuffled().take(randomCount)
//
//
//        selectedFields.forEach { field ->
//            when (field) {
//                "risk_neck" -> this.risk_neck = (1..2).random().toString()
//                "risk_shoulder_left" -> this.risk_shoulder_left = (1..2).random().toString()
//                "risk_shoulder_right" -> this.risk_shoulder_right = (1..2).random().toString()
//                "risk_elbow_left" -> this.risk_elbow_left = (1..2).random().toString()
//                "risk_elbow_right" -> this.risk_elbow_right = (1..2).random().toString()
//                "risk_wrist_left" -> this.risk_wrist_left = (1..2).random().toString()
//                "risk_wrist_right" -> this.risk_wrist_right = (1..2).random().toString()
//                "risk_hip_left" -> this.risk_hip_left = (1..1).random().toString()
//                "risk_hip_right" -> this.risk_hip_right = (1..1).random().toString()
//                "risk_knee_left" -> this.risk_knee_left = (1..2).random().toString()
//                "risk_knee_right" -> this.risk_knee_right = (1..2).random().toString()
//                "risk_ankle_left" -> this.risk_ankle_left = (1..1).random().toString()
//                "risk_ankle_right" -> this.risk_ankle_right = (1..1).random().toString()
//            }
//        }
//    }
}