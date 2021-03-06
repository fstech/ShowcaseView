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

package com.github.amlcurran.showcaseview.targets;

import com.github.amlcurran.showcaseview.R;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.View;

/**
 * Target a view on the screen. This will centre the target on the view.
 */
public class ViewTarget implements Target {

  private final View mView;
  private float mRadius = 0;

  private int mX = -1;

  private int mY = -1;

  private boolean mExactHeight = false;

  private int mOuterRadius = -1;

  public ViewTarget(View view) {
    mView = view;
  }

  public ViewTarget(View view, float radius) {
    mView = view;
    mRadius = radius;
  }

  public ViewTarget(int viewId, Activity activity) {
    mView = activity.findViewById(viewId);
  }

  public ViewTarget setX(int dpX) {
    mX = dpToPixels(dpX);
    return this;
  }

  public ViewTarget setY(int dpY) {
    mY = dpToPixels(dpY);
    return this;
  }

  public ViewTarget setRadius(int dpRadius) {
    mRadius = dpToPixels(dpRadius);
    return this;
  }

  public ViewTarget setExactHeight() {
    mExactHeight = true;
    return this;
  }

  @Override
  public Point getPoint() {
    int[] location = new int[2];
    mView.getLocationInWindow(location);
    int x = mX != -1 ? mX : location[0] + mView.getWidth() / 2;
    int y = mY != -1 ? mY : location[1] + mView.getHeight() / 2;
    return new Point(x, y);
  }

  @Override
  public float getRadius() {
    if (mExactHeight) {
      mRadius = mView.getMeasuredHeight() / 2;
    }
    if (mRadius == 0) {
      mRadius = Math.max(mView.getMeasuredHeight(), mView.getMeasuredWidth()) / 2;
    }
    int outerRadius = mOuterRadius != -1 ? mOuterRadius : (int) mView.getResources().getDimension(R.dimen.showcase_radius_outer);
    return mRadius + outerRadius;
  }

  @Override
  public void setOuterRadius(int outerRadius) {
    mOuterRadius = outerRadius;
  }

  public int dpToPixels(int dp) {
    Resources resources = Resources.getSystem();
    return (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics()));
  }
}
