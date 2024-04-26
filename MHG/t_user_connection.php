<?php 
$servername = "localhost";
$username = "root";
$password = "tangoplus0585!";
$dbname = "tango_gym_test";

$con = mysqli_connect($servername, $username, $password, $dbname);

$key = openssl_random_pseudo_bytes(16);
$ivlen = openssl_cipher_iv_length($cipher);
$iv = openssl_random_pseudo_bytes($ivlen);
$tag = "";



if (!$con) {
    echo "Failed to connect to MySQL".mysqli_connect_error();
}

mysqli_set_charset($con, "utf8");
$method = $_SERVER['REQUEST_METHOD'];
$data = json_decode(file_get_contents('php://input'), true);
// $data = json_decode($output, true);

switch($method) {
    case 'POST': 
        $sql = "SELECT * FROM t_user WHERE login_token= ?";
        $stmt = mysqli_prepare($con, $sql);
        mysqli_stmt_bind_param($stmt, "i", $data["login_token"]);
        $res = mysqli_stmt_execute($stmt);
        if ($res === TRUE) {
            mysqli_stmt_bind_result($stmt, $user_sn, $user_id, $user_password, $user_name, $user_gender, $user_grade, $register_date, $mobile, $user_email, $birthday, $last_login_date, $modify_date, $height, $weight, $address, $address_detail, $login_token);
            while (mysqli_stmt_fetch($stmt)) {

                echo "$stmt, $user_id, $user_password, $user_name, $user_gender, $user_grade, $register_date, $mobile, $user_email, $birthday, $last_login_date, $modify_date, $height, $weight, $address, $address_detail, $login_token\n"; // 각 행 출력
            }
        } else {
            echo "POST Error: " . mysqli_error($con);
        };
        mysqli_close($con);
        break;
    case 'PUT': 
            $sql = "INSERT INTO t_user (user_id, user_password, user_name, user_gender, user_grade, register_date, mobile, user_email, birthday, last_login_date, modify_date, height, weight, address ,address_detail, login_token) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            $stmt = mysqli_prepare($con, $sql);
            $encrypted_password = openssl_encrypt($data["user_password"], $cipher, $key, $options= 0, $iv, $tag);
            
            mysqli_stmt_bind_param($stmt, "ssssssssssssssss", $data["user_id"], $encrypted_password, $data["user_name"], $data["user_gender"], $data["user_grade"], $data["register_date"], $data["mobile"], $data["user_email"], $data["birthday"], $data["last_login_date"], $data["modify_date"], $data["height"], $data["weight"], $data["address"], $data["address_detail"], $data["login_token"]);
            $res = mysqli_stmt_execute($stmt);
            if ($res === TRUE) {
                echo "Insert Successfully";
            } else {
                echo "Insert Error: " . mysqli_error($con);
            };
            mysqli_close($con);
            break;
    case 'DELETE':
        $sql = "DELETE FROM t_user WHERE login_token= ?";
        $stmt = mysqli_prepare($con, $sql);
        mysqli_stmt_bind_param($stmt, "i", $data["login_token"]);
        $res = mysqli_stmt_execute($stmt);
        if ($res === TRUE) {
            echo "Delete Success";
        } else {
            echo "Delete Error: " . mysqli_error($con);
        }
        mysqli_close($con);
        break;
    default:
        break;
}
?>