plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id ("kotlin-kapt")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.tangoplus.tangoq"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tangoplus.tangoq"
        minSdk = 27
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 46
        versionName = "1.55"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    viewBinding{
        enable = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}
//noinspection UseTomlInstead
dependencies {

    // 무결성 검증
    implementation("com.google.android.play:integrity:1.4.0")

    // 스켈레톤
    implementation("com.google.mediapipe:tasks-vision:0.10.0")

    // 외부 라이브러리
    implementation("com.github.shuhart:StepView:v1.5.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.tbuonomo:dotsindicator:5.0")
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("io.github.litao0621:nifty-slider:1.4.6")
    implementation("com.mikhaellopez:circularprogressbar:3.1.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.kizitonwose.calendar:view:2.5.1")
    implementation(libs.firebase.appcheck.ktx)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation("com.github.skydoves:balloon:1.6.4")
    implementation("com.github.skydoves:progressview:1.1.3")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.github.aabhasr1:OtpView:v1.1.2-ktx")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
    implementation("com.github.douglasjunior:android-simple-tooltip:1.1.0")
    implementation("com.arthenica:ffmpeg-kit-full-gpl:4.5.LTS")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
//    implementation("io.github.ParkSangGwon:tedpermission-normal:3.3.0")

    // api
    implementation("com.navercorp.nid:oauth:5.10.0")
    implementation("com.kakao.sdk:v2-common:2.19.0")
    implementation("com.kakao.sdk:v2-user:2.20.6")
    implementation("com.kakao.sdk:v2-auth:2.19.0")
    implementation("com.google.gms:google-services:4.4.2")
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-messaging-ktx:24.1.0")
    implementation("com.google.firebase:firebase-analytics:22.2.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.datastore:datastore-core:1.1.3")

    // 통신
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
//    implementation("com.android.volley:volley:1.2.1")
    implementation("com.github.yuriy-budiyev:code-scanner:2.3.0")
    implementation("androidx.work:work-runtime:2.10.0")

    // room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-rxjava3:$roomVersion")
    implementation("androidx.room:room-rxjava2:$roomVersion")
    implementation("androidx.room:room-guava:$roomVersion")
    implementation("androidx.room:room-testing:$roomVersion")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    annotationProcessor("android.arch.persistence.room:rxjava2:1.1.1")
    //noinspection KaptUsageInsteadOfKsp
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite:2.4.0")


    // 미디어
    val camera_version = "1.4.1"
    implementation("androidx.camera:camera-core:$camera_version")
    implementation("androidx.camera:camera-camera2:$camera_version")
    implementation("androidx.camera:camera-lifecycle:$camera_version")
    implementation("androidx.camera:camera-video:$camera_version")
    implementation("androidx.camera:camera-extensions:$camera_version")
    implementation("androidx.camera:camera-view:$camera_version")
    implementation(libs.androidx.camera.core)

    // 내부 라이브러리
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.protobuf:protobuf-javalite:4.26.1")

    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-effect:1.5.1")
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.support.v4)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}