using SQLite;
using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class t_measure_result_dynamic
{
    [PrimaryKey, AutoIncrement]
    public int sn { get; set; } = 0;
    public int device_sn { get; set; } = 0;
    public int server_sn { get; set; } = 0;
    public int measure_sn { get; set; } = 0;
    public string user_uuid { get; set; } = "";
    public int user_sn { get; set; } = 0;
    public String user_name { get; set; } = "";
    public int measure_seq { get; set; } = 0;
    public int measure_type { get; set; } = 0;
    public DateTime reg_date { get; set; } 
    public DateTime measure_start_time { get; set; }
    public DateTime measure_end_time { get; set; }
    public String measure_photo_file_name { get; set; } = "";
    public int measure_overlay_width { get; set; } = 0;
    public int measure_overlay_height { get; set; } = 0;
    public float measure_overlay_scale_factor_x { get; set; } = 1;
    public float measure_overlay_scale_factor_y { get; set; } = 1;

    public string measure_server_json_name { get; set; } = "";
    public string measure_server_file_name { get; set; } = "";

    /* measure result */
    // dynamic over head squat front horizontal
    public float ohs_front_horizontal_angle_elbow { get; set; } = 0;
    public float ohs_front_horizontal_distance_elbow { get; set; } = 0;
    public float ohs_front_horizontal_angle_shoulder { get; set; } = 0;
    public float ohs_front_horizontal_distance_shoulder { get; set; } = 0;
    public float ohs_front_horizontal_angle_hip { get; set; } = 0;
    public float ohs_front_horizontal_distance_hip { get; set; } = 0;
    public float ohs_front_horizontal_distance_center_left_hip { get; set; } = 0;
    public float ohs_front_horizontal_distance_center_right_hip { get; set; } = 0;
    public float ohs_front_horizontal_angle_knee { get; set; } = 0;
    public float ohs_front_horizontal_distance_knee { get; set; } = 0;
    public float ohs_front_horizontal_distance_center_left_knee { get; set; } = 0;
    public float ohs_front_horizontal_distance_center_right_knee { get; set; } = 0;
    public float ohs_front_horizontal_distance_center_left_toe { get; set; } = 0;
    public float ohs_front_horizontal_distance_center_right_toe { get; set; } = 0;

    // dynamic over head squat front vertical
    public float ohs_front_vertical_angle_wrist_elbow_shoulder_left { get; set; } = 0;
    public float ohs_front_vertical_angle_wrist_elbow_shoulder_right { get; set; } = 0;
    public float ohs_front_vertical_angle_wrist_elbow_left { get; set; } = 0;  
    public float ohs_front_vertical_angle_wrist_elbow_right { get; set; } = 0; 
    public float ohs_front_vertical_angle_elbow_shoulder_left { get; set; } = 0;
    public float ohs_front_vertical_angle_elbow_shoulder_right { get; set; } = 0;
    public float ohs_front_vertical_angle_hip_knee_toe_left { get; set; } = 0;
    public float ohs_front_vertical_angle_hip_knee_toe_right { get; set; } = 0;
    public float ohs_front_vertical_angle_hip_knee_left { get; set; } = 0;
    public float ohs_front_vertical_angle_hip_knee_right { get; set; } = 0;
    public float ohs_front_vertical_angle_knee_toe_left { get; set; } = 0;
    public float ohs_front_vertical_angle_knee_toe_right { get; set; } = 0;
    public float ohs_front_vertical_angle_ankle_toe_left { get; set; } = 0;
    public float ohs_front_vertical_angle_ankle_toe_right { get; set; } = 0;
    public float ohs_front_vertical_angle_knee_ankle_toe_left { get; set; } = 0;
    public float ohs_front_vertical_angle_knee_ankle_toe_right { get; set; } = 0;

    // dynamic over head squat front hand
    public float ohs_front_horizontal_angle_wrist { get; set; } = 0;
    public float ohs_front_horizontal_distance_wrist { get; set; } = 0;
    public float ohs_front_horizontal_distance_center_left_wrist { get; set; } = 0;
    public float ohs_front_horizontal_distance_center_right_wrist { get; set; } = 0;
    public float ohs_front_horizontal_angle_mid_finger_tip { get; set; } = 0;
    public float ohs_front_horizontal_distance_mid_finger_tip { get; set; } = 0;
    public float ohs_front_horizontal_distance_center_mid_finger_tip_left { get; set; } = 0;
    public float ohs_front_horizontal_distance_center_mid_finger_tip_right { get; set; } = 0;

    // dynamic over head squat side left
    public float ohs_side_left_angle_wrist_shoulder { get; set; } = 0;
    public float ohs_side_left_angle_shoulder_hip { get; set; } = 0;
    public float ohs_side_left_angle_wrist_shoulder_hip { get; set; } = 0;
    public float ohs_side_left_angle_knee_ankle { get; set; } = 0;
    public float ohs_side_left_angle_shoulder_hip_knee { get; set; } = 0;
    public float ohs_side_left_angle_hip_knee_ankle { get; set; } = 0;
    public float ohs_side_left_angle_knee_ankle_toe { get; set; } = 0;

    // dynamic over head squat side right
    public float ohs_side_right_angle_wrist_shoulder { get; set; } = 0;
    public float ohs_side_right_angle_shoulder_hip { get; set; } = 0;
    public float ohs_side_right_angle_wrist_shoulder_hip { get; set; } = 0;
    public float ohs_side_right_angle_knee_ankle { get; set; } = 0;
    public float ohs_side_right_angle_shoulder_hip_knee { get; set; } = 0;
    public float ohs_side_right_angle_hip_knee_ankle { get; set; } = 0;
    public float ohs_side_right_angle_knee_ankle_toe { get; set; } = 0;

    // dynamic over head squat back horizontal
    public float ohs_back_horizontal_angle_wrist { get; set; } = 0;
    public float ohs_back_horizontal_distance_wrist { get; set; } = 0;
    public float ohs_back_horizontal_angle_shoulder { get; set; } = 0;
    public float ohs_back_horizontal_distance_shoulder { get; set; } = 0;
    public float ohs_back_horizontal_angle_hip { get; set; } = 0;
    public float ohs_back_horizontal_distance_hip { get; set; } = 0;
    public float ohs_back_horizontal_angle_knee { get; set; } = 0;
    public float ohs_back_horizontal_distance_knee { get; set; } = 0;
    public float ohs_back_horizontal_distance_center_wrist_left { get; set; } = 0;
    public float ohs_back_horizontal_distance_center_wrist_right { get; set; } = 0;
    public float ohs_back_horizontal_distance_center_hip_left { get; set; } = 0;
    public float ohs_back_horizontal_distance_center_hip_right { get; set; } = 0;
    public float ohs_back_horizontal_distance_center_knee_left { get; set; } = 0;
    public float ohs_back_horizontal_distance_center_knee_right { get; set; } = 0;
    public float ohs_back_horizontal_angle_mid_finger_tip { get; set; } = 0;
    public float ohs_back_horizontal_distance_mid_finger_tip { get; set; } = 0;
    public float ohs_back_horizontal_distance_center_mid_finger_tip_left { get; set; } = 0;
    public float ohs_back_horizontal_distance_center_mid_finger_tip_right { get; set; } = 0;

    // dynamic over head squat back vertical
    public float ohs_back_vertical_angle_knee_heel_left { get; set; } = 0;
    public float ohs_back_vertical_angle_knee_heel_right { get; set; } = 0;
    public float ohs_back_vertical_angle_knee_ankle_heel_left { get; set; } = 0;
    public float ohs_back_vertical_angle_knee_ankle_heel_right { get; set; } = 0;

    // dynamic one leg stand front left horizontal
    public float ols_front_left_horizontal_angle_shoulder { get; set; } = 0;
    public float ols_front_left_horizontal_distance_shoulder { get; set; } = 0;
    public float ols_front_left_horizontal_angle_hip { get; set; } = 0;
    public float ols_front_left_horizontal_distance_hip { get; set; } = 0;

    // dynamic one leg stand front left vertical
    public float ols_front_left_vertical_angle_hip_knee { get; set; } = 0;
    public float ols_front_left_vertical_angle_knee_toe { get; set; } = 0;
    public float ols_front_left_vertical_angle_hip_knee_toe { get; set; } = 0;
    public float ols_front_left_vertical_angle_hip_knee_opposite { get; set; } = 0;
    public float ols_front_left_vertical_angle_knee_toe_opposite { get; set; } = 0;
    public float ols_front_left_vertical_angle_hip_knee_toe_opposite { get; set; } = 0;
    public float ols_front_left_vertical_distance_toe_opposite_toe { get; set; } = 0;

    // dynamic one leg stand front right horizontal
    public float ols_front_right_horizontal_angle_shoulder { get; set; } = 0;
    public float ols_front_right_horizontal_distance_shoulder { get; set; } = 0;
    public float ols_front_right_horizontal_angle_hip { get; set; } = 0;
    public float ols_front_right_horizontal_distance_hip { get; set; } = 0;

    // dynamic one leg stand front right vertical
    public float ols_front_right_vertical_angle_hip_knee { get; set; } = 0;
    public float ols_front_right_vertical_angle_knee_toe { get; set; } = 0;
    public float ols_front_right_vertical_angle_hip_knee_toe { get; set; } = 0;
    public float ols_front_right_vertical_angle_hip_knee_opposite { get; set; } = 0;
    public float ols_front_right_vertical_angle_knee_toe_opposite { get; set; } = 0;
    public float ols_front_right_vertical_angle_hip_knee_toe_opposite { get; set; } = 0;
    public float ols_front_right_vertical_distance_toe_opposite_toe { get; set; } = 0;


    [Ignore]
    public List<LandmarkCoordinateJson> pose_landmark { get; set; } = new List<LandmarkCoordinateJson>();
    [Ignore]
    public List<HandLandmarkResultJson> hand_landmark { get; set; } = new List<HandLandmarkResultJson>();


    public int uploaded { get; set; } = 0;
    public DateTime upload_date { get; set; }

    public int result_index { get; set; } = 0;

    public int uploaded_json { get; set; } = 0;
    public int uploaded_file { get; set; } = 0;
    public int uploaded_json_fail { get; set; } = 0;
    public int uploaded_file_fail { get; set; } = 0;

    public int used { get; set; } = 0;




    public void CopyValueFromServer(t_measure_result_dynamic mrd)
    {
        server_sn = mrd.sn;
        user_uuid = mrd.user_uuid;
        user_sn = mrd.user_sn;
        user_name = mrd.user_name;
        measure_seq = mrd.measure_seq;
        measure_type = mrd.measure_type;
        //reg_date = mrd.reg_date;
        //measure_start_time = mrd.measure_start_time;
        //measure_end_time = mrd.measure_end_time;
        //measure_photo_file_name = mrd.measure_photo_file_name;
        //measure_overlay_width = mrd.measure_overlay_width;
        //measure_overlay_height = mrd.measure_overlay_height;
        //measure_overlay_scale_factor_x = mrd.measure_overlay_scale_factor_x;
        //measure_overlay_scale_factor_y = mrd.measure_overlay_scale_factor_y;
        measure_server_file_name = mrd.measure_server_file_name;
        measure_server_json_name = mrd.measure_server_json_name;

        ohs_front_horizontal_angle_elbow = mrd.ohs_front_horizontal_angle_elbow;
        ohs_front_horizontal_distance_elbow = mrd.ohs_front_horizontal_distance_elbow;
        ohs_front_horizontal_angle_shoulder = mrd.ohs_front_horizontal_angle_shoulder;
        ohs_front_horizontal_distance_shoulder = mrd.ohs_front_horizontal_distance_shoulder;
        ohs_front_horizontal_angle_hip = mrd.ohs_front_horizontal_angle_hip;
        ohs_front_horizontal_distance_hip = mrd.ohs_front_horizontal_distance_hip;
        ohs_front_horizontal_distance_center_left_hip = mrd.ohs_front_horizontal_distance_center_left_hip;
        ohs_front_horizontal_distance_center_right_hip = mrd.ohs_front_horizontal_distance_center_right_hip;
        ohs_front_horizontal_angle_knee = mrd.ohs_front_horizontal_angle_knee;
        ohs_front_horizontal_distance_knee = mrd.ohs_front_horizontal_distance_knee;
        ohs_front_horizontal_distance_center_left_knee = mrd.ohs_front_horizontal_distance_center_left_knee;
        ohs_front_horizontal_distance_center_right_knee = mrd.ohs_front_horizontal_distance_center_right_knee;
        ohs_front_horizontal_distance_center_left_toe = mrd.ohs_front_horizontal_distance_center_left_toe;
        ohs_front_horizontal_distance_center_right_toe = mrd.ohs_front_horizontal_distance_center_right_toe;

        ohs_front_vertical_angle_wrist_elbow_shoulder_left = mrd.ohs_front_vertical_angle_wrist_elbow_shoulder_left;
        ohs_front_vertical_angle_wrist_elbow_shoulder_right = mrd.ohs_front_vertical_angle_wrist_elbow_shoulder_right;
        ohs_front_vertical_angle_wrist_elbow_left = mrd.ohs_front_vertical_angle_wrist_elbow_left;
        ohs_front_vertical_angle_wrist_elbow_right = mrd.ohs_front_vertical_angle_wrist_elbow_right;
        ohs_front_vertical_angle_elbow_shoulder_left = mrd.ohs_front_vertical_angle_elbow_shoulder_left;
        ohs_front_vertical_angle_elbow_shoulder_right = mrd.ohs_front_vertical_angle_elbow_shoulder_right;
        ohs_front_vertical_angle_hip_knee_toe_left = mrd.ohs_front_vertical_angle_hip_knee_toe_left;
        ohs_front_vertical_angle_hip_knee_toe_right = mrd.ohs_front_vertical_angle_hip_knee_toe_right;
        ohs_front_vertical_angle_hip_knee_left = mrd.ohs_front_vertical_angle_hip_knee_left;
        ohs_front_vertical_angle_hip_knee_right = mrd.ohs_front_vertical_angle_hip_knee_right;
        ohs_front_vertical_angle_knee_toe_left = mrd.ohs_front_vertical_angle_knee_toe_left;
        ohs_front_vertical_angle_knee_toe_right = mrd.ohs_front_vertical_angle_knee_toe_right;
        ohs_front_vertical_angle_ankle_toe_left = mrd.ohs_front_vertical_angle_ankle_toe_left;
        ohs_front_vertical_angle_ankle_toe_right = mrd.ohs_front_vertical_angle_ankle_toe_right;
        ohs_front_vertical_angle_knee_ankle_toe_left = mrd.ohs_front_vertical_angle_knee_ankle_toe_left;
        ohs_front_vertical_angle_knee_ankle_toe_right = mrd.ohs_front_vertical_angle_knee_ankle_toe_right;

        ohs_front_horizontal_angle_wrist = mrd.ohs_front_horizontal_angle_wrist;
        ohs_front_horizontal_distance_wrist = mrd.ohs_front_horizontal_distance_wrist;
        ohs_front_horizontal_distance_center_left_wrist = mrd.ohs_front_horizontal_distance_center_left_wrist;
        ohs_front_horizontal_distance_center_right_wrist = mrd.ohs_front_horizontal_distance_center_right_wrist;
        ohs_front_horizontal_angle_mid_finger_tip = mrd.ohs_front_horizontal_angle_mid_finger_tip;
        ohs_front_horizontal_distance_mid_finger_tip = mrd.ohs_front_horizontal_distance_mid_finger_tip;
        ohs_front_horizontal_distance_center_mid_finger_tip_left = mrd.ohs_front_horizontal_distance_center_mid_finger_tip_left;
        ohs_front_horizontal_distance_center_mid_finger_tip_right = mrd.ohs_front_horizontal_distance_center_mid_finger_tip_right;

        ohs_side_left_angle_wrist_shoulder = mrd.ohs_side_left_angle_wrist_shoulder;
        ohs_side_left_angle_shoulder_hip = mrd.ohs_side_left_angle_shoulder_hip;
        ohs_side_left_angle_wrist_shoulder_hip = mrd.ohs_side_left_angle_wrist_shoulder_hip;
        ohs_side_left_angle_knee_ankle = mrd.ohs_side_left_angle_knee_ankle;
        ohs_side_left_angle_shoulder_hip_knee = mrd.ohs_side_left_angle_shoulder_hip_knee;
        ohs_side_left_angle_hip_knee_ankle = mrd.ohs_side_left_angle_hip_knee_ankle;
        ohs_side_left_angle_knee_ankle_toe = mrd.ohs_side_left_angle_knee_ankle_toe;

        ohs_side_right_angle_wrist_shoulder = mrd.ohs_side_right_angle_wrist_shoulder;
        ohs_side_right_angle_shoulder_hip = mrd.ohs_side_right_angle_shoulder_hip;
        ohs_side_right_angle_wrist_shoulder_hip = mrd.ohs_side_right_angle_wrist_shoulder_hip;
        ohs_side_right_angle_knee_ankle = mrd.ohs_side_right_angle_knee_ankle;
        ohs_side_right_angle_shoulder_hip_knee = mrd.ohs_side_right_angle_shoulder_hip_knee;
        ohs_side_right_angle_hip_knee_ankle = mrd.ohs_side_right_angle_hip_knee_ankle;
        ohs_side_right_angle_knee_ankle_toe = mrd.ohs_side_right_angle_knee_ankle_toe;

        ohs_back_horizontal_angle_wrist = mrd.ohs_back_horizontal_angle_wrist;
        ohs_back_horizontal_distance_wrist = mrd.ohs_back_horizontal_distance_wrist;
        ohs_back_horizontal_angle_shoulder = mrd.ohs_back_horizontal_angle_shoulder;
        ohs_back_horizontal_distance_shoulder = mrd.ohs_back_horizontal_distance_shoulder;
        ohs_back_horizontal_angle_hip = mrd.ohs_back_horizontal_angle_hip;
        ohs_back_horizontal_distance_hip = mrd.ohs_back_horizontal_distance_hip;
        ohs_back_horizontal_angle_knee = mrd.ohs_back_horizontal_angle_knee;
        ohs_back_horizontal_distance_knee = mrd.ohs_back_horizontal_distance_knee;
        ohs_back_horizontal_distance_center_wrist_left = mrd.ohs_back_horizontal_distance_center_wrist_left;
        ohs_back_horizontal_distance_center_wrist_right = mrd.ohs_back_horizontal_distance_center_wrist_right;
        ohs_back_horizontal_distance_center_hip_left = mrd.ohs_back_horizontal_distance_center_hip_left;
        ohs_back_horizontal_distance_center_hip_right = mrd.ohs_back_horizontal_distance_center_hip_right;
        ohs_back_horizontal_distance_center_knee_left = mrd.ohs_back_horizontal_distance_center_knee_left;
        ohs_back_horizontal_distance_center_knee_right = mrd.ohs_back_horizontal_distance_center_knee_right;
        ohs_back_horizontal_angle_mid_finger_tip = mrd.ohs_back_horizontal_angle_mid_finger_tip;
        ohs_back_horizontal_distance_mid_finger_tip = mrd.ohs_back_horizontal_distance_mid_finger_tip;
        ohs_back_horizontal_distance_center_mid_finger_tip_left = mrd.ohs_back_horizontal_distance_center_mid_finger_tip_left;
        ohs_back_horizontal_distance_center_mid_finger_tip_right = mrd.ohs_back_horizontal_distance_center_mid_finger_tip_right;

        ohs_back_vertical_angle_knee_heel_left = mrd.ohs_back_vertical_angle_knee_heel_left;
        ohs_back_vertical_angle_knee_heel_right = mrd.ohs_back_vertical_angle_knee_heel_right;
        ohs_back_vertical_angle_knee_ankle_heel_left = mrd.ohs_back_vertical_angle_knee_ankle_heel_left;
        ohs_back_vertical_angle_knee_ankle_heel_right = mrd.ohs_back_vertical_angle_knee_ankle_heel_right;


        ols_front_left_horizontal_angle_shoulder = mrd.ols_front_left_horizontal_angle_shoulder;
        ols_front_left_horizontal_distance_shoulder = mrd.ols_front_left_horizontal_distance_shoulder;
        ols_front_left_horizontal_angle_hip = mrd.ols_front_left_horizontal_angle_hip;
        ols_front_left_horizontal_distance_hip = mrd.ols_front_left_horizontal_distance_hip;

        ols_front_left_vertical_angle_hip_knee = mrd.ols_front_left_vertical_angle_hip_knee;
        ols_front_left_vertical_angle_knee_toe = mrd.ols_front_left_vertical_angle_knee_toe;
        ols_front_left_vertical_angle_hip_knee_toe = mrd.ols_front_left_vertical_angle_hip_knee_toe;
        ols_front_left_vertical_angle_hip_knee_opposite = mrd.ols_front_left_vertical_angle_hip_knee_opposite;
        ols_front_left_vertical_angle_knee_toe_opposite = mrd.ols_front_left_vertical_angle_knee_toe_opposite;
        ols_front_left_vertical_angle_hip_knee_toe_opposite = mrd.ols_front_left_vertical_angle_hip_knee_toe_opposite;
        ols_front_left_vertical_distance_toe_opposite_toe = mrd.ols_front_left_vertical_distance_toe_opposite_toe;

        ols_front_right_horizontal_angle_shoulder = mrd.ols_front_right_horizontal_angle_shoulder;
        ols_front_right_horizontal_distance_shoulder = mrd.ols_front_right_horizontal_distance_shoulder;
        ols_front_right_horizontal_angle_hip = mrd.ols_front_right_horizontal_angle_hip;
        ols_front_right_horizontal_distance_hip = mrd.ols_front_right_horizontal_distance_hip;

        ols_front_right_vertical_angle_hip_knee = mrd.ols_front_right_vertical_angle_hip_knee;
        ols_front_right_vertical_angle_knee_toe = mrd.ols_front_right_vertical_angle_knee_toe;
        ols_front_right_vertical_angle_hip_knee_toe = mrd.ols_front_right_vertical_angle_hip_knee_toe;
        ols_front_right_vertical_angle_hip_knee_opposite = mrd.ols_front_right_vertical_angle_hip_knee_opposite;
        ols_front_right_vertical_angle_knee_toe_opposite = mrd.ols_front_right_vertical_angle_knee_toe_opposite;
        ols_front_right_vertical_angle_hip_knee_toe_opposite = mrd.ols_front_right_vertical_angle_hip_knee_toe_opposite;
        ols_front_right_vertical_distance_toe_opposite_toe = mrd.ols_front_right_vertical_distance_toe_opposite_toe;

        uploaded = mrd.uploaded;
        upload_date = mrd.upload_date;
        uploaded_json = mrd.uploaded_json;
        uploaded_file = mrd.uploaded_file;
        used = mrd.used;
    }


    public void setLandmarksInfo(LandmarkResult landmarkResult, List<HandLandmarkResult> handLandmark)
    {
        pose_landmark.Clear();
        for (int i = 0; i < landmarkResult.pose_landmark.Count; i++)
        {
            pose_landmark.Add(new LandmarkCoordinateJson(landmarkResult.pose_landmark[i]));
        }

        hand_landmark.Clear();
        for (int i = 0; i < handLandmark.Count; i++)
        {
            hand_landmark.Add(new HandLandmarkResultJson(handLandmark[i]));
        }
    }

    public void setResult(LandmarkResult landmarkResult, MEASURE_TYPE measure_type,
        List<HandLandmarkResult> resultsHand)
    {
        LandmarkCoordinate nose = landmarkResult.pose_landmark[(int)LANDMARK_NAME.NOSE];
        LandmarkCoordinate left_eye_inner = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_EYE_INNER];
        LandmarkCoordinate left_eye = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_EYE];
        LandmarkCoordinate left_eye_outer = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_EYE_OUTER];
        LandmarkCoordinate right_eye_inner = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_EYE_INNER];
        LandmarkCoordinate right_eye = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_EYE];
        LandmarkCoordinate right_eye_outer = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_EYE_OUTER];
        LandmarkCoordinate left_ear = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_EAR];
        LandmarkCoordinate right_ear = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_EAR];
        LandmarkCoordinate mouth_left = landmarkResult.pose_landmark[(int)LANDMARK_NAME.MOUTH_LEFT];
        LandmarkCoordinate mouth_right = landmarkResult.pose_landmark[(int)LANDMARK_NAME.MOUTH_RIGHT];
        LandmarkCoordinate left_shoulder = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_SHOULDER];
        LandmarkCoordinate right_shoulder = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_SHOULDER];
        LandmarkCoordinate left_elbow = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_ELBOW];
        LandmarkCoordinate right_elbow = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_ELBOW];
        LandmarkCoordinate left_wrist = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_WRIST];
        LandmarkCoordinate right_wrist = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_WRIST];
        LandmarkCoordinate left_pinky = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_PINKY];
        LandmarkCoordinate right_pinky = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_PINKY];
        LandmarkCoordinate left_index = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_INDEX];
        LandmarkCoordinate right_index = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_INDEX];
        LandmarkCoordinate left_thumb = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_THUMB];
        LandmarkCoordinate right_thumb = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_THUMB];
        LandmarkCoordinate left_hip = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_HIP];
        LandmarkCoordinate right_hip = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_HIP];
        LandmarkCoordinate left_knee = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_KNEE];
        LandmarkCoordinate right_knee = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_KNEE];
        LandmarkCoordinate left_ankle = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_ANKLE];
        LandmarkCoordinate right_ankle = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_ANKLE];
        LandmarkCoordinate left_heel = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_HEEL];
        LandmarkCoordinate right_heel = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_HEEL];
        LandmarkCoordinate left_foot_index = landmarkResult.pose_landmark[(int)LANDMARK_NAME.LEFT_FOOT_INDEX];
        LandmarkCoordinate right_foot_index = landmarkResult.pose_landmark[(int)LANDMARK_NAME.RIGHT_FOOT_INDEX];

        LandmarkCoordinate center_shoulder = new LandmarkCoordinate((int)LANDMARK_NAME.CENTER_SHOULDER,
            left_shoulder, right_shoulder);
        LandmarkCoordinate center_hip = new LandmarkCoordinate((int)LANDMARK_NAME.CENTER_HIP,
            left_hip, right_hip);
        LandmarkCoordinate center_ankle = new LandmarkCoordinate((int)LANDMARK_NAME.CENTER_ANKLE,
            left_ankle, right_ankle);

        HandLandmarkResult hand_left = null;
        HandLandmarkResult hand_right = null;
        for (int i = 0; resultsHand != null && i < resultsHand.Count; i++)
        {
            if (resultsHand[i].left_right == 0) hand_left = resultsHand[i];
            else hand_right = resultsHand[i];
        }

        if(measure_type == MEASURE_TYPE.MT_DYNAMIC_OVERHEADSQUAT_FRONT)
        {
            // horizontal
            ohs_front_horizontal_angle_wrist = MathHelpers.getAngle2D180(left_wrist, right_wrist);
            ohs_front_horizontal_distance_wrist = MathHelpers.getDistanceY(left_wrist, right_wrist);
            ohs_front_horizontal_angle_elbow = MathHelpers.getAngle2D180(left_elbow, right_elbow);
            ohs_front_horizontal_distance_elbow = MathHelpers.getDistanceY(left_elbow, right_elbow);
            ohs_front_horizontal_angle_shoulder = MathHelpers.getAngle2D180(left_shoulder, right_shoulder);
            ohs_front_horizontal_distance_shoulder = MathHelpers.getDistanceY(left_shoulder, right_shoulder);
            ohs_front_horizontal_angle_hip = MathHelpers.getAngle2D180(left_hip, right_hip);
            ohs_front_horizontal_distance_hip = MathHelpers.getDistanceY(left_hip, right_hip);
            ohs_front_horizontal_angle_knee = MathHelpers.getAngle2D180(left_knee, right_knee);
            ohs_front_horizontal_distance_knee = MathHelpers.getDistanceY(left_knee, right_knee);
            ohs_front_horizontal_distance_center_left_wrist = MathHelpers.getDistanceX(center_ankle, left_wrist);
            ohs_front_horizontal_distance_center_right_wrist = MathHelpers.getDistanceX(center_ankle, right_wrist);
            ohs_front_horizontal_distance_center_left_hip = MathHelpers.getDistanceX(center_ankle, left_hip);
            ohs_front_horizontal_distance_center_right_hip = MathHelpers.getDistanceX(center_ankle, right_hip);
            ohs_front_horizontal_distance_center_left_knee = MathHelpers.getDistanceX(center_ankle, left_knee);
            ohs_front_horizontal_distance_center_right_knee = MathHelpers.getDistanceX(center_ankle, right_knee);
            ohs_front_horizontal_distance_center_left_toe = MathHelpers.getDistanceX(center_ankle, left_foot_index);
            ohs_front_horizontal_distance_center_right_toe = MathHelpers.getDistanceX(center_ankle, right_foot_index);

            // vertical
            ohs_front_vertical_angle_wrist_elbow_shoulder_left = MathHelpers.getAngle3Point2D(left_wrist, left_elbow, left_shoulder);
            ohs_front_vertical_angle_wrist_elbow_shoulder_right = MathHelpers.getAngle3Point2D(right_wrist, right_elbow, right_shoulder);
            ohs_front_vertical_angle_wrist_elbow_left = MathHelpers.getAngle2D180(left_wrist, left_elbow);
            ohs_front_vertical_angle_wrist_elbow_right = MathHelpers.getAngle2D180(right_wrist, right_elbow, true);
            ohs_front_vertical_angle_elbow_shoulder_left = MathHelpers.getAngle2D180(left_elbow, left_shoulder);
            ohs_front_vertical_angle_elbow_shoulder_right = MathHelpers.getAngle2D180(right_elbow, right_shoulder, true);
            ohs_front_vertical_angle_hip_knee_toe_left = MathHelpers.getAngle3Point2D(left_hip, left_knee, left_foot_index);
            ohs_front_vertical_angle_hip_knee_toe_right = MathHelpers.getAngle3Point2D(right_hip, right_knee, right_foot_index);
            ohs_front_vertical_angle_hip_knee_left = MathHelpers.getAngle2D180(left_hip, left_knee);
            ohs_front_vertical_angle_hip_knee_right = MathHelpers.getAngle2D180(right_hip, right_knee, true);
            ohs_front_vertical_angle_knee_toe_left = MathHelpers.getAngle2D180(left_knee, left_foot_index);
            ohs_front_vertical_angle_knee_toe_right = MathHelpers.getAngle2D180(right_knee, right_foot_index, true);
            ohs_front_vertical_angle_ankle_toe_left = MathHelpers.getAngle2D180(left_ankle, left_foot_index);
            ohs_front_vertical_angle_ankle_toe_right = MathHelpers.getAngle2D180(right_ankle, right_foot_index, true);
            ohs_front_vertical_angle_knee_ankle_toe_left = MathHelpers.getAngle3Point2D(left_knee, left_ankle, left_foot_index);
            ohs_front_vertical_angle_knee_ankle_toe_right = MathHelpers.getAngle3Point2D(right_knee, right_ankle, right_foot_index);

            // hand
            if(hand_left != null)
            {
                ohs_front_horizontal_distance_center_mid_finger_tip_left = MathHelpers.getDistanceX(
                    center_ankle, hand_left.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_TIP]);
            }
            if(hand_right != null)
            {
                ohs_front_horizontal_distance_center_mid_finger_tip_right = MathHelpers.getDistanceX(
                    center_ankle, hand_right.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_TIP]);
            }
            if(hand_left != null && hand_right != null)
            {
                ohs_front_horizontal_angle_mid_finger_tip = MathHelpers.getAngle2D180(
                    hand_left.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_TIP],
                    hand_right.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_TIP]);
                ohs_front_horizontal_distance_mid_finger_tip = MathHelpers.getDistanceY(
                    hand_left.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_TIP],
                    hand_right.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_TIP]);
            }
        }
        else if(measure_type == MEASURE_TYPE.MT_DYNAMIC_OVERHEADSQUAT_SIDE_LEFT)
        {

        }
    }

    public void setResultDynamicOHS(int result_index, f_dynamic_ohs_front result)
    {
        this.result_index = result_index;
        ohs_front_horizontal_angle_wrist = result.horizontal_angle_wrist;
        ohs_front_horizontal_distance_wrist = result.horizontal_distance_wrist;
        ohs_front_horizontal_angle_elbow = result.horizontal_angle_elbow;
        ohs_front_horizontal_distance_elbow = result.horizontal_distance_elbow;
        ohs_front_horizontal_angle_shoulder = result.horizontal_angle_shoulder;
        ohs_front_horizontal_distance_shoulder = result.horizontal_distance_shoulder;
        ohs_front_horizontal_angle_hip = result.horizontal_angle_hip;
        ohs_front_horizontal_distance_hip = result.horizontal_distance_hip;
        ohs_front_horizontal_angle_knee = result.horizontal_angle_knee;
        ohs_front_horizontal_distance_knee = result.horizontal_distance_knee;
        ohs_front_horizontal_distance_center_left_wrist = result.horizontal_distance_center_left_wrist;
        ohs_front_horizontal_distance_center_right_wrist = result.horizontal_distance_center_right_wrist;
        ohs_front_horizontal_distance_center_left_hip = result.horizontal_distance_center_left_hip;
        ohs_front_horizontal_distance_center_right_hip = result.horizontal_distance_center_right_hip;
        ohs_front_horizontal_distance_center_left_knee = result.horizontal_distance_center_left_knee;
        ohs_front_horizontal_distance_center_right_knee = result.horizontal_distance_center_right_knee;
        ohs_front_horizontal_distance_center_left_toe = result.horizontal_distance_center_left_toe;
        ohs_front_horizontal_distance_center_right_toe = result.horizontal_distance_center_right_toe;

        ohs_front_vertical_angle_wrist_elbow_shoulder_left = result.vertical_angle_wrist_elbow_shoulder_left;
        ohs_front_vertical_angle_wrist_elbow_shoulder_right = result.vertical_angle_wrist_elbow_shoulder_right;
        ohs_front_vertical_angle_wrist_elbow_left = result.vertical_angle_wrist_elbow_left;
        ohs_front_vertical_angle_wrist_elbow_right = result.vertical_angle_wrist_elbow_right;
        ohs_front_vertical_angle_elbow_shoulder_left = result.vertical_angle_elbow_shoulder_left;
        ohs_front_vertical_angle_elbow_shoulder_right = result.vertical_angle_elbow_shoulder_right;
        ohs_front_vertical_angle_hip_knee_toe_left = result.vertical_angle_hip_knee_toe_left;
        ohs_front_vertical_angle_hip_knee_toe_right = result.vertical_angle_hip_knee_toe_right;
        ohs_front_vertical_angle_hip_knee_left = result.vertical_angle_hip_knee_left;
        ohs_front_vertical_angle_hip_knee_right = result.vertical_angle_hip_knee_right;
        ohs_front_vertical_angle_knee_toe_left = result.vertical_angle_knee_toe_left;
        ohs_front_vertical_angle_knee_toe_right = result.vertical_angle_knee_toe_right;

        ohs_front_horizontal_angle_mid_finger_tip = result.horizontal_angle_mid_finger_tip;
        ohs_front_horizontal_distance_mid_finger_tip = result.horizontal_distance_mid_finger_tip;
        ohs_front_horizontal_distance_center_mid_finger_tip_left = result.horizontal_distance_center_mid_finger_tip_left;
        ohs_front_horizontal_distance_center_mid_finger_tip_right = result.horizontal_distance_center_mid_finger_tip_right;

        pose_landmark = result.pose_landmark;
        hand_landmark = result.hand_landmark;
    }
}
