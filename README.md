# AXIS

On-device Android coach. CameraX + MediaPipe Pose watch your squat, push-up or plank, draw the skeleton, count the work, and speak when the line breaks.

Built as a portfolio piece: live vision, clean architecture, Compose UI, offline storage.

## What it does

- **Live form** — front camera, 33-point skeleton overlay, joints flare coral when a cue fires
- **Three lifts** — squat (depth / knees / torso), push-up (elbows + body line), plank (hold timer)
- **Voice + haptics** — TTS cues, click on every counted rep
- **Recap + history** — form score ring, weekly bars, Room log
- **Evening reminder** — WorkManager at 19:00
- **Private** — the pose model runs on-device. No account, no cloud

## Stack

Kotlin · Jetpack Compose · Material 3 · Hilt · Room · DataStore · CameraX · MediaPipe Tasks Vision · WorkManager · JUnit

```
app.axis.coach
  ui/           Compose screens (home, session, recap, history, onboarding)
  domain/       Exercise models + pure form analyzers (unit-tested)
  pose/         CameraX → MediaPipe live stream
  data/         Room sessions + DataStore prefs
  audio/        TTS + haptics
  notify/       Daily reminder worker
```

## Run it

1. Android Studio Ladybug / Narwhal or newer
2. Open this folder
3. Let Gradle sync
4. Run on a **physical phone** — the emulator camera is useless for pose
5. Grant camera. Side-on, full body, ~2.5 m from the phone

If `pose_landmarker_lite.task` is missing from `app/src/main/assets/`, download it:

https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task

## Why this is a portfolio app

Recruiters get three things in one APK: modern Compose, a real CameraX/ML pipeline, and domain logic you can read without Android (the analyzers are plain Kotlin + tests). The UI is a custom dark athletic system, not a default purple scaffold.
