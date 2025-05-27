package com.tangoplus.tangoq.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "t_measure_info")
data class MeasureInfo(
    @PrimaryKey(autoGenerate = true) val mobile_info_sn: Int = 0, // 실제 키오스크의 measureSn 과 같은 역할 (로컬 DB의 measureSn)
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
    var risk_result_ment: String? = "",
    var kakao_send_count: Int? = 0,
    var kakao_send_date : String? = "0000-00-00 00:00:00",
    var show_lines: Int? = 1
    ) {
    companion object {
        fun getCurrentDateTime(): String =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
}