/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.westalgo.factorycamera.ui;

import android.view.GestureDetector;

/**
 * This interface defines a listener that watches preview status, including SurfaceTexture
 * change and preview gestures.
 */
public interface PreviewStatusListener {
    /**
     * The preview status listener needs to provide an
     * {@link android.view.GestureDetector.OnGestureListener} in order to listen
     * to the touch events that happen on preview.
     *
     * @return a listener that listens to touch events
     */
    public GestureDetector.OnGestureListener getGestureListener();

    /**
     * An {@link android.view.View.OnTouchListener} can be provided in addition to
     * or instead of a {@link android.view.GestureDetector.OnGestureListener}
     * for listening to touch events on the preview.  The listener is called whenever
     * there is a touch event on the {@link com.westalgo.factorycamera.ui.PreviewOverlay}.
     */
//    public View.OnTouchListener getTouchListener();

}
