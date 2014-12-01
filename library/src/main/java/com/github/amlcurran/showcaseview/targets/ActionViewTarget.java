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

import android.app.Activity;
import android.graphics.Point;
import android.view.ViewParent;

public class ActionViewTarget implements Target {

  private final Activity mActivity;
  private final Type mType;

  private ViewTarget mViewTarget;
  private boolean mIsInitialized;

  public ActionViewTarget(Activity activity, Type type) {
    mActivity = activity;
    mType = type;
  }

  protected void setUp() {
    Reflector reflector = ReflectorFactory.getReflectorForActivity(mActivity);
    ViewParent p = reflector.getActionBarView(); //ActionBarView
    ActionBarViewWrapper actionBarViewWrapper = new ActionBarViewWrapper(p);
    switch (mType) {
    case SPINNER:
      mViewTarget = new ViewTarget(actionBarViewWrapper.getSpinnerView());
      break;

    case HOME:
      mViewTarget = new ViewTarget(reflector.getHomeButton());
      break;

    case OVERFLOW:
      mViewTarget = new ViewTarget(actionBarViewWrapper.getOverflowView());
      break;

    case TITLE:
      mViewTarget = new ViewTarget(actionBarViewWrapper.getTitleView());
      break;

    case MEDIA_ROUTE_BUTTON:
      mViewTarget = new ViewTarget(actionBarViewWrapper.getMediaRouterButtonView());
      break;
    }
    mIsInitialized = true;
  }

  @Override
  public Point getPoint() {
    if (!mIsInitialized) {
      setUp();
    }
    return mViewTarget.getPoint();
  }

  @Override
  public float getRadius() {
    if (!mIsInitialized) {
      setUp();
    }
    return mViewTarget.getRadius();
  }

  public enum Type {
    SPINNER, HOME, TITLE, OVERFLOW, MEDIA_ROUTE_BUTTON
  }
}
