// Copyright 2021 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mediapipe.examples.arhandtracking;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutions.hands.HandLandmark;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsOptions;
import com.google.mediapipe.solutions.hands.HandsResult;

/**
 * Main activity of AR Hand Tracking app.
 * This app uses the camera to track hands in real-time and labels finger tips with numbers 1-5.
 */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  private Hands hands;
  // Run the pipeline and the model inference on GPU
  private static final boolean RUN_ON_GPU = true;

  private CameraInput cameraInput;
  private SolutionGlSurfaceView<HandsResult> glSurfaceView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupCameraMode();
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Restarts the camera and the opengl surface rendering.
    cameraInput = new CameraInput(this);
    cameraInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));
    glSurfaceView.post(this::startCamera);
    glSurfaceView.setVisibility(View.VISIBLE);
  }

  @Override
  protected void onPause() {
    super.onPause();
    glSurfaceView.setVisibility(View.GONE);
    cameraInput.close();
  }

  /** Sets up the AR hand tracking in camera mode. */
  private void setupCameraMode() {
    // Initializes MediaPipe Hands solution instance in streaming mode.
    hands =
        new Hands(
            this,
            HandsOptions.builder()
                .setStaticImageMode(false)
                .setMaxNumHands(2)
                .setRunOnGpu(RUN_ON_GPU)
                .build());
    hands.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Hands error:" + message));

    cameraInput = new CameraInput(this);
    cameraInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));

    // Initializes a new Gl surface view with our custom AR renderer.
    glSurfaceView =
        new SolutionGlSurfaceView<>(this, hands.getGlContext(), hands.getGlMajorVersion());
    glSurfaceView.setSolutionResultRenderer(new ARHandTrackingRenderer());
    glSurfaceView.setRenderInputImage(true);

    hands.setResultListener(
        handsResult -> {
          logFingerTips(handsResult);
          glSurfaceView.setRenderData(handsResult);
          glSurfaceView.requestRender();
        });

    // Start camera after the gl surface view is attached.
    glSurfaceView.post(this::startCamera);

    // Updates the preview layout.
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    frameLayout.removeAllViewsInLayout();
    frameLayout.addView(glSurfaceView);
    glSurfaceView.setVisibility(View.VISIBLE);
    frameLayout.requestLayout();
  }

  private void startCamera() {
    cameraInput.start(
        this,
        hands.getGlContext(),
        CameraInput.CameraFacing.FRONT,
        glSurfaceView.getWidth(),
        glSurfaceView.getHeight());
  }

  private void logFingerTips(HandsResult result) {
    if (result.multiHandLandmarks().isEmpty()) {
      return;
    }

    // Log finger tip positions for the first detected hand
    NormalizedLandmark thumbTip =
        result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.THUMB_TIP);
    NormalizedLandmark indexTip =
        result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.INDEX_FINGER_TIP);
    NormalizedLandmark middleTip =
        result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.MIDDLE_FINGER_TIP);
    NormalizedLandmark ringTip =
        result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.RING_FINGER_TIP);
    NormalizedLandmark pinkyTip =
        result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.PINKY_TIP);

    Log.i(
        TAG,
        String.format(
            "Finger tips - Thumb(1): (%.2f, %.2f), Index(2): (%.2f, %.2f), "
                + "Middle(3): (%.2f, %.2f), Ring(4): (%.2f, %.2f), Pinky(5): (%.2f, %.2f)",
            thumbTip.getX(),
            thumbTip.getY(),
            indexTip.getX(),
            indexTip.getY(),
            middleTip.getX(),
            middleTip.getY(),
            ringTip.getX(),
            ringTip.getY(),
            pinkyTip.getX(),
            pinkyTip.getY()));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (hands != null) {
      hands.close();
    }
  }
}
