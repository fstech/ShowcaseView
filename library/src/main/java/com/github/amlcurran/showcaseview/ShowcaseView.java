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

import static com.github.amlcurran.showcaseview.AnimationFactory.AnimationEndListener;
import static com.github.amlcurran.showcaseview.AnimationFactory.AnimationStartListener;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener.HideReason;
import com.github.amlcurran.showcaseview.targets.Target;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * A view which allows you to showcase areas of your app with an explanation.
 */
public class ShowcaseView extends RelativeLayout
    implements View.OnTouchListener, ShowcaseViewApi {

  private static final int HOLO_BLUE = Color.parseColor("#33B5E5");

  private final Button mNextButton;
  private final Button mSkipButton;
  private final Button mBackButton;
  private final View mTextContainer;
  private final TextView mTitleTextView;
  private final TextView mDetailTextView;
  private final View mNavigationButtonsContainer;
  private ImageView mImageView;

  private final ShowcaseDrawer showcaseDrawer;
  private final AnimationFactory animationFactory;
  private final ShotStateStore shotStateStore;

  // Showcase metrics
  private int showcaseX = -1;
  private int showcaseY = -1;
  private float showcaseRadius = 0;
  private float scaleMultiplier = 1f;

  // Touch items
  private boolean hasCustomClickListener = false;
  private boolean blockTouches = true;
  private boolean blockInsideWindowTouches = false;
  private boolean hideOnTouch = false;
  private OnShowcaseEventListener mEventListener = OnShowcaseEventListener.NONE;

  private boolean hasNoTarget = false;
  private Bitmap bitmapBuffer;

  // Animation items
  private long fadeInMillis;
  private long fadeOutMillis;
  private boolean isShowing;

  protected ShowcaseView(Context context, boolean newStyle) {
    this(context, null, R.styleable.CustomTheme_showcaseViewStyle, newStyle);
  }

  protected ShowcaseView(Context context, AttributeSet attrs, int defStyle, boolean newStyle) {
    super(context, attrs, defStyle);

    ApiUtils apiUtils = new ApiUtils();
    animationFactory = new AnimatorAnimationFactory();
    shotStateStore = new ShotStateStore(context);

    apiUtils.setFitsSystemWindowsCompat(this);
    getViewTreeObserver().addOnGlobalLayoutListener(new UpdateOnGlobalLayout());

    // Get the attributes for the ShowcaseView
    final TypedArray styled = context.getTheme()
        .obtainStyledAttributes(attrs, R.styleable.ShowcaseView, R.attr.showcaseViewStyle,
            R.style.ShowcaseView);

    // Set the default animation times
    fadeInMillis = getResources().getInteger(android.R.integer.config_mediumAnimTime);
    fadeOutMillis = getResources().getInteger(android.R.integer.config_mediumAnimTime);

    mSkipButton = (Button) LayoutInflater.from(context).inflate(R.layout.showcase_skip_button, null);
    mTextContainer = LayoutInflater.from(context).inflate(R.layout.showcase_text, null);
    mTitleTextView = (TextView) mTextContainer.findViewById(R.id.text_title);
    mDetailTextView = (TextView) mTextContainer.findViewById(R.id.text_detail);
    mNavigationButtonsContainer = LayoutInflater.from(context).inflate(R.layout.showcase_navigation_buttons, null);
    mNextButton = (Button) mNavigationButtonsContainer.findViewById(R.id.showcase_next_button);
    mBackButton = (Button) mNavigationButtonsContainer.findViewById(R.id.showcase_back_button);
    if (!hasCustomClickListener) {
      mNextButton.setOnClickListener(nextOnClickListener);
      mBackButton.setOnClickListener(backOnClickListener);
    }
    if (newStyle) {
      showcaseDrawer = new NewShowcaseDrawer(getResources());
    } else {
      showcaseDrawer = new StandardShowcaseDrawer(getResources());
    }

    updateStyle(styled, false);

    init();
  }

  private void init() {

    setOnTouchListener(this);

    if (mSkipButton.getParent() == null) {
      int margin = (int) getResources().getDimension(R.dimen.button_margin);
      RelativeLayout.LayoutParams lps = (LayoutParams) generateDefaultLayoutParams();
      lps.addRule(RelativeLayout.ALIGN_PARENT_TOP);
      lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
      lps.setMargins(margin, margin + getStatusBarHeight(), margin, margin);
      mSkipButton.setLayoutParams(lps);
      mSkipButton.setText(R.string.skip);
      if (!hasCustomClickListener) {
        mSkipButton.setOnClickListener(skipOnClickListener);
      }
      addView(mSkipButton);
    }

    if (mTextContainer.getParent() == null) {
      int margin = (int) getResources().getDimension(R.dimen.button_margin);
      RelativeLayout.LayoutParams lps = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lps.setMargins(margin, margin, margin, margin);
      mTextContainer.setLayoutParams(lps);
      addView(mTextContainer);
    }

    if (mNavigationButtonsContainer.getParent() == null) {
      int margin = (int) getResources().getDimension(R.dimen.button_margin);
      RelativeLayout.LayoutParams lps = (LayoutParams) generateDefaultLayoutParams();
      lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
      lps.setMargins(margin, margin, margin, margin);
      mNavigationButtonsContainer.setLayoutParams(lps);
      addView(mNavigationButtonsContainer);
    }
  }

  public void initImage() {
    if (mImageView != null) {
      int margin = (int) getResources().getDimension(R.dimen.button_margin);
      RelativeLayout.LayoutParams lps = (LayoutParams) generateDefaultLayoutParams();
      lps.addRule(RelativeLayout.ALIGN_PARENT_TOP);
      lps.addRule(RelativeLayout.CENTER_HORIZONTAL);
      lps.setMargins(margin, getStatusBarHeight() + margin, margin, margin);
      mImageView.setLayoutParams(lps);
      addView(mImageView);
    }
  }

  public Rect getImageViewRect() {
    int margin = (int) getResources().getDimension(R.dimen.button_margin);
    Rect rect = new Rect();
    rect.set(mImageView.getLeft(), mImageView.getTop(),
        mImageView.getMeasuredWidth(), 2 * margin + mImageView.getMeasuredHeight());
    return rect;
  }

  private int getStatusBarHeight() {
    int statusBarHeight = getResources().getIdentifier("status_bar_height", "dimen", "android");
    return getResources().getDimensionPixelSize(statusBarHeight);
  }

  public boolean hasImageView() {
    return mImageView != null;
  }

  private boolean hasShot() {
    return shotStateStore.hasShot();
  }

  void setShowcasePosition(Point point) {
    setShowcasePosition(point.x, point.y);
  }

  void setShowcasePosition(int x, int y) {
    if (shotStateStore.hasShot()) {
      return;
    }
    showcaseX = x;
    showcaseY = y;
    recalculateText();
    invalidate();
  }

  public void setTarget(final Target target) {
    setShowcase(target, false);
  }

  public void setImage(final int drawable) {
    if (drawable != -1) {
      mImageView = new ImageView(this.getContext());
      mImageView.setImageDrawable(getResources().getDrawable(drawable));
    }
  }

  public void setShowcase(final Target target, final boolean animate) {
    postDelayed(new Runnable() {
      @Override
      public void run() {

        if (!shotStateStore.hasShot()) {

          updateBitmap();
          if (target != null) {
            Point targetPoint = target.getPoint();
            if (targetPoint != null) {
              hasNoTarget = false;
              if (animate) {
                animationFactory.animateTargetToPoint(ShowcaseView.this, targetPoint, target.getRadius());
              } else {
                showcaseRadius = target.getRadius();
                setShowcasePosition(targetPoint);
              }
            } else {
              hasNoTarget = true;
              invalidate();
            }
          } else {
            hasNoTarget = true;
            invalidate();
          }
        }
      }
    }, 100);
  }

  private void updateBitmap() {
    if ((bitmapBuffer == null || haveBoundsChanged()) && getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
      if (bitmapBuffer != null)
        bitmapBuffer.recycle();
      bitmapBuffer = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
    }
  }

  private boolean haveBoundsChanged() {
    return getMeasuredWidth() != bitmapBuffer.getWidth() ||
        getMeasuredHeight() != bitmapBuffer.getHeight();
  }

  public boolean hasShowcaseView() {
    return (showcaseX != 1000000 && showcaseY != 1000000) && !hasNoTarget;
  }

  public void setShowcaseX(int x) {
    setShowcasePosition(x, showcaseY);
  }

  public void setShowcaseY(int y) {
    setShowcasePosition(showcaseX, y);
  }

  public int getShowcaseX() {
    return showcaseX;
  }

  public int getShowcaseY() {
    return showcaseY;
  }

  /**
   * Override the standard button click event
   *
   * @param listener Listener to listen to on click events
   */
  public void overrideButtonClick(OnClickListener listener) {
    if (shotStateStore.hasShot()) {
      return;
    }
    if (mNextButton != null) {
      if (listener != null) {
        mNextButton.setOnClickListener(listener);
      } else {
        mNextButton.setOnClickListener(nextOnClickListener);
      }
    }
    hasCustomClickListener = true;
  }

  public void setOnShowcaseEventListener(OnShowcaseEventListener listener) {
    if (listener != null) {
      mEventListener = listener;
    } else {
      mEventListener = OnShowcaseEventListener.NONE;
    }
  }

  public void setButtonText(CharSequence text) {
    if (mNextButton != null) {
      mNextButton.setText(text);
    }
  }

  private void recalculateText() {
    RelativeLayout.LayoutParams textParams = (LayoutParams) mTextContainer.getLayoutParams();
    if (showcaseY - showcaseRadius * 2 <= 0) {
      textParams.addRule(CENTER_IN_PARENT);
      setButtonPositions(true);
    } else if (showcaseY + showcaseRadius * 2 >= getMeasuredHeight()) {
      textParams.addRule(CENTER_IN_PARENT);
      setButtonPositions(false);
    } else if (showcaseY <= getMeasuredHeight() / 2) {
      //TODO: Check if these numbers are sane
      textParams.addRule(ALIGN_PARENT_TOP);
      int margin = (int) getResources().getDimension(R.dimen.button_margin);
      int marginTop = (int) getResources().getDimension(R.dimen.showcase_margin);
      textParams.setMargins(margin, (int) (marginTop + showcaseY + showcaseRadius), margin, margin);
      setButtonPositions(true);
    } else {
      //TODO: Check if these numbers are sane
      textParams.addRule(ALIGN_PARENT_BOTTOM);
      int margin = (int) getResources().getDimension(R.dimen.button_margin);
      int marginTop = (int) getResources().getDimension(R.dimen.showcase_margin);
      textParams.setMargins(margin, margin, margin,  (int) (marginTop + (getMeasuredHeight() - showcaseY) + showcaseRadius));
      setButtonPositions(true);
    }
    mTextContainer.requestLayout();
    mNavigationButtonsContainer.requestLayout();
  }

  private void setButtonPositions(boolean bottom) {
    RelativeLayout.LayoutParams buttonParams = (LayoutParams) mNavigationButtonsContainer.getLayoutParams();
    if (bottom) {
      buttonParams.addRule(ALIGN_PARENT_BOTTOM);
    } else {
      buttonParams.addRule(BELOW, mTextContainer.getId());
    }
  }

  @SuppressWarnings("NullableProblems")
  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (showcaseX < 0 || showcaseY < 0 || shotStateStore.hasShot() || bitmapBuffer == null) {
      super.dispatchDraw(canvas);
      return;
    }

    //Draw background color
    showcaseDrawer.erase(bitmapBuffer);

    // Draw the showcase drawable
    if (!hasNoTarget) {
      showcaseDrawer.drawShowcase(bitmapBuffer, showcaseX, showcaseY, scaleMultiplier, showcaseRadius);
    }

    showcaseDrawer.drawToCanvas(canvas, bitmapBuffer);

    super.dispatchDraw(canvas);
  }

  @Override
  public void hide() {
    dispatchHide(HideReason.NEXT);
  }

  @Override
  public void skip() {
    dispatchHide(HideReason.SKIP);
  }

  public void back() {
    dispatchHide(HideReason.BACK);
  }

  public void dispatchHide(HideReason reason) {
    clearBitmap();
    // If the type is set to one-shot, store that it has shot
    shotStateStore.storeShot();
    fadeOutShowcase(reason);
  }


  private void clearBitmap() {
    if (bitmapBuffer != null && !bitmapBuffer.isRecycled()) {
      bitmapBuffer.recycle();
      bitmapBuffer = null;
    }
  }

  private void fadeOutShowcase(final HideReason reason) {
    animationFactory.fadeOutView(this, fadeOutMillis, new AnimationEndListener() {
      @Override
      public void onAnimationEnd() {
        setVisibility(View.GONE);
        isShowing = false;
        mEventListener.onShowcaseViewDidHide(ShowcaseView.this, reason);
      }
    });
  }

  @Override
  public void show() {
    isShowing = true;
    mEventListener.onShowcaseViewShow(this);
    fadeInShowcase();
  }

  private void fadeInShowcase() {
    animationFactory.fadeInView(this, fadeInMillis,
        new AnimationStartListener() {
          @Override
          public void onAnimationStart() {
            setVisibility(View.VISIBLE);
          }
        }
    );
  }

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {

    float xDelta = Math.abs(motionEvent.getRawX() - showcaseX);
    float yDelta = Math.abs(motionEvent.getRawY() - showcaseY);
    double distanceFromFocus = Math.sqrt(Math.pow(xDelta, 2) + Math.pow(yDelta, 2));

    if (MotionEvent.ACTION_UP == motionEvent.getAction() &&
        hideOnTouch && distanceFromFocus > showcaseRadius) {
      this.hide();
      return true;
    }

    return (blockTouches && distanceFromFocus > showcaseRadius) ||
        (blockInsideWindowTouches && distanceFromFocus <= showcaseRadius);
  }

  private static void insertShowcaseView(ShowcaseView showcaseView, Activity activity) {
    showcaseView.initImage();
    ((ViewGroup) activity.getWindow().getDecorView()).addView(showcaseView);
    if (!showcaseView.hasShot()) {
      showcaseView.show();
    } else {
      showcaseView.hideImmediate();
    }
  }

  private void hideImmediate() {
    isShowing = false;
    setVisibility(GONE);
  }

  @Override
  public void setContentTitle(CharSequence title) {
    mTitleTextView.setText(title);
  }

  @Override
  public void setContentText(CharSequence text) {
    mDetailTextView.setText(text);
  }

  public void hideButton() {
    mNextButton.setVisibility(GONE);
    mSkipButton.setVisibility(GONE);
    mBackButton.setVisibility(GONE);
  }

  public void showButton() {
    mNextButton.setVisibility(VISIBLE);
    mSkipButton.setVisibility(VISIBLE);
    mBackButton.setVisibility(VISIBLE);
  }

  /**
   * Builder class which allows easier creation of {@link ShowcaseView}s.
   * It is recommended that you use this Builder class.
   */
  public static class Builder {

    final ShowcaseView showcaseView;
    private final Activity activity;

    public Builder(Activity activity) {
      this(activity, false);
    }

    public Builder(Activity activity, boolean useNewStyle) {
      this.activity = activity;
      this.showcaseView = new ShowcaseView(activity, useNewStyle);
    }

    /**
     * Create the {@link com.github.amlcurran.showcaseview.ShowcaseView} and show it.
     *
     * @return the created ShowcaseView
     */
    public ShowcaseView build() {
      insertShowcaseView(showcaseView, activity);
      return showcaseView;
    }

    /**
     * Set the title text shown on the ShowcaseView.
     */
    public Builder setContentTitle(int resId) {
      return setContentTitle(activity.getString(resId));
    }

    /**
     * Set the title text shown on the ShowcaseView.
     */
    public Builder setContentTitle(CharSequence title) {
      showcaseView.setContentTitle(title);
      return this;
    }

    /**
     * Set the descriptive text shown on the ShowcaseView.
     */
    public Builder setContentText(int resId) {
      return setContentText(activity.getString(resId));
    }

    /**
     * Set the descriptive text shown on the ShowcaseView.
     */
    public Builder setContentText(CharSequence text) {
      showcaseView.setContentText(text);
      return this;
    }

    /**
     * Set the target of the showcase.
     *
     * @param target a {@link com.github.amlcurran.showcaseview.targets.Target} representing
     * the item to showcase (e.g., a button, or action item).
     */
    public Builder setTarget(Target target) {
      showcaseView.setTarget(target);
      return this;
    }

    /**
     * Instead of the target, show the image.
     *
     * @param drawable Drawable to be draw on the showcase.
     */
    public Builder setImage(int drawable) {
      showcaseView.setImage(drawable);
      return this;
    }

    /**
     * Set the style of the ShowcaseView. See the sample app for example styles.
     */
    public Builder setStyle(int theme) {
      showcaseView.setStyle(theme);
      return this;
    }

    /**
     * Set a listener which will override the button clicks.
     * <p/>
     * Note that you will have to manually hide the ShowcaseView
     */
    public Builder setOnClickListener(OnClickListener onClickListener) {
      showcaseView.overrideButtonClick(onClickListener);
      return this;
    }

    /**
     * Don't make the ShowcaseView block touches on itself. This doesn't
     * block touches in the showcased area.
     * <p/>
     * By default, the ShowcaseView does block touches
     */
    public Builder doNotBlockTouches() {
      showcaseView.setBlocksTouches(false);
      return this;
    }

    /**
     * Blocks touches in the showcased area.
     */
    public Builder blockInsideWindowTouches(boolean value) {
      showcaseView.setBlocksInsideWindowTouches(value);
      return this;
    }

    /**
     * Make this ShowcaseView hide when the user touches outside the showcased area.
     * This enables {@link #doNotBlockTouches()} as well.
     * <p/>
     * By default, the ShowcaseView doesn't hide on touch.
     */
    public Builder hideOnTouchOutside() {
      showcaseView.setBlocksTouches(true);
      showcaseView.setHideOnTouchOutside(true);
      return this;
    }

    /**
     * Set the ShowcaseView to only ever show once.
     *
     * @param shotId a unique identifier (<em>across the app</em>) to store
     * whether this ShowcaseView has been shown.
     */
    public Builder singleShot(long shotId) {
      showcaseView.setSingleShot(shotId);
      return this;
    }

    public Builder setShowcaseEventListener(OnShowcaseEventListener showcaseEventListener) {
      showcaseView.setOnShowcaseEventListener(showcaseEventListener);
      return this;
    }

    public Builder setSkipButtonEnabled(boolean skipButtonEnabled) {
      showcaseView.mSkipButton.setVisibility(skipButtonEnabled ? VISIBLE : GONE);
      return this;
    }

    public Builder setBackButtonEnabled(boolean backButtonEnabled) {
      showcaseView.mBackButton.setVisibility(backButtonEnabled ? VISIBLE : GONE);
      return this;
    }

    public Builder setNextButtonText(int textResId) {
      showcaseView.mNextButton.setText(textResId);
      return this;
    }
  }


  /**
   * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#setSingleShot(long)
   */
  private void setSingleShot(long shotId) {
    shotStateStore.setSingleShot(shotId);
  }

  /**
   * Change the position of the ShowcaseView's button from the default bottom-right position.
   *
   * @param layoutParams a {@link android.widget.RelativeLayout.LayoutParams} representing
   * the new position of the button
   */

  /**
   * Set the duration of the fading in and fading out of the ShowcaseView
   */
  private void setFadeDurations(long fadeInMillis, long fadeOutMillis) {
    this.fadeInMillis = fadeInMillis;
    this.fadeOutMillis = fadeOutMillis;
  }

  /**
   * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#hideOnTouchOutside()
   */
  @Override
  public void setHideOnTouchOutside(boolean hideOnTouch) {
    this.hideOnTouch = hideOnTouch;
  }

  /**
   * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#doNotBlockTouches()
   */
  @Override
  public void setBlocksTouches(boolean blockTouches) {
    this.blockTouches = blockTouches;
  }

  /**
   * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#blockInsideWindowTouches(boolean)
   */
  @Override
  public void setBlocksInsideWindowTouches(boolean blockInsideWindowTouches) {
    this.blockInsideWindowTouches = blockInsideWindowTouches;
  }

  /**
   * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#setStyle(int)
   */
  @Override
  public void setStyle(int theme) {
    TypedArray array = getContext().obtainStyledAttributes(theme, R.styleable.ShowcaseView);
    updateStyle(array, true);
  }

  @Override
  public boolean isShowing() {
    return isShowing;
  }

  private void updateStyle(TypedArray styled, boolean invalidate) {
    int backgroundColor = styled.getColor(R.styleable.ShowcaseView_sv_backgroundColor, Color.argb(128, 80, 80, 80));
    int showcaseColor = styled.getColor(R.styleable.ShowcaseView_sv_showcaseColor, HOLO_BLUE);
    String buttonText = styled.getString(R.styleable.ShowcaseView_sv_buttonText);
    if (TextUtils.isEmpty(buttonText)) {
      buttonText = getResources().getString(android.R.string.ok);
    }
    boolean tintButton = styled.getBoolean(R.styleable.ShowcaseView_sv_tintButtonColor, true);

    int titleTextAppearance = styled.getResourceId(R.styleable.ShowcaseView_sv_titleTextAppearance,
        R.style.TextAppearance_ShowcaseView_Title);
    int detailTextAppearance = styled.getResourceId(R.styleable.ShowcaseView_sv_detailTextAppearance,
        R.style.TextAppearance_ShowcaseView_Detail);

    styled.recycle();

    showcaseDrawer.setShowcaseColour(showcaseColor);
    showcaseDrawer.setBackgroundColour(backgroundColor);
    tintButton(showcaseColor, tintButton);
    mNextButton.setText(buttonText);
    mTitleTextView.setTextAppearance(getContext(), titleTextAppearance);
    mDetailTextView.setTextAppearance(getContext(), detailTextAppearance);

    if (invalidate) {
      invalidate();
    }
  }

  private void tintButton(int showcaseColor, boolean tintButton) {
    if (tintButton) {
      mNextButton.getBackground().setColorFilter(showcaseColor, PorterDuff.Mode.MULTIPLY);
      mSkipButton.getBackground().setColorFilter(showcaseColor, PorterDuff.Mode.MULTIPLY);
    } else {
      mNextButton.getBackground().setColorFilter(HOLO_BLUE, PorterDuff.Mode.MULTIPLY);
      mSkipButton.getBackground().setColorFilter(HOLO_BLUE, PorterDuff.Mode.MULTIPLY);
    }
  }

  private class UpdateOnGlobalLayout implements ViewTreeObserver.OnGlobalLayoutListener {

    @Override
    public void onGlobalLayout() {
      if (!shotStateStore.hasShot()) {
        updateBitmap();
      }
    }
  }

  private OnClickListener nextOnClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      hide();
    }
  };

  private OnClickListener skipOnClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      skip();
    }
  };

  private OnClickListener backOnClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      back();
    }
  };
}
