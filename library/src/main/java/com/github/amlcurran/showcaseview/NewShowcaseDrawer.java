/*
 * Copyright 2014 Alex Curran
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.amlcurran.showcaseview;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;

/**
 * Created by curraa01 on 13/10/2013.
 */
class NewShowcaseDrawer extends StandardShowcaseDrawer {

  private final float outerRadiusSpace;
  private final Paint showcasePaint;

  public NewShowcaseDrawer(Resources resources) {
    super(resources);
    outerRadiusSpace = resources.getDimension(R.dimen.showcase_radius_outer);
    showcasePaint = new Paint();
    showcasePaint.setAntiAlias(true);
    showcasePaint.setStyle(Style.STROKE);
    showcasePaint.setStrokeWidth(outerRadiusSpace);
  }

  @Override
  public void setShowcaseColour(int color) {
    showcasePaint.setColor(color);
  }

  @Override
  public void drawShowcase(Bitmap buffer, float x, float y, float scaleMultiplier, float radius) {
    Canvas bufferCanvas = new Canvas(buffer);
    bufferCanvas.drawCircle(x, y, radius + outerRadiusSpace / 2, eraserPaint);
    bufferCanvas.drawCircle(x, y, radius, showcasePaint);
  }

  @Override
  public int getShowcaseWidth(float radius) {
    return (int) ((radius + outerRadiusSpace) * 2);
  }

  @Override
  public int getShowcaseHeight(float radius) {
    return (int) ((radius + outerRadiusSpace) * 2);
  }

  @Override
  public void setBackgroundColour(int backgroundColor) {
    this.backgroundColour = backgroundColor;
  }
}
