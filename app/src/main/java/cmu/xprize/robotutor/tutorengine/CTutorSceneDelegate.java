//*********************************************************************************
//
//    Copyright(c) 2016 Carnegie Mellon University. All Rights Reserved.
//    Copyright(c) Kevin Willows All Rights Reserved
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************

package cmu.xprize.robotutor.tutorengine;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;

import java.util.List;

import cmu.xprize.robotutor.R;
import cmu.xprize.robotutor.tutorengine.graph.scene_descriptor;

/**
 * All ITutorScene's use an instance of this to drive scene functionality
 *
 */
public class CTutorSceneDelegate implements ITutorScene {

    private ViewGroup           mOwnerViewGroup;

    private String              mTutorId;
    private String              mInstanceId;
    private Context             mContext;

    protected ITutorScene       mParent;
    protected CTutor            mTutor;
    protected CTutorAnimator    mAnimator;
    protected ITutorNavigator   mNavigator;
    protected ITutorLogManager  mLogManager;


//    public var audioStartTimer:CWOZTimerProxy;
//
//    public static const DEFAULT_MONITOR_INTERVAL:Number = 3000;
//
//    protected var _timer:Timer;
//    protected var _interval:Number = DEFAULT_MONITOR_INTERVAL;


    //## Mod aug 22 2013 - KT updates are single shot per scene

    protected boolean ktUpdated = false;

    // We support 3 types of scene drivers
    // ActionTracks    - simple instances of CActionTrack object with audio/events
    // ActionSequences - sequences of CActionTracks
    // AnimationGraphs = full CTutorAnimator support for complex sequences

    private String	seqID;
    private List    seqTrack;
    private int     seqIndex;

    private CTutorAnimator animationGraph;


    // Attach the View to this functionality
    public CTutorSceneDelegate(ViewGroup owner) {
        mOwnerViewGroup = owner;
    }

    public ViewGroup getOwner() {return mOwnerViewGroup;}


    //*** ITutorObject implementation

    @Override
    public void init(Context context, AttributeSet attrs) {

        mContext = context;

        // Load attributes
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.tutor);

        mTutorId = a.getString(
                R.styleable.tutor_tutorId);

        a.recycle();

    }

    @Override
    public void setName(String name) { mTutorId = name; }

    @Override
    public String name() { return mTutorId; }

    @Override
    public void setParent(ITutorSceneImpl parent) { mParent = parent; }

    @Override
    public void setTutor(CTutor tutor) { mTutor = tutor; }

    @Override
    public void setNavigator(ITutorNavigator navigator) { mNavigator = navigator; }

    @Override
    public void setLogManager(ITutorLogManager logManager) { mLogManager = logManager; }


    //*** ITutorScene Implementation

    @Override
    public String preEnterScene(scene_descriptor scene, String Direction) {
        // By default return the same direction requested
        return Direction;
    }

    @Override
    public void onEnterScene() {

        // Create a unique timestamp for this scene

//            gTutor.timeStamp.createLogAttr("dur_"+name, true);
    }

    @Override
    public String preExitScene(String Direction, int sceneCurr) {

        return TCONST.OKNAV;
    }

    @Override
    public void onExitScene() {

    }

}