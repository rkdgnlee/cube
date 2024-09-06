using Google.Protobuf.Reflection;
using Newtonsoft.Json;
using SQLite;
using System;
using System.Collections;
using System.Collections.Generic;
using Unity.VisualScripting;
using UnityEngine;


public class t_measure_result_static
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

    public string measure_server_file_name { get; set; } = "";
    public string measure_server_json_name { get; set; } = "";


    /* measure result */
    // static front horizontal
    public float front_horizontal_angle_ear { get; set; } = 0;
    public float front_horizontal_distance_sub_ear { get; set; } = 0;
    public float front_horizontal_angle_shoulder { get; set; } = 0;
    public float front_horizontal_distance_sub_shoulder { get; set; } = 0;
    public float front_horizontal_angle_elbow { get; set; } = 0;
    public float front_horizontal_distance_sub_elbow { get; set; } = 0;
    public float front_horizontal_angle_wrist { get; set; } = 0;
    public float front_horizontal_distance_sub_wrist { get; set; } = 0;
    public float front_horizontal_angle_hip { get; set; } = 0;
    public float front_horizontal_distance_sub_hip { get; set; } = 0;
    public float front_horizontal_angle_knee { get; set; } = 0;
    public float front_horizontal_distance_sub_knee { get; set; } = 0;
    public float front_horizontal_angle_ankle { get; set; } = 0;
    public float front_horizontal_distance_sub_ankle { get; set; } = 0;
    public float front_horizontal_distance_wrist_left { get; set; } = 0;
    public float front_horizontal_distance_wrist_right { get; set; } = 0;
    public float front_horizontal_distance_knee_left { get; set; } = 0;
    public float front_horizontal_distance_knee_right { get; set; } = 0;
    public float front_horizontal_distance_ankle_left { get; set; } = 0;
    public float front_horizontal_distance_ankle_right { get; set; } = 0;
    public float front_horizontal_distance_toe_left { get; set; } = 0;
    public float front_horizontal_distance_toe_right { get; set; } = 0;

    // static front vertical
    public float front_vertical_angle_shoulder_elbow_left { get; set; } = 0;
    public float front_vertical_angle_shoulder_elbow_right { get; set; } = 0;
    public float front_vertical_angle_elbow_wrist_left { get; set; } = 0;
    public float front_vertical_angle_elbow_wrist_right { get; set; } = 0;
    public float front_vertical_angle_shoulder_elbow_wrist_left { get; set; } = 0;
    public float front_vertical_angle_shoulder_elbow_wrist_right { get; set; } = 0;
    public float front_vertical_angle_wrist_thumb_left { get; set; } = 0;
    public float front_vertical_angle_wrist_thumb_right { get; set; } = 0;
    public float front_vertical_angle_hip_knee_left { get; set; } = 0;
    public float front_vertical_angle_hip_knee_right { get; set; } = 0;
    public float front_vertical_angle_hip_knee_ankle_left { get; set; } = 0;
    public float front_vertical_angle_hip_knee_ankle_right { get; set; } = 0;
    public float front_vertical_angle_hip_knee_upper_knee_left { get; set;} = 0;
    public float front_vertical_angle_hip_knee_upper_knee_right { get; set; } = 0;
    public float front_vertical_angle_knee_ankle_left { get; set; } = 0;
    public float front_vertical_angle_knee_ankle_right { get; set; } = 0;
    public float front_vertical_angle_ankle_toe_left { get; set; } = 0;
    public float front_vertical_angle_ankle_toe_right { get; set; } = 0;

    // static front hand
    public float front_horizontal_angle_thumb { get; set; } = 0;
    public float front_horizontal_distance_sub_thumb { get; set; } = 0;
    public float front_horizontal_distance_thumb_left { get; set; } = 0;
    public float front_horizontal_distance_thumb_right { get; set; } = 0;
    public float front_hand_angle_thumb_cmc_tip_left { get; set; } = 0;
    public float front_hand_angle_thumb_cmc_tip_right { get; set; } = 0;
    public float front_hand_distance_index_pinky_mcp_left { get; set; } = 0;
    public float front_hand_distance_index_pinky_mcp_right { get; set; } = 0;
    public float front_hand_angle_elbow_wrist_mid_finger_mcp_left { get; set; } = 0;
    public float front_hand_angle_elbow_wrist_mid_finger_mcp_right { get; set; } = 0;

    // static side left
    public float side_left_horizontal_distance_shoulder { get; set; } = 0;
    public float side_left_horizontal_distance_hip { get; set; } = 0;
    public float side_left_horizontal_distance_pinky { get; set; } = 0;
    public float side_left_horizontal_distance_wrist { get; set; } = 0;
    public float side_left_vertical_angle_shoulder_elbow { get; set; } = 0;
    public float side_left_vertical_angle_elbow_wrist { get; set; } = 0;
    public float side_left_vertical_angle_hip_knee { get; set; } = 0;
    public float side_left_vertical_angle_ear_shoulder { get; set; } = 0;
    public float side_left_vertical_angle_nose_shoulder { get; set; } = 0;
    public float side_left_vertical_angle_shoulder_elbow_wrist { get; set; } = 0;
    public float side_left_vertical_angle_hip_knee_ankle { get; set; } = 0;

    // static side right
    public float side_right_horizontal_distance_shoulder { get; set; } = 0;
    public float side_right_horizontal_distance_hip { get; set; } = 0;
    public float side_right_horizontal_distance_pinky { get; set; } = 0;
    public float side_right_horizontal_distance_wrist { get; set; } = 0;
    public float side_right_vertical_angle_shoulder_elbow { get; set; } = 0;
    public float side_right_vertical_angle_elbow_wrist { get; set; } = 0;
    public float side_right_vertical_angle_hip_knee { get; set; } = 0;
    public float side_right_vertical_angle_ear_shoulder { get; set; } = 0;
    public float side_right_vertical_angle_nose_shoulder { get; set; } = 0;
    public float side_right_vertical_angle_shoulder_elbow_wrist { get; set; } = 0;
    public float side_right_vertical_angle_hip_knee_ankle { get; set; } = 0;

    // static back horizontal
    public float back_horizontal_angle_ear { get; set; } = 0;
    public float back_horizontal_distance_sub_ear { get; set; } = 0;
    public float back_horizontal_angle_shoulder { get; set; } = 0;
    public float back_horizontal_distance_sub_shoulder { get; set; } = 0;
    public float back_horizontal_angle_elbow { get; set; } = 0;
    public float back_horizontal_distance_sub_elbow { get; set; } = 0;
    public float back_horizontal_angle_wrist { get; set; } = 0;
    public float back_horizontal_distance_sub_wrist { get; set; } = 0;
    public float back_horizontal_angle_hip { get; set; } = 0;
    public float back_horizontal_distance_sub_hip { get; set; } = 0;
    public float back_horizontal_angle_knee { get; set; } = 0;
    public float back_horizontal_distance_sub_knee { get; set; } = 0;
    public float back_horizontal_angle_ankle { get; set; } = 0;
    public float back_horizontal_distance_sub_ankle { get; set; } = 0;
    public float back_horizontal_distance_knee_left { get; set; } = 0;
    public float back_horizontal_distance_knee_right { get; set; } = 0;
    public float back_horizontal_distance_heel_left { get; set; } = 0;
    public float back_horizontal_distance_heel_right { get; set; } = 0;

    // static back vertical
    public float back_vertical_angle_nose_center_shoulder { get; set; } = 0;
    public float back_vertical_angle_shoudler_center_hip { get;set; } = 0;
    public float back_vertical_angle_nose_center_hip { get; set; } = 0;
    public float back_vertical_angle_knee_heel_left { get; set; } = 0;
    public float back_vertical_angle_knee_heel_right { get; set; } = 0;

    // static back hand
    public float back_horizontal_distance_wrist_left { get; set; } = 0;
    public float back_horizontal_distance_wrist_right { get; set; } = 0;
    public float back_hand_distance_index_pinky_mcp_left { get; set; } = 0;
    public float back_hand_distance_index_pinky_mcp_right { get; set; } = 0;

    // static back sit horizontal
    public float back_sit_horizontal_angle_ear { get; set; } = 0;
    public float back_sit_horizontal_distance_sub_ear { get; set; } = 0;
    public float back_sit_horizontal_angle_shoulder { get; set; } = 0;
    public float back_sit_horizontal_distance_sub_shoulder { get; set; } = 0;
    public float back_sit_horizontal_angle_hip { get; set; } = 0;
    public float back_sit_horizontal_distance_sub_hip { get; set; } = 0;

    // static back sit vertical
    public float back_sit_vertical_angle_nose_left_shoulder_right_shoulder { get; set; } = 0;
    public float back_sit_vertical_angle_left_shoulder_right_shoulder_nose { get; set; } = 0;
    public float back_sit_vertical_angle_right_shoulder_nose_left_shoulder { get; set; } = 0;
    public float back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder { get; set; } = 0;
    public float back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder { get; set; } = 0;
    public float back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip { get; set; } = 0;
    public float back_sit_vertical_angle_shoulder_center_hip { get; set; } = 0;


    // MT_static_ELBOW_ALIGN
    public float front_elbow_align_angle_left_upper_elbow_elbow_wrist { get; set; } = 0;
    public float front_elbow_align_angle_right_upper_elbow_elbow_wrist { get; set; } = 0;
    public float front_elbow_align_distance_left_wrist_shoulder { get; set; } = 0;
    public float front_elbow_align_distance_right_wrist_shoulder { get; set; } = 0;
    public float front_elbow_align_distance_wrist_height { get; set; } = 0;
    public float front_elbow_align_distance_mid_index_height { get; set; } = 0;
    public float front_elbow_align_distance_shoulder_mid_index_left { get; set; } = 0;
    public float front_elbow_align_distance_shoulder_mid_index_right { get; set; } = 0;
    public float front_elbow_align_angle_mid_index_wrist_elbow_left { get; set; } = 0;
    public float front_elbow_align_angle_mid_index_wrist_elbow_right { get; set; } = 0;
    public float front_elbow_align_angle_left_shoulder_elbow_wrist { get; set; } = 0;
    public float front_elbow_align_angle_right_shoulder_elbow_wrist { get; set; } = 0;
    public float front_elbow_align_distance_center_mid_finger_left { get; set; } = 0;
    public float front_elbow_align_distance_center_mid_finger_right { get; set; } = 0;
    public float front_elbow_align_distance_center_wrist_left { get; set; } = 0;
    public float front_elbow_align_distance_center_wrist_right { get; set; } = 0;

    [Ignore]
    public List<LandmarkCoordinateJson> pose_landmark { get; set; } = new List<LandmarkCoordinateJson>();
    [Ignore]
    public List<HandLandmarkResultJson> hand_landmark { get; set; } = new List<HandLandmarkResultJson>();

    public int uploaded { get; set; } = 0;
    public DateTime upload_date { get; set; }

    public int uploaded_json { get; set; } = 0;
    public int uploaded_file { get; set; } = 0;
    public int uploaded_json_fail { get; set; } = 0;
    public int uploaded_file_fail { get; set; } = 0;

    public int used { get; set; } = 0;





    public void CopyValueFromServer(t_measure_result_static mrs)
    {
        server_sn = mrs.sn;
        user_uuid = mrs.user_uuid;
        user_sn = mrs.user_sn;
        user_name = mrs.user_name;
        measure_seq = mrs.measure_seq;
        measure_type = mrs.measure_type;
        //reg_date = mrs.reg_date;
        //measure_start_time = mrs.measure_start_time;
        //measure_end_time = mrs.measure_end_time;
        //measure_photo_file_name = mrs.measure_photo_file_name;
        //measure_overlay_width = mrs.measure_overlay_width;
        //measure_overlay_height = mrs.measure_overlay_height;
        //measure_overlay_scale_factor_x = mrs.measure_overlay_scale_factor_x;
        //measure_overlay_scale_factor_y = mrs.measure_overlay_scale_factor_y;
        measure_server_file_name = mrs.measure_server_file_name;
        measure_server_json_name = mrs.measure_server_json_name;

        front_horizontal_angle_ear = mrs.front_horizontal_angle_ear;
        front_horizontal_distance_sub_ear = mrs.front_horizontal_distance_sub_ear;
        front_horizontal_angle_shoulder = mrs.front_horizontal_angle_shoulder;
        front_horizontal_distance_sub_shoulder = mrs.front_horizontal_distance_sub_shoulder;
        front_horizontal_angle_elbow = mrs.front_horizontal_angle_elbow;
        front_horizontal_distance_sub_elbow = mrs.front_horizontal_distance_sub_elbow;
        front_horizontal_angle_wrist = mrs.front_horizontal_angle_wrist;
        front_horizontal_distance_sub_wrist = mrs.front_horizontal_distance_sub_wrist;
        front_horizontal_angle_hip = mrs.front_horizontal_angle_hip;
        front_horizontal_distance_sub_hip = mrs.front_horizontal_distance_sub_hip;
        front_horizontal_angle_knee = mrs.front_horizontal_angle_knee;
        front_horizontal_distance_sub_knee = mrs.front_horizontal_distance_sub_knee;
        front_horizontal_angle_ankle = mrs.front_horizontal_angle_ankle;
        front_horizontal_distance_sub_ankle = mrs.front_horizontal_distance_sub_ankle;
        front_horizontal_distance_wrist_left = mrs.front_horizontal_distance_wrist_left;
        front_horizontal_distance_wrist_right = mrs.front_horizontal_distance_wrist_right;
        front_horizontal_distance_knee_left = mrs.front_horizontal_distance_knee_left;
        front_horizontal_distance_knee_right = mrs.front_horizontal_distance_knee_right;
        front_horizontal_distance_ankle_left = mrs.front_horizontal_distance_ankle_left;
        front_horizontal_distance_ankle_right = mrs.front_horizontal_distance_ankle_right;
        front_horizontal_distance_toe_left = mrs.front_horizontal_distance_toe_left;
        front_horizontal_distance_toe_right = mrs.front_horizontal_distance_toe_right;

        front_vertical_angle_shoulder_elbow_left = mrs.front_vertical_angle_shoulder_elbow_left;
        front_vertical_angle_shoulder_elbow_right = mrs.front_vertical_angle_shoulder_elbow_right;
        front_vertical_angle_elbow_wrist_left = mrs.front_vertical_angle_elbow_wrist_left;
        front_vertical_angle_elbow_wrist_right = mrs.front_vertical_angle_elbow_wrist_right;
        front_vertical_angle_shoulder_elbow_wrist_left = mrs.front_vertical_angle_shoulder_elbow_wrist_left;
        front_vertical_angle_shoulder_elbow_wrist_right = mrs.front_vertical_angle_shoulder_elbow_wrist_right;
        front_vertical_angle_wrist_thumb_left = mrs.front_vertical_angle_wrist_thumb_left;
        front_vertical_angle_wrist_thumb_right = mrs.front_vertical_angle_wrist_thumb_right;
        front_vertical_angle_hip_knee_left = mrs.front_vertical_angle_hip_knee_left;
        front_vertical_angle_hip_knee_right = mrs.front_vertical_angle_hip_knee_right;
        front_vertical_angle_hip_knee_ankle_left = mrs.front_vertical_angle_hip_knee_ankle_left;
        front_vertical_angle_hip_knee_ankle_right = mrs.front_vertical_angle_hip_knee_ankle_right;
        front_vertical_angle_hip_knee_upper_knee_left = mrs.front_vertical_angle_hip_knee_upper_knee_left;
        front_vertical_angle_hip_knee_upper_knee_right = mrs.front_vertical_angle_hip_knee_upper_knee_right;
        front_vertical_angle_knee_ankle_left = mrs.front_vertical_angle_knee_ankle_left;
        front_vertical_angle_knee_ankle_right = mrs.front_vertical_angle_knee_ankle_right;
        front_vertical_angle_ankle_toe_left = mrs.front_vertical_angle_ankle_toe_left;
        front_vertical_angle_ankle_toe_right = mrs.front_vertical_angle_ankle_toe_right;

        front_horizontal_angle_thumb = mrs.front_horizontal_angle_thumb;
        front_horizontal_distance_sub_thumb = mrs.front_horizontal_distance_sub_thumb;
        front_horizontal_distance_thumb_left = mrs.front_horizontal_distance_thumb_left;
        front_horizontal_distance_thumb_right = mrs.front_horizontal_distance_thumb_right;
        front_hand_angle_thumb_cmc_tip_left = mrs.front_hand_angle_thumb_cmc_tip_left;
        front_hand_angle_thumb_cmc_tip_right = mrs.front_hand_angle_thumb_cmc_tip_right;
        front_hand_distance_index_pinky_mcp_left = mrs.front_hand_distance_index_pinky_mcp_left;
        front_hand_distance_index_pinky_mcp_right = mrs.front_hand_distance_index_pinky_mcp_right;
        front_hand_angle_elbow_wrist_mid_finger_mcp_left = mrs.front_hand_angle_elbow_wrist_mid_finger_mcp_left;
        front_hand_angle_elbow_wrist_mid_finger_mcp_right = mrs.front_hand_angle_elbow_wrist_mid_finger_mcp_right;

        side_left_horizontal_distance_shoulder = mrs.side_left_horizontal_distance_shoulder;
        side_left_horizontal_distance_hip = mrs.side_left_horizontal_distance_hip;
        side_left_horizontal_distance_pinky = mrs.side_left_horizontal_distance_pinky;
        side_left_horizontal_distance_wrist = mrs.side_left_horizontal_distance_wrist;
        side_left_vertical_angle_shoulder_elbow = mrs.side_left_vertical_angle_shoulder_elbow;
        side_left_vertical_angle_elbow_wrist = mrs.side_left_vertical_angle_elbow_wrist;
        side_left_vertical_angle_hip_knee = mrs.side_left_vertical_angle_hip_knee;
        side_left_vertical_angle_ear_shoulder = mrs.side_left_vertical_angle_ear_shoulder;
        side_left_vertical_angle_nose_shoulder = mrs.side_left_vertical_angle_nose_shoulder;
        side_left_vertical_angle_shoulder_elbow_wrist = mrs.side_left_vertical_angle_shoulder_elbow_wrist;
        side_left_vertical_angle_hip_knee_ankle = mrs.side_left_vertical_angle_hip_knee_ankle;

        side_right_horizontal_distance_shoulder = mrs.side_right_horizontal_distance_shoulder;
        side_right_horizontal_distance_hip = mrs.side_right_horizontal_distance_hip;
        side_right_horizontal_distance_pinky = mrs.side_right_horizontal_distance_pinky;
        side_right_horizontal_distance_wrist = mrs.side_right_horizontal_distance_wrist;
        side_right_vertical_angle_shoulder_elbow = mrs.side_right_vertical_angle_shoulder_elbow;
        side_right_vertical_angle_elbow_wrist = mrs.side_right_vertical_angle_elbow_wrist;
        side_right_vertical_angle_hip_knee = mrs.side_right_vertical_angle_hip_knee;
        side_right_vertical_angle_ear_shoulder = mrs.side_right_vertical_angle_ear_shoulder;
        side_right_vertical_angle_nose_shoulder = mrs.side_right_vertical_angle_nose_shoulder;
        side_right_vertical_angle_shoulder_elbow_wrist = mrs.side_right_vertical_angle_shoulder_elbow_wrist;
        side_right_vertical_angle_hip_knee_ankle = mrs.side_right_vertical_angle_hip_knee_ankle;

        back_horizontal_angle_ear = mrs.back_horizontal_angle_ear;
        back_horizontal_distance_sub_ear = mrs.back_horizontal_distance_sub_ear;
        back_horizontal_angle_shoulder = mrs.back_horizontal_angle_shoulder;
        back_horizontal_distance_sub_shoulder = mrs.back_horizontal_distance_sub_shoulder;
        back_horizontal_angle_elbow = mrs.back_horizontal_angle_elbow;
        back_horizontal_distance_sub_elbow = mrs.back_horizontal_distance_sub_elbow;
        back_horizontal_angle_wrist = mrs.back_horizontal_angle_wrist;
        back_horizontal_distance_sub_wrist = mrs.back_horizontal_distance_sub_wrist;
        back_horizontal_angle_hip = mrs.back_horizontal_angle_hip;
        back_horizontal_distance_sub_hip = mrs.back_horizontal_distance_sub_hip;
        back_horizontal_angle_knee = mrs.back_horizontal_angle_knee;
        back_horizontal_distance_sub_knee = mrs.back_horizontal_distance_sub_knee;
        back_horizontal_angle_ankle = mrs.back_horizontal_angle_ankle;
        back_horizontal_distance_sub_ankle = mrs.back_horizontal_distance_sub_ankle;
        back_horizontal_distance_knee_left = mrs.back_horizontal_distance_knee_left;
        back_horizontal_distance_knee_right = mrs.back_horizontal_distance_knee_right;
        back_horizontal_distance_heel_left = mrs.back_horizontal_distance_heel_left;
        back_horizontal_distance_heel_right = mrs.back_horizontal_distance_heel_right;

        back_vertical_angle_nose_center_shoulder = mrs.back_vertical_angle_nose_center_shoulder;
        back_vertical_angle_shoudler_center_hip = mrs.back_vertical_angle_shoudler_center_hip;
        back_vertical_angle_nose_center_hip = mrs.back_vertical_angle_nose_center_hip;
        back_vertical_angle_knee_heel_left = mrs.back_vertical_angle_knee_heel_left;
        back_vertical_angle_knee_heel_right = mrs.back_vertical_angle_knee_heel_right;

        back_horizontal_distance_wrist_left = mrs.back_horizontal_distance_wrist_left;
        back_horizontal_distance_wrist_right = mrs.back_horizontal_distance_wrist_right;
        back_hand_distance_index_pinky_mcp_left = mrs.back_hand_distance_index_pinky_mcp_left;
        back_hand_distance_index_pinky_mcp_right = mrs.back_hand_distance_index_pinky_mcp_right;

        back_sit_horizontal_angle_ear = mrs.back_sit_horizontal_angle_ear;
        back_sit_horizontal_distance_sub_ear = mrs.back_sit_horizontal_distance_sub_ear;
        back_sit_horizontal_angle_shoulder = mrs.back_sit_horizontal_angle_shoulder;
        back_sit_horizontal_distance_sub_shoulder = mrs.back_sit_horizontal_distance_sub_shoulder;
        back_sit_horizontal_angle_hip = mrs.back_sit_horizontal_angle_hip;
        back_sit_horizontal_distance_sub_hip = mrs.back_sit_horizontal_distance_sub_hip;

        back_sit_vertical_angle_nose_left_shoulder_right_shoulder = mrs.back_sit_vertical_angle_nose_left_shoulder_right_shoulder;
        back_sit_vertical_angle_left_shoulder_right_shoulder_nose = mrs.back_sit_vertical_angle_left_shoulder_right_shoulder_nose;
        back_sit_vertical_angle_right_shoulder_nose_left_shoulder = mrs.back_sit_vertical_angle_right_shoulder_nose_left_shoulder;
        back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder = mrs.back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder;
        back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder = mrs.back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder;
        back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip = mrs.back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip;
        back_sit_vertical_angle_shoulder_center_hip = mrs.back_sit_vertical_angle_shoulder_center_hip;

        front_elbow_align_angle_left_upper_elbow_elbow_wrist = mrs.front_elbow_align_angle_left_upper_elbow_elbow_wrist;
        front_elbow_align_angle_right_upper_elbow_elbow_wrist = mrs.front_elbow_align_angle_right_upper_elbow_elbow_wrist;
        front_elbow_align_distance_left_wrist_shoulder = mrs.front_elbow_align_distance_left_wrist_shoulder;
        front_elbow_align_distance_right_wrist_shoulder = mrs.front_elbow_align_distance_right_wrist_shoulder;
        front_elbow_align_distance_wrist_height = mrs.front_elbow_align_distance_wrist_height;
        front_elbow_align_distance_mid_index_height = mrs.front_elbow_align_distance_mid_index_height;
        front_elbow_align_distance_shoulder_mid_index_left = mrs.front_elbow_align_distance_shoulder_mid_index_left;
        front_elbow_align_distance_shoulder_mid_index_right = mrs.front_elbow_align_distance_shoulder_mid_index_right;
        front_elbow_align_angle_mid_index_wrist_elbow_left = mrs.front_elbow_align_angle_mid_index_wrist_elbow_left;
        front_elbow_align_angle_mid_index_wrist_elbow_right = mrs.front_elbow_align_angle_mid_index_wrist_elbow_right;
        front_elbow_align_angle_left_shoulder_elbow_wrist = mrs.front_elbow_align_angle_left_shoulder_elbow_wrist;
        front_elbow_align_angle_right_shoulder_elbow_wrist = mrs.front_elbow_align_angle_right_shoulder_elbow_wrist;
        front_elbow_align_distance_center_mid_finger_left = mrs.front_elbow_align_distance_center_mid_finger_left;
        front_elbow_align_distance_center_mid_finger_right = mrs.front_elbow_align_distance_center_mid_finger_right;
        front_elbow_align_distance_center_wrist_left = mrs.front_elbow_align_distance_center_wrist_left;
        front_elbow_align_distance_center_wrist_right = mrs.front_elbow_align_distance_center_wrist_right;

        uploaded = mrs.uploaded;
        upload_date = mrs.upload_date;
        uploaded_json = mrs.uploaded_json;
        uploaded_file = mrs.uploaded_file;
        used = mrs.used;
    }

    public void setLandmarksInfo(LandmarkResult landmarkResult, List<HandLandmarkResult> handLandmark)
    {
        pose_landmark.Clear();
        for(int i=0; i<landmarkResult.pose_landmark.Count; i++)
        {
            pose_landmark.Add(new LandmarkCoordinateJson(landmarkResult.pose_landmark[i]));
        }

        hand_landmark.Clear();
        for(int i=0; i< handLandmark.Count; i++)
        {
            hand_landmark.Add(new HandLandmarkResultJson(handLandmark[i]));
        }
    }

    public void setResultStatic(LandmarkResult landmarkResult, MEASURE_TYPE measure_type,
        List<HandLandmarkResult> resultsHand)
    {
        try
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
            for(int i=0; resultsHand != null && i < resultsHand.Count; i++)
            {
                if (resultsHand[i].left_right == 0) hand_left = resultsHand[i];
                else hand_right = resultsHand[i];
            }

            if (measure_type == MEASURE_TYPE.MT_STATIC_FRONT)
            {
                front_horizontal_angle_ear = MathHelpers.getAngle2D180(left_ear, right_ear);
                front_horizontal_distance_sub_ear = MathHelpers.getDistanceY(left_ear, right_ear);
                front_horizontal_angle_shoulder = MathHelpers.getAngle2D180(left_shoulder, right_shoulder);
                front_horizontal_distance_sub_shoulder = MathHelpers.getDistanceY(left_shoulder, right_shoulder);
                front_horizontal_angle_elbow = MathHelpers.getAngle2D180(left_elbow, right_elbow);
                front_horizontal_distance_sub_elbow = MathHelpers.getDistanceY(left_elbow, right_elbow);
                front_horizontal_angle_wrist = MathHelpers.getAngle2D180(left_wrist, right_wrist);
                front_horizontal_distance_sub_wrist = MathHelpers.getDistanceY(left_wrist, right_wrist);
                front_horizontal_angle_hip = MathHelpers.getAngle2D180(left_hip, right_hip);
                front_horizontal_distance_sub_hip = MathHelpers.getDistanceY(left_hip, right_hip);
                front_horizontal_angle_knee = MathHelpers.getAngle2D180(left_knee, right_knee);
                front_horizontal_distance_sub_knee = MathHelpers.getDistanceY(left_knee, right_knee);
                front_horizontal_angle_ankle = MathHelpers.getAngle2D180(left_ankle, right_ankle);
                front_horizontal_distance_sub_ankle = MathHelpers.getDistanceY(left_ankle, right_ankle);

                front_horizontal_distance_wrist_left = MathHelpers.getDistanceX(center_ankle, left_wrist);
                front_horizontal_distance_wrist_right = MathHelpers.getDistanceX(center_ankle, right_wrist);
                front_horizontal_distance_knee_left = MathHelpers.getDistanceX(center_ankle, left_knee);
                front_horizontal_distance_knee_right = MathHelpers.getDistanceX(center_ankle, right_knee);
                front_horizontal_distance_ankle_left = MathHelpers.getDistanceX(center_ankle, left_ankle);
                front_horizontal_distance_ankle_right = MathHelpers.getDistanceX(center_ankle, right_ankle);
                front_horizontal_distance_toe_left = MathHelpers.getDistanceX(center_ankle, left_foot_index);
                front_horizontal_distance_toe_right = MathHelpers.getDistanceX(center_ankle, right_foot_index);                

                front_vertical_angle_shoulder_elbow_left = MathHelpers.getAngle2D180(left_shoulder, left_elbow);
                front_vertical_angle_shoulder_elbow_right = MathHelpers.getAngle2D180(right_shoulder, right_elbow, true);
                front_vertical_angle_elbow_wrist_left = MathHelpers.getAngle2D180(left_elbow, left_wrist);
                front_vertical_angle_elbow_wrist_right = MathHelpers.getAngle2D180(right_elbow, right_wrist, true);
                front_vertical_angle_shoulder_elbow_wrist_left = MathHelpers.getAngle3Point2D(left_shoulder, left_elbow, left_wrist);
                front_vertical_angle_shoulder_elbow_wrist_right = MathHelpers.getAngle3Point2D(right_shoulder, right_elbow, right_wrist);
                front_vertical_angle_hip_knee_left = MathHelpers.getAngle2D180(left_hip, left_knee);
                front_vertical_angle_hip_knee_right = MathHelpers.getAngle2D180(right_hip, right_knee, true);
                front_vertical_angle_hip_knee_ankle_left = MathHelpers.getAngle3Point2D(left_hip, left_knee, left_ankle);
                front_vertical_angle_hip_knee_ankle_right = MathHelpers.getAngle3Point2D(right_hip, right_knee, right_ankle);
                front_vertical_angle_hip_knee_upper_knee_left = MathHelpers.getAngle3Point2D(
                    left_hip.screen_position, left_knee.screen_position, new Vector2(left_knee.screen_position.x, left_hip.screen_position.y));
                front_vertical_angle_hip_knee_upper_knee_right = MathHelpers.getAngle3Point2D(
                    right_hip.screen_position, right_knee.screen_position, new Vector2(right_knee.screen_position.x, right_hip.screen_position.y));
                front_vertical_angle_knee_ankle_left = MathHelpers.getAngle2D180(left_knee, left_ankle);
                front_vertical_angle_knee_ankle_right = MathHelpers.getAngle2D180(right_knee, right_ankle, true);
                front_vertical_angle_ankle_toe_left = MathHelpers.getAngle2D180(left_ankle, left_foot_index);
                front_vertical_angle_ankle_toe_right = MathHelpers.getAngle2D180(right_ankle, right_foot_index, true);


                if (hand_left != null)
                {
                    front_horizontal_distance_thumb_left = MathHelpers.getDistanceX(center_ankle, hand_left.landmarks[(int)LANDMARK_NAME_HAND.THUMB_TIP]);
                    front_hand_angle_thumb_cmc_tip_left = MathHelpers.getAngle2D180(
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.THUMB_CMC], hand_left.landmarks[(int)LANDMARK_NAME_HAND.THUMB_TIP]);
                    front_hand_distance_index_pinky_mcp_left = MathHelpers.getDistanceX(
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.INDEX_FINGER_MCP], hand_left.landmarks[(int)LANDMARK_NAME_HAND.PINKY_MCP]);
                    front_hand_distance_index_pinky_mcp_left = MathHelpers.getDistanceX(
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.INDEX_FINGER_MCP], hand_left.landmarks[(int)LANDMARK_NAME_HAND.PINKY_MCP]);
                    front_hand_angle_elbow_wrist_mid_finger_mcp_left = MathHelpers.getAngle3Point2D(
                        left_elbow.screen_position,
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.WRIST].screen_position,
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_MCP].screen_position);
                    front_vertical_angle_wrist_thumb_left = MathHelpers.getAngle2D180(
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.WRIST].screen_position,
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.THUMB_TIP].screen_position);
                }
                if (hand_right != null)
                {
                    front_horizontal_distance_thumb_right = MathHelpers.getDistanceX(center_ankle, hand_right.landmarks[(int)LANDMARK_NAME_HAND.THUMB_TIP]);
                    front_hand_angle_thumb_cmc_tip_right = MathHelpers.getAngle2D180(
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.THUMB_CMC], hand_right.landmarks[(int)LANDMARK_NAME_HAND.THUMB_TIP], true);
                    front_hand_distance_index_pinky_mcp_right = MathHelpers.getDistanceX(
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.INDEX_FINGER_MCP], hand_right.landmarks[(int)LANDMARK_NAME_HAND.PINKY_MCP]);
                    front_hand_distance_index_pinky_mcp_right = MathHelpers.getDistanceX(
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.INDEX_FINGER_MCP], hand_right.landmarks[(int)LANDMARK_NAME_HAND.PINKY_MCP]);
                    front_hand_angle_elbow_wrist_mid_finger_mcp_right = MathHelpers.getAngle3Point2D(
                        right_elbow.screen_position,
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.WRIST].screen_position,
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_MCP].screen_position);
                    front_vertical_angle_wrist_thumb_right = MathHelpers.getAngle2D180(
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.WRIST].screen_position,
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.THUMB_TIP].screen_position, true);
                }
                if (hand_left != null && hand_right != null)
                {
                    front_horizontal_angle_thumb = MathHelpers.getAngle2D180(
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.THUMB_TIP], hand_right.landmarks[(int)LANDMARK_NAME_HAND.THUMB_TIP]);
                    front_horizontal_distance_sub_thumb = MathHelpers.getDistanceY(
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.THUMB_TIP], hand_right.landmarks[(int)LANDMARK_NAME_HAND.THUMB_TIP]);
                }
            }
            else if(measure_type == MEASURE_TYPE.MT_STATIC_SIDE_LEFT)
            {
                side_left_horizontal_distance_shoulder = MathHelpers.getDistanceX(left_ankle, left_shoulder);
                side_left_horizontal_distance_hip = MathHelpers.getDistanceX(left_ankle, left_hip);
                side_left_horizontal_distance_wrist = MathHelpers.getDistanceX(left_ankle, left_wrist);
                side_left_vertical_angle_shoulder_elbow = MathHelpers.getAngle2D180(left_shoulder, left_elbow);
                side_left_vertical_angle_shoulder_elbow_wrist = MathHelpers.getAngle3Point2D(
                        left_shoulder, left_elbow, left_wrist);
                side_left_vertical_angle_elbow_wrist = MathHelpers.getAngle2D180(left_elbow, left_wrist);
                side_left_vertical_angle_hip_knee = MathHelpers.getAngle2D180(left_hip, left_knee);
                side_left_vertical_angle_hip_knee_ankle = MathHelpers.getAngle3Point2D(left_hip, left_knee, left_ankle);
                side_left_vertical_angle_ear_shoulder = MathHelpers.getAngle2D180(left_ear, left_shoulder);
                side_left_vertical_angle_nose_shoulder = MathHelpers.getAngle2D180(nose, left_shoulder);

                if(hand_left != null)
                {
                    side_left_horizontal_distance_pinky = MathHelpers.getDistanceX(left_ankle, hand_left.landmarks[(int)LANDMARK_NAME_HAND.PINKY_MCP]);
                    //side_left_vertical_angle_elbow_wrist = MathHelpers.getAngle2D180(left_elbow, hand_left.landmarks[(int)LANDMARK_NAME_HAND.WRIST]);
                    //side_left_vertical_angle_shoulder_elbow_wrist = MathHelpers.getAngle3Point2D(
                    //    left_shoulder, left_elbow, hand_left.landmarks[(int)LANDMARK_NAME_HAND.WRIST]);
                }
            }
            else if (measure_type == MEASURE_TYPE.MT_STATIC_SIDE_RIGHT)
            {
                side_right_horizontal_distance_shoulder = MathHelpers.getDistanceX(right_ankle, right_shoulder);
                side_right_horizontal_distance_hip = MathHelpers.getDistanceX(right_ankle, right_hip);
                side_right_horizontal_distance_wrist = MathHelpers.getDistanceX(right_ankle, right_wrist);
                side_right_vertical_angle_shoulder_elbow = MathHelpers.getAngle2D180(right_shoulder, right_elbow, true);
                side_right_vertical_angle_shoulder_elbow_wrist = MathHelpers.getAngle3Point2D(
                        right_shoulder, right_elbow, right_wrist);
                side_right_vertical_angle_elbow_wrist = MathHelpers.getAngle2D180(right_elbow, right_wrist, true);
                side_right_vertical_angle_hip_knee = MathHelpers.getAngle2D180(right_hip, right_knee, true);
                side_right_vertical_angle_hip_knee_ankle = MathHelpers.getAngle3Point2D(right_hip, right_knee, right_ankle);
                side_right_vertical_angle_ear_shoulder = MathHelpers.getAngle2D180(right_ear, right_shoulder, true);
                side_right_vertical_angle_nose_shoulder = MathHelpers.getAngle2D180(nose, right_shoulder, true);

                if (hand_right != null)
                {
                    side_right_horizontal_distance_pinky = MathHelpers.getDistanceX(right_ankle, hand_right.landmarks[(int)LANDMARK_NAME_HAND.PINKY_MCP]);
                    //side_right_vertical_angle_elbow_wrist = MathHelpers.getAngle2D180(right_elbow, hand_right.landmarks[(int)LANDMARK_NAME_HAND.WRIST], true);
                    //side_right_vertical_angle_shoulder_elbow_wrist = MathHelpers.getAngle3Point2D(
                    //    right_shoulder, right_elbow, hand_right.landmarks[(int)LANDMARK_NAME_HAND.WRIST]);
                }
            }
            else if(measure_type == MEASURE_TYPE.MT_STATIC_BACK)
            {
                back_horizontal_angle_ear = MathHelpers.getAngle2D180(left_ear, right_ear);
                back_horizontal_distance_sub_ear = MathHelpers.getDistanceY(left_ear, right_ear);
                back_horizontal_angle_shoulder = MathHelpers.getAngle2D180(left_shoulder, right_shoulder);
                back_horizontal_distance_sub_shoulder = MathHelpers.getDistanceY(left_shoulder, right_shoulder);
                back_horizontal_angle_elbow = MathHelpers.getAngle2D180(left_elbow, right_elbow);
                back_horizontal_distance_sub_elbow = MathHelpers.getDistanceY(left_elbow, right_elbow);
                back_horizontal_angle_wrist = MathHelpers.getAngle2D180(left_wrist, right_wrist);
                back_horizontal_distance_sub_wrist = MathHelpers.getDistanceY(left_wrist, right_wrist);
                back_horizontal_angle_hip = MathHelpers.getAngle2D180(left_hip, right_hip);
                back_horizontal_distance_sub_hip = MathHelpers.getDistanceY(left_hip, right_hip);
                back_horizontal_angle_knee = MathHelpers.getAngle2D180(left_knee, right_knee);
                back_horizontal_distance_sub_knee = MathHelpers.getDistanceY(left_knee, right_knee);
                back_horizontal_angle_ankle = MathHelpers.getAngle2D180(left_ankle, right_ankle);
                back_horizontal_distance_sub_ankle = MathHelpers.getDistanceY(left_ankle, right_ankle);
                back_horizontal_distance_wrist_left = MathHelpers.getDistanceX(center_ankle, left_wrist);
                back_horizontal_distance_wrist_right = MathHelpers.getDistanceX(center_ankle, right_wrist);
                back_horizontal_distance_knee_left = MathHelpers.getDistanceX(center_ankle, left_knee);
                back_horizontal_distance_knee_right = MathHelpers.getDistanceX(center_ankle, right_knee);
                back_horizontal_distance_heel_left = MathHelpers.getDistanceX(center_ankle, left_heel);
                back_horizontal_distance_heel_right = MathHelpers.getDistanceX(center_ankle, right_heel);

                back_vertical_angle_nose_center_shoulder = MathHelpers.getAngle2D180(nose, center_shoulder);
                back_vertical_angle_shoudler_center_hip = MathHelpers.getAngle2D180(center_shoulder, center_hip);
                back_vertical_angle_nose_center_hip = MathHelpers.getAngle2D180(nose, center_hip);
                back_vertical_angle_knee_heel_left = MathHelpers.getAngle2D180(left_knee, left_heel);
                back_vertical_angle_knee_heel_right = MathHelpers.getAngle2D180(right_knee, right_heel, true);


                if(hand_left != null)
                {
                    back_hand_distance_index_pinky_mcp_left = MathHelpers.getDistanceX(
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.INDEX_FINGER_MCP],
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.PINKY_MCP]);
                }
                if (hand_right != null)
                {
                    back_hand_distance_index_pinky_mcp_right = MathHelpers.getDistanceX(
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.INDEX_FINGER_MCP],
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.PINKY_MCP]);
                }
            }
            else if(measure_type == MEASURE_TYPE.MT_STATIC_BACK_SIT)
            {
                back_sit_horizontal_angle_ear = MathHelpers.getAngle2D180(left_ear, right_ear);
                back_sit_horizontal_distance_sub_ear = MathHelpers.getDistanceY(left_ear, right_ear);
                back_sit_horizontal_angle_shoulder = MathHelpers.getAngle2D180(left_shoulder, right_shoulder);
                back_sit_horizontal_distance_sub_shoulder = MathHelpers.getDistanceY(left_shoulder, right_shoulder);
                back_sit_horizontal_angle_hip = MathHelpers.getAngle2D180(left_hip, right_hip);
                back_sit_horizontal_distance_sub_hip = MathHelpers.getDistanceY(left_hip, right_hip);

                back_sit_vertical_angle_nose_left_shoulder_right_shoulder = MathHelpers.getAngle3Point2D(nose, left_shoulder, right_shoulder);
                back_sit_vertical_angle_left_shoulder_right_shoulder_nose = MathHelpers.getAngle3Point2D(left_shoulder, right_shoulder, nose);
                back_sit_vertical_angle_right_shoulder_nose_left_shoulder = MathHelpers.getAngle3Point2D(right_shoulder, nose, left_shoulder);
                back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder = MathHelpers.getAngle3Point2D(left_shoulder, center_hip, right_shoulder);
                back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder = MathHelpers.getAngle3Point2D(center_hip, right_shoulder, left_shoulder);
                back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip = MathHelpers.getAngle3Point2D(right_shoulder, left_shoulder, center_hip);
                back_sit_vertical_angle_shoulder_center_hip = MathHelpers.getAngle2D180(center_shoulder, center_hip);
            }
            else if(measure_type == MEASURE_TYPE.MT_STATIC_ELBOW_ALIGN)
            {
                front_elbow_align_angle_left_upper_elbow_elbow_wrist = MathHelpers.getAngle3Point2D(
                    new Vector2(left_elbow.screen_position.x, left_shoulder.screen_position.y),
                    left_elbow.screen_position,
                    left_wrist.screen_position);
                front_elbow_align_angle_right_upper_elbow_elbow_wrist = MathHelpers.getAngle3Point2D(
                    new Vector2(right_elbow.screen_position.x, right_shoulder.screen_position.y),
                    right_elbow.screen_position,
                    right_wrist.screen_position);
                front_elbow_align_distance_left_wrist_shoulder = MathHelpers.getDistanceX(left_shoulder, left_wrist);
                front_elbow_align_distance_right_wrist_shoulder = MathHelpers.getDistanceX(right_shoulder, right_wrist);
                front_elbow_align_distance_wrist_height = MathHelpers.getDistanceY(left_wrist, right_wrist);

                front_elbow_align_angle_left_shoulder_elbow_wrist = MathHelpers.getAngle3Point2D(left_shoulder, left_elbow, left_wrist);
                front_elbow_align_angle_right_shoulder_elbow_wrist = MathHelpers.getAngle3Point2D(right_shoulder, right_elbow, right_wrist);

                front_elbow_align_distance_center_wrist_left = MathHelpers.getDistanceX(center_ankle, left_wrist);
                front_elbow_align_distance_center_wrist_right = MathHelpers.getDistanceX(center_ankle, right_wrist);

                if (hand_left != null)
                {
                    front_elbow_align_distance_shoulder_mid_index_left = MathHelpers.getDistanceX(
                        left_shoulder, hand_left.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_MCP]);
                    front_elbow_align_angle_mid_index_wrist_elbow_left = MathHelpers.getAngle3Point2D(
                        left_elbow,
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.WRIST],
                        hand_left.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_MCP]);
                    front_elbow_align_distance_center_mid_finger_left = MathHelpers.getDistanceX(
                        center_ankle, hand_left.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_MCP]);
                }
                if (hand_right != null)
                {
                    front_elbow_align_distance_shoulder_mid_index_right = MathHelpers.getDistanceX(
                        right_shoulder, hand_right.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_MCP]);
                    front_elbow_align_angle_mid_index_wrist_elbow_right = MathHelpers.getAngle3Point2D(
                        right_elbow,
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.WRIST],
                        hand_right.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_MCP]);
                    front_elbow_align_distance_center_mid_finger_right = MathHelpers.getDistanceX(
                        center_ankle, hand_right.landmarks[(int)LANDMARK_NAME_HAND.MIDDLE_FINGER_MCP]);                    
                }
            }
        }
        catch (Exception e)
        {
            Debug.LogException(e);
        }
    }
}

