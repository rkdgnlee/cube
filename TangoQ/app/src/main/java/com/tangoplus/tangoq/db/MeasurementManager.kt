package com.tangoplus.tangoq.db

import org.json.JSONArray

object MeasurementManager {


    fun getDangerParts(measureInfo: MeasureInfo) : MutableList<Pair<String, Float>> {
        val dangerParts = mutableListOf<Pair<String, Float>>()

        val neckRisk = measureInfo.risk_neck.toFloat()
        if (neckRisk > 0) {
            dangerParts.add(Pair("목관절", neckRisk))
        }

        val shoulderLeftRisk = measureInfo.risk_shoulder_left.toFloat()
        if (shoulderLeftRisk > 0) {
            dangerParts.add(Pair("우측 어깨", shoulderLeftRisk))
        }

        val shoulderRightRisk = measureInfo.risk_shoulder_right.toFloat()
        if (shoulderRightRisk > 0) {
            dangerParts.add(Pair("좌측 어깨", shoulderRightRisk))
        }

        val elbowLeftRisk = measureInfo.risk_elbow_left.toFloat()
        if (elbowLeftRisk > 0) {
            dangerParts.add(Pair("좌측 팔꿉", elbowLeftRisk))
        }
        val elbowRightRisk =  measureInfo.risk_elbow_right.toFloat()
        if (elbowRightRisk > 0) {
            dangerParts.add(Pair("우측 팔꿉", elbowRightRisk))
        }

        val wristLeftRisk = measureInfo.risk_wrist_left.toFloat()
        if (wristLeftRisk > 0) {
            dangerParts.add(Pair("좌측 손목", wristLeftRisk))
        }
        val wristRightRisk = measureInfo.risk_wrist_right.toFloat()
        if (wristRightRisk > 0) {
            dangerParts.add(Pair("우측 손목", wristRightRisk))
        }

        val hipLeftRisk = measureInfo.risk_hip_left.toFloat()
        if (hipLeftRisk > 0) {
            dangerParts.add(Pair("좌측 골반", hipLeftRisk))
        }
        val hipRightRisk = measureInfo.risk_hip_right.toFloat()
        if (hipRightRisk > 0) {
            dangerParts.add(Pair("우측 골반", hipRightRisk))
        }

        val kneeLeftRisk = measureInfo.risk_knee_left.toFloat()
        if (kneeLeftRisk > 0) {
            dangerParts.add(Pair("좌측 무릎", kneeLeftRisk))
        }
        val kneeRightRisk = measureInfo.risk_knee_right.toFloat()
        if (kneeRightRisk > 0) {
            dangerParts.add(Pair("우측 무릎", kneeRightRisk))
        }

        val ankleLeftRisk = measureInfo.risk_ankle_left.toFloat()
        if (ankleLeftRisk > 0) {
            dangerParts.add(Pair("좌측 발목", ankleLeftRisk))
        }
        val ankleRightRisk = measureInfo.risk_ankle_right.toFloat()
        if (ankleRightRisk > 0) {
            dangerParts.add(Pair("우측 발목", ankleRightRisk))
        }

        return dangerParts
    }
    fun convertToJsonArrays(dangerParts: List<Pair<String, Float>>): Pair<JSONArray, JSONArray> {
        val partIndices = mapOf(
            "목관절" to 1,
            "어깨" to 2,
            "팔꿉" to 3,
            "손목" to 4,
            "골반" to 5,
            "무릎" to 6,
            "발목" to 7
        )

        val indices = mutableListOf<Int>()
        val values = mutableListOf<Float>()

        dangerParts.forEach { (part, value) ->
            val basicPart = when {
                part.contains("목관절") -> "목관절"
                part.contains("어깨") -> "어깨"
                part.contains("팔꿉") -> "팔꿉"
                part.contains("손목") -> "손목"
                part.contains("골반") -> "골반"
                part.contains("무릎") -> "무릎"
                part.contains("발목") -> "발목"
                else -> ""
            }

            // 매핑된 인덱스가 있으면 추가
            partIndices[basicPart]?.let { index ->
                indices.add(index)
                values.add(value)
            }
        }

        return Pair(JSONArray(indices), JSONArray(values))
    }
}