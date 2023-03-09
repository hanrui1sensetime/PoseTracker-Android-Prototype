# PoseTracker-Android-Prototype
PoseTracker Android Demo Prototype, which is based on [mmdeploy](https://github.com/open-mmlab/mmdeploy/tree/dev-1.x)

Before start, you need a personal computer with linux system and an android mobile phone with Android version >= 11.0.

## QuickStart

Here is `app-release.apk` in `PoseTracker-Android-Prototype/app/release` folder.

Copy this file and install to the mobile phone, then you can enjoy it.

## Usage

**step 0.** Git clone this repo.

```
git clone https://github.com/hanrui1sensetime/PoseTracker-Android-Prototype.git
export POSETRACKER_DEMO_ROOT=${PWD}
```

**step 1.** Download OpenCV-android sdk and set as Java module.

```
wget https://github.com/opencv/opencv/releases/download/4.7.0/opencv-4.7.0-android-sdk.zip
unzip opencv-4.7.0-android-sdk.zip
ln -s OpenCV-android-sdk/sdk PoseTracker-Android-Prototype/openCVLibrary470
```

**step 2.** Cross compile MMDeploy SDK.

```
git clone -b dev-1.x https://github.com/open-mmlab/mmdeploy.git
git submodule update --init
mkdir -p mmdeploy/build && cd mmdeploy/build
```

Then you can follow [this doc](https://github.com/open-mmlab/mmdeploy/blob/dev-1.x/docs/en/01-how-to-build/android.md) to build mmdeploy sdk.

**step 3.** Prepare `jniLibs`.

`jniLibs` requires `libopencv_java4.so`, `libmmdeploy_java.so` and `libc++_shared.so`.

```
cd ${POSETRACKER_DEMO_ROOT}
mkdir PoseTracker-Android-Prototype/app/src/main/jniLibs/arm64-v8a
cp OpenCV-android-sdk/sdk/native/libs/arm64-v8a/libopencv_java4.so PoseTracker-Android-Prototype/app/src/main/jniLibs/arm64-v8a/
cp mmdeploy/build/lib/libmmdeploy_java.so PoseTracker-Android-Prototype/app/src/main/jniLibs/arm64-v8a/
cp ${NDK_PATH}/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/x86_64-linux-android/libc++_shared.so PoseTracker-Android-Prototype/app/src/main/jniLibs/arm64-v8a/
```

In the bash code above, the ${NDK_PATH} can be found in
the [build mmdeploy android doc](https://github.com/open-mmlab/mmdeploy/blob/dev-1.x/docs/en/01-how-to-build/android.md).

**step 4.** Open Android Studio and build the Android demo.

Link the mobile phone to PC, open Android Studio. Then click `Build->Make Project` and `Run->Run app`.

## How to Develop Android App by MMDeploy Java API

TODO.
