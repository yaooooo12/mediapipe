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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutioncore.ResultGlRenderer;
import com.google.mediapipe.solutions.hands.HandLandmark;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsResult;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A custom AR implementation of {@link ResultGlRenderer} to render {@link HandsResult}
 * with finger tip labels (1-5) and arrows.
 */
public class ARHandTrackingRenderer implements ResultGlRenderer<HandsResult> {
  private static final String TAG = "ARHandTrackingRenderer";

  private static final float[] LEFT_HAND_CONNECTION_COLOR = new float[] {0.2f, 1f, 0.2f, 1f};
  private static final float[] RIGHT_HAND_CONNECTION_COLOR = new float[] {1f, 0.2f, 0.2f, 1f};
  private static final float CONNECTION_THICKNESS = 25.0f;
  private static final float[] LEFT_HAND_LANDMARK_COLOR = new float[] {1f, 0.2f, 0.2f, 1f};
  private static final float[] RIGHT_HAND_LANDMARK_COLOR = new float[] {0.2f, 1f, 0.2f, 1f};
  private static final float LANDMARK_RADIUS = 0.008f;
  private static final int NUM_SEGMENTS = 120;

  // Finger tip arrow and label colors
  private static final float[] ARROW_COLOR = new float[] {1f, 1f, 0f, 1f}; // Yellow
  private static final float ARROW_LENGTH = 0.08f;
  private static final float ARROW_WIDTH = 0.03f;

  // Fingernail detection colors and sizes
  private static final float[] FINGERNAIL_COLOR = new float[] {1f, 0f, 0f, 1f}; // Red
  private static final float FINGERNAIL_WIDTH = 0.015f;  // Nail width
  private static final float FINGERNAIL_HEIGHT = 0.02f;  // Nail height
  private static final float FINGERNAIL_LINE_WIDTH = 5.0f; // Outline thickness
  private static final float FINGERNAIL_POINT_SIZE = 8.0f; // Point size for edge dots

  private static final String VERTEX_SHADER =
      "uniform mat4 uProjectionMatrix;\n"
          + "attribute vec4 vPosition;\n"
          + "void main() {\n"
          + "  gl_Position = uProjectionMatrix * vPosition;\n"
          + "}";
  private static final String FRAGMENT_SHADER =
      "precision mediump float;\n"
          + "uniform vec4 uColor;\n"
          + "void main() {\n"
          + "  gl_FragColor = uColor;\n"
          + "}";

  // Texture shader for rendering text labels
  private static final String TEXTURE_VERTEX_SHADER =
      "uniform mat4 uProjectionMatrix;\n"
          + "attribute vec4 vPosition;\n"
          + "attribute vec2 aTexCoord;\n"
          + "varying vec2 vTexCoord;\n"
          + "void main() {\n"
          + "  gl_Position = uProjectionMatrix * vPosition;\n"
          + "  vTexCoord = aTexCoord;\n"
          + "}";

  private static final String TEXTURE_FRAGMENT_SHADER =
      "precision mediump float;\n"
          + "varying vec2 vTexCoord;\n"
          + "uniform sampler2D uTexture;\n"
          + "void main() {\n"
          + "  gl_FragColor = texture2D(uTexture, vTexCoord);\n"
          + "}";

  private int program;
  private int positionHandle;
  private int projectionMatrixHandle;
  private int colorHandle;

  private int textureProgram;
  private int texturePositionHandle;
  private int textureProjectionMatrixHandle;
  private int textureCoordHandle;
  private int textureHandle;

  // Texture IDs for finger labels
  private Map<Integer, Integer> labelTextures = new HashMap<>();

  private int loadShader(int type, String shaderCode) {
    int shader = GLES20.glCreateShader(type);
    GLES20.glShaderSource(shader, shaderCode);
    GLES20.glCompileShader(shader);
    return shader;
  }

  @Override
  public void setupRendering() {
    // Setup basic shader program
    program = GLES20.glCreateProgram();
    int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
    int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
    GLES20.glAttachShader(program, vertexShader);
    GLES20.glAttachShader(program, fragmentShader);
    GLES20.glLinkProgram(program);
    positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
    projectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjectionMatrix");
    colorHandle = GLES20.glGetUniformLocation(program, "uColor");

    // Setup texture shader program for labels
    textureProgram = GLES20.glCreateProgram();
    int textureVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, TEXTURE_VERTEX_SHADER);
    int textureFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, TEXTURE_FRAGMENT_SHADER);
    GLES20.glAttachShader(textureProgram, textureVertexShader);
    GLES20.glAttachShader(textureProgram, textureFragmentShader);
    GLES20.glLinkProgram(textureProgram);
    texturePositionHandle = GLES20.glGetAttribLocation(textureProgram, "vPosition");
    textureProjectionMatrixHandle = GLES20.glGetUniformLocation(textureProgram, "uProjectionMatrix");
    textureCoordHandle = GLES20.glGetAttribLocation(textureProgram, "aTexCoord");
    textureHandle = GLES20.glGetUniformLocation(textureProgram, "uTexture");

    // Create label textures for fingers 1-5
    createLabelTextures();
  }

  private void createLabelTextures() {
    String[] labels = {"1", "2", "3", "4", "5"};
    for (int i = 0; i < labels.length; i++) {
      labelTextures.put(i, createTextTexture(labels[i]));
    }
  }

  private int createTextTexture(String text) {
    Paint paint = new Paint();
    paint.setTextSize(80);
    paint.setColor(Color.YELLOW);
    paint.setAntiAlias(true);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setStyle(Paint.Style.FILL);

    // Add stroke for better visibility
    paint.setShadowLayer(8f, 2f, 2f, Color.BLACK);

    float baseline = -paint.ascent();
    int width = 120;
    int height = 120;

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(Color.TRANSPARENT);
    canvas.drawText(text, width / 2, baseline + 20, paint);

    int[] textureIds = new int[1];
    GLES20.glGenTextures(1, textureIds, 0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    bitmap.recycle();

    return textureIds[0];
  }

  @Override
  public void renderResult(HandsResult result, float[] projectionMatrix) {
    if (result == null) {
      return;
    }
    GLES20.glUseProgram(program);
    GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0);
    GLES20.glLineWidth(CONNECTION_THICKNESS);

    int numHands = result.multiHandLandmarks().size();
    for (int i = 0; i < numHands; ++i) {
      boolean isLeftHand = result.multiHandedness().get(i).getLabel().equals("Left");
      List<NormalizedLandmark> landmarks = result.multiHandLandmarks().get(i).getLandmarkList();

      drawConnections(
          landmarks,
          isLeftHand ? LEFT_HAND_CONNECTION_COLOR : RIGHT_HAND_CONNECTION_COLOR);

      for (NormalizedLandmark landmark : landmarks) {
        drawCircle(
            landmark.getX(),
            landmark.getY(),
            isLeftHand ? LEFT_HAND_LANDMARK_COLOR : RIGHT_HAND_LANDMARK_COLOR);
      }

      // Draw arrows and labels for finger tips
      drawFingerTipAnnotations(landmarks, projectionMatrix);

      // Draw fingernail outlines
      drawFingernails(landmarks);
    }
  }

  private void drawFingerTipAnnotations(List<NormalizedLandmark> landmarks, float[] projectionMatrix) {
    // Finger tip landmark indices and their corresponding labels
    int[] fingerTips = {
        HandLandmark.THUMB_TIP,        // 4 -> Label "1"
        HandLandmark.INDEX_FINGER_TIP,  // 8 -> Label "2"
        HandLandmark.MIDDLE_FINGER_TIP, // 12 -> Label "3"
        HandLandmark.RING_FINGER_TIP,   // 16 -> Label "4"
        HandLandmark.PINKY_TIP          // 20 -> Label "5"
    };

    for (int i = 0; i < fingerTips.length; i++) {
      NormalizedLandmark tip = landmarks.get(fingerTips[i]);
      float tipX = tip.getX();
      float tipY = tip.getY();

      // Draw arrow pointing to the finger tip
      drawArrow(tipX, tipY);

      // Draw number label
      drawTextLabel(i, tipX, tipY, projectionMatrix);
    }
  }

  private void drawArrow(float x, float y) {
    // Draw arrow pointing down towards the finger tip
    float arrowTipX = x;
    float arrowTipY = y - ARROW_LENGTH;
    float arrowLeftX = x - ARROW_WIDTH / 2;
    float arrowLeftY = y - ARROW_LENGTH * 0.6f;
    float arrowRightX = x + ARROW_WIDTH / 2;
    float arrowRightY = y - ARROW_LENGTH * 0.6f;

    // Arrow triangle
    float[] vertices = {
        arrowTipX, arrowTipY, 0,
        arrowLeftX, arrowLeftY, 0,
        arrowRightX, arrowRightY, 0
    };

    FloatBuffer vertexBuffer =
        ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices);
    vertexBuffer.position(0);

    GLES20.glUniform4fv(colorHandle, 1, ARROW_COLOR, 0);
    GLES20.glEnableVertexAttribArray(positionHandle);
    GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
  }

  private void drawTextLabel(int labelIndex, float x, float y, float[] projectionMatrix) {
    if (!labelTextures.containsKey(labelIndex)) {
      return;
    }

    GLES20.glUseProgram(textureProgram);
    GLES20.glUniformMatrix4fv(textureProjectionMatrixHandle, 1, false, projectionMatrix, 0);

    // Enable blending for transparent text background
    GLES20.glEnable(GLES20.GL_BLEND);
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

    float labelSize = 0.04f;
    float labelX = x;
    float labelY = y - ARROW_LENGTH - labelSize;

    // Position vertices for the label quad
    float[] vertices = {
        labelX - labelSize, labelY - labelSize, 0,  // Bottom-left
        labelX + labelSize, labelY - labelSize, 0,  // Bottom-right
        labelX - labelSize, labelY + labelSize, 0,  // Top-left
        labelX + labelSize, labelY + labelSize, 0   // Top-right
    };

    float[] texCoords = {
        0, 1,  // Bottom-left
        1, 1,  // Bottom-right
        0, 0,  // Top-left
        1, 0   // Top-right
    };

    FloatBuffer vertexBuffer =
        ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices);
    vertexBuffer.position(0);

    FloatBuffer texCoordBuffer =
        ByteBuffer.allocateDirect(texCoords.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords);
    texCoordBuffer.position(0);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, labelTextures.get(labelIndex));
    GLES20.glUniform1i(textureHandle, 0);

    GLES20.glEnableVertexAttribArray(texturePositionHandle);
    GLES20.glVertexAttribPointer(texturePositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

    GLES20.glEnableVertexAttribArray(textureCoordHandle);
    GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

    GLES20.glDisable(GLES20.GL_BLEND);
    GLES20.glUseProgram(program);
  }

  private void drawFingernails(List<NormalizedLandmark> landmarks) {
    // Define fingernail positions using PIP, DIP and TIP landmarks
    // For thumb, use MCP, IP and TIP
    int[][] fingerNailLandmarks = {
        {HandLandmark.THUMB_MCP, HandLandmark.THUMB_IP, HandLandmark.THUMB_TIP},           // Thumb
        {HandLandmark.INDEX_FINGER_PIP, HandLandmark.INDEX_FINGER_DIP, HandLandmark.INDEX_FINGER_TIP},  // Index
        {HandLandmark.MIDDLE_FINGER_PIP, HandLandmark.MIDDLE_FINGER_DIP, HandLandmark.MIDDLE_FINGER_TIP}, // Middle
        {HandLandmark.RING_FINGER_PIP, HandLandmark.RING_FINGER_DIP, HandLandmark.RING_FINGER_TIP},    // Ring
        {HandLandmark.PINKY_PIP, HandLandmark.PINKY_DIP, HandLandmark.PINKY_TIP}           // Pinky
    };

    GLES20.glUniform4fv(colorHandle, 1, FINGERNAIL_COLOR, 0);

    for (int[] nailLandmarks : fingerNailLandmarks) {
      NormalizedLandmark pip = landmarks.get(nailLandmarks[0]);
      NormalizedLandmark dip = landmarks.get(nailLandmarks[1]);
      NormalizedLandmark tip = landmarks.get(nailLandmarks[2]);

      // Draw precise fingernail edge with dense points
      drawFingernailOutline(pip, dip, tip);
    }
  }

  private void drawFingernailOutline(NormalizedLandmark pip, NormalizedLandmark dip, NormalizedLandmark tip) {
    // Calculate finger direction vector from DIP to TIP
    float fingerDirX = tip.getX() - dip.getX();
    float fingerDirY = tip.getY() - dip.getY();
    float fingerLength = (float) Math.sqrt(fingerDirX * fingerDirX + fingerDirY * fingerDirY);

    if (fingerLength < 0.001f) return; // Avoid division by zero

    // Normalize direction vector
    fingerDirX /= fingerLength;
    fingerDirY /= fingerLength;

    // Calculate perpendicular vector for nail width
    float perpX = -fingerDirY;
    float perpY = fingerDirX;

    // Calculate finger width based on PIP to DIP distance
    float pipToDipX = dip.getX() - pip.getX();
    float pipToDipY = dip.getY() - pip.getY();
    float fingerWidth = (float) Math.sqrt(pipToDipX * pipToDipX + pipToDipY * pipToDipY) * 0.6f;

    // Fingernail starts at about 60% from DIP to TIP (adjusted for more accurate positioning)
    float nailStartRatio = 0.60f;
    float nailStartX = dip.getX() + fingerDirX * fingerLength * nailStartRatio;
    float nailStartY = dip.getY() + fingerDirY * fingerLength * nailStartRatio;

    // Fingernail ends at about 98% from DIP to TIP (very close to tip)
    float nailEndRatio = 0.98f;
    float nailEndX = dip.getX() + fingerDirX * fingerLength * nailEndRatio;
    float nailEndY = dip.getY() + fingerDirY * fingerLength * nailEndRatio;

    // Use many more points for precise edge detection
    int perimeterPoints = 60; // High density points around nail edge
    float[] vertices = new float[perimeterPoints * 3];

    int idx = 0;
    for (int i = 0; i < perimeterPoints; i++) {
      float t = (float) i / perimeterPoints;
      float x, y;

      if (t < 0.25f) {
        // Left side (bottom to top)
        float sideT = t / 0.25f;
        x = nailStartX - perpX * fingerWidth * 0.42f;
        y = nailStartY - perpY * fingerWidth * 0.42f;
        float endX = nailEndX - perpX * fingerWidth * 0.38f;
        float endY = nailEndY - perpY * fingerWidth * 0.38f;
        x += (endX - x) * sideT;
        y += (endY - y) * sideT;
      } else if (t < 0.75f) {
        // Top arc (left to right, rounded)
        float arcT = (t - 0.25f) / 0.5f;
        // Create smooth arc from left to right
        float angle = (float) Math.PI * (1.0f - arcT);
        x = nailEndX + (float) Math.cos(angle) * perpX * fingerWidth * 0.38f;
        y = nailEndY + (float) Math.cos(angle) * perpY * fingerWidth * 0.38f;
        // Add forward offset for natural curved nail shape
        float curveDepth = (float) Math.sin(angle) * fingerWidth * 0.12f;
        x += fingerDirX * curveDepth;
        y += fingerDirY * curveDepth;
      } else {
        // Right side (top to bottom)
        float sideT = (t - 0.75f) / 0.25f;
        float startX = nailEndX + perpX * fingerWidth * 0.38f;
        float startY = nailEndY + perpY * fingerWidth * 0.38f;
        x = nailStartX + perpX * fingerWidth * 0.42f;
        y = nailStartY + perpY * fingerWidth * 0.42f;
        x = startX + (x - startX) * sideT;
        y = startY + (y - startY) * sideT;
      }

      vertices[idx++] = x;
      vertices[idx++] = y;
      vertices[idx++] = 0;
    }

    FloatBuffer vertexBuffer =
        ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices);
    vertexBuffer.position(0);

    // Draw as dense line loop for precise edge visualization
    GLES20.glEnableVertexAttribArray(positionHandle);
    GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    GLES20.glLineWidth(FINGERNAIL_LINE_WIDTH);
    GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, perimeterPoints);
  }

  private void drawConnections(List<NormalizedLandmark> handLandmarkList, float[] colorArray) {
    GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);
    for (Hands.Connection c : Hands.HAND_CONNECTIONS) {
      NormalizedLandmark start = handLandmarkList.get(c.start());
      NormalizedLandmark end = handLandmarkList.get(c.end());
      float[] vertex = {start.getX(), start.getY(), end.getX(), end.getY()};
      FloatBuffer vertexBuffer =
          ByteBuffer.allocateDirect(vertex.length * 4)
              .order(ByteOrder.nativeOrder())
              .asFloatBuffer()
              .put(vertex);
      vertexBuffer.position(0);
      GLES20.glEnableVertexAttribArray(positionHandle);
      GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
      GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
    }
  }

  private void drawCircle(float x, float y, float[] colorArray) {
    GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);
    int vertexCount = NUM_SEGMENTS + 2;
    float[] vertices = new float[vertexCount * 3];
    vertices[0] = x;
    vertices[1] = y;
    vertices[2] = 0;
    for (int i = 1; i < vertexCount; i++) {
      float angle = 2.0f * i * (float) Math.PI / NUM_SEGMENTS;
      int currentIndex = 3 * i;
      vertices[currentIndex] = x + (float) (LANDMARK_RADIUS * Math.cos(angle));
      vertices[currentIndex + 1] = y + (float) (LANDMARK_RADIUS * Math.sin(angle));
      vertices[currentIndex + 2] = 0;
    }
    FloatBuffer vertexBuffer =
        ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices);
    vertexBuffer.position(0);
    GLES20.glEnableVertexAttribArray(positionHandle);
    GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);
  }

  public void release() {
    GLES20.glDeleteProgram(program);
    GLES20.glDeleteProgram(textureProgram);
    for (int textureId : labelTextures.values()) {
      GLES20.glDeleteTextures(1, new int[] {textureId}, 0);
    }
    labelTextures.clear();
  }
}
