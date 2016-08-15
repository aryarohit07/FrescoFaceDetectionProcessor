/*
 * Copyright 2016 Rohit Arya
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

package com.rohitarya.fresco.facedetection.processor.core;

import android.content.Context;

import com.google.android.gms.vision.face.FaceDetector;

/**
 * Created by Rohit Arya (http://rohitarya.com) on 2/8/16.
 */
public class FrescoFaceDetector {

    private static volatile FaceDetector faceDetector;
    private static Context mContext;

    public static Context getContext() {
        if(mContext == null) {
            throw new RuntimeException("Initialize FrescoFaceDetector by calling FrescoFaceDetector.initialize(context).");
        }
        return mContext;
    }

    public static void initialize(Context context) {
        mContext = context;
    }

    private static void initDetector() {
        if(faceDetector == null) {
            synchronized ((FrescoFaceDetector.class)) {
                if(faceDetector == null) {
                    faceDetector = new
                            FaceDetector.Builder(getContext())
                            .setTrackingEnabled(false)
                            .build();
                }
            }
        }
    }

    public static FaceDetector getFaceDetector() {
        if(mContext==null) {
            throw new RuntimeException("Initialize FrescoFaceDetector by calling FrescoFaceDetector.initialize(context).");
        }
        initDetector();
        return faceDetector;
    }

    public static void releaseDetector() {
        if(faceDetector!=null) {
            faceDetector.release();
            faceDetector = null;
        }
        mContext = null;
    }
}
