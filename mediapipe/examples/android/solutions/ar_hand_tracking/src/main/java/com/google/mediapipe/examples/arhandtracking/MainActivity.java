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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
 * Supports switching between front and back cameras and start/stop camera control.
 */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  private Hands hands;
  // Run the pipeline and the model inference on GPU
  private static final boolean RUN_ON_GPU = true;

  private CameraInput cameraInput;
  private SolutionGlSurfaceView<HandsResult> glSurfaceView;

  // Track current camera facing
  private CameraInput.CameraFacing currentCameraFacing = CameraInput.CameraFacing.FRONT;
  private boolean isCameraRunning = false;
  
  // UI components
  private Button startStopButton;
  private ImageButton switchCameraButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupHandsTracking();
    setupButtons();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (isCameraRunning && cameraInput != null && glSurfaceView != null) {
      // Restarts the camera and the opengl surface rendering.
      cameraInput = new CameraInput(this);
      cameraInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));
      glSurfaceView.post(this::startCamera);
      glSurfaceView.setVisibility(View.VISIBLE);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (cameraInput != null) {
      cameraInput.close();
    }
    if (glSurfaceView != null) {
      glSurfaceView.setVisibility(View.GONE);
    }
  }

  /** Sets up the MediaPipe Hands tracking. */
  private void setupHandsTracking() {
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
  }

  /** Sets up button click listeners. */
  private void setupButtons() {
    startStopButton = findViewById(R.id.button_start_camera);
    switchCameraButton = findViewById(R.id.button_switch_camera);

    startStopButton.setOnClickListener(
        v -> {
          if (isCameraRunning) {
            stopCamera();
          } else {
            startCameraMode();
          }
        });

    switchCameraButton.setOnClickListener(
        v -> {
          if (isCameraRunning) {
            switchCamera();
          }
        });
  }

  /** Starts camera mode and displays the view. */
  private void startCameraMode() {
    isCameraRunning = true;
    
    // Update button text to "Stop Camera"
    startStopButton.setText(R.string.stop_camera);
    
    // Enable switch camera button
    switchCameraButton.setEnabled(true);

    // Updates the preview layout.
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    frameLayout.removeAllViewsInLayout();
    frameLayout.addView(glSurfaceView);
    glSurfaceView.setVisibility(View.VISIBLE);
    frameLayout.requestLayout();

    // Start camera after the gl surface view is attached.
    glSurfaceView.post(this::startCamera);
    
    Log.i(TAG, "Camera started");
  }

  /** Stops the camera. */
  private void stopCamera() {
    isCameraRunning = false;
    
    // Update button text to "Start Camera"
    startStopButton.setText(R.string.start_camera);
    
    // Disable switch camera button
    switchCameraButton.setEnabled(false);

    // Stop camera input
    if (cameraInput != null) {
      cameraInput.close();
    }

    // Hide the surface view
    if (glSurfaceView != null) {
      glSurfaceView.setVisibility(View.GONE);
    }

    // Clear the preview layout
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    frameLayout.removeAllViewsInLayout();
    
    Log.i(TAG, "Camera stopped");
  }

  /** Switches between front and back camera. */
  private void switchCamera() {
    // Toggle camera facing
    if (currentCameraFacing == CameraInput.CameraFacing.FRONT) {
      currentCameraFacing = CameraInput.CameraFacing.BACK;
      Log.i(TAG, "Switched to back camera");
    } else {
      currentCameraFacing = CameraInput.CameraFacing.FRONT;
      Log.i(TAG, "Switched to front camera");
    }

    // Restart camera with new facing
    if (cameraInput != null) {
      cameraInput.close();
    }

    cameraInput = new CameraInput(this);
    cameraInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));
    glSurfaceView.post(this::startCamera);
  }

  private void startCamera() {
    cameraInput.start(
        this,
        hands.getGlContext(),
        currentCameraFacing,
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
    if (cameraInput != null) {
      cameraInput.close();
    }
  }
}
