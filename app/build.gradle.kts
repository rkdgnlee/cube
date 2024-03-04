plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.mhg"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mhg"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    viewBinding{
        enable = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {


// 외부 라이브러리
    implementation ("io.github.ShawnLin013:number-picker:2.4.13")
    implementation("com.github.shuhart:StepView:v1.5.1")
    implementation("com.seosh817:circularseekbar:1.0.2")
    implementation("com.github.bumptech.glide:glide:4.11.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.tbuonomo:dotsindicator:5.0")
// 로그인 api
    implementation("com.navercorp.nid:oauth:5.9.0")
    implementation("com.kakao.sdk:v2-common:2.19.0")
    implementation("com.kakao.sdk:v2-user:2.19.0")
    implementation("com.kakao.sdk:v2-auth:2.19.0")
    implementation("com.google.gms:google-services:4.4.1")
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.1")
    implementation("com.google.firebase:firebase-analytics:21.5.1")
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("androidx.security:security-crypto:1.0.0")


//    implementation(files("libs/bbaton_dependancy.aar"))
// 카메라
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-video:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.guava:guava:29.0-android")

    // 통신
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.google.code.gson:gson:2.10")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.android.volley:volley:1.2.1")

    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.android.identity:identity-credential-android:20231002")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    val paging_version = "3.2.1"
    implementation("androidx.paging:paging-runtime:$paging_version")
    implementation ("androidx.browser:browser:1.7.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}