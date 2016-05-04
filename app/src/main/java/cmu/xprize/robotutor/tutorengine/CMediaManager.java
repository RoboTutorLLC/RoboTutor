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

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import cmu.xprize.robotutor.tutorengine.graph.type_timeline;
import cmu.xprize.robotutor.tutorengine.graph.type_timer;
import cmu.xprize.util.TCONST;
import cmu.xprize.util.TTSsynthesizer;
import edu.cmu.xprize.listener.ListenerBase;


/**
 * This is a Singleton Object
 *
 * This object centralizes access to ongoing processes like audio playback and timelines so
 * that we can pause, play, stop, or destroy them globally.  There is a system wide hard limit on the number
 * of active MediaPlayers in Android. (TODO: need reference)
 *
 * It provides volume control centralization
 *
 * It provides Media caching support
 *
 *
 */
public class CMediaManager   {

    private ArrayList<mediaController>      mControllerSet = new ArrayList<mediaController>();
    private HashMap<String, type_timer>     mTimerMap      = new HashMap<String, type_timer>();
    private HashMap<String, type_timeline>  mTimeLineMap   = new HashMap<String, type_timeline>();

    private AssetManager                    mAssetManager;
    private ListenerBase                    mListener;

    // Note that there is per tutor Language capability
    //
    static private HashMap<CTutor, String>  mLangFtrMap     = new HashMap<CTutor, String>();
    static private TTSsynthesizer           TTS;

    final static public String TAG = "CMediaManager";



    private static CMediaManager ourInstance = new CMediaManager();


    public static CMediaManager getInstance() {
        return ourInstance;
    }


    private CMediaManager() {
    }


    public void setAssetManager(AssetManager manager) {
        mAssetManager = manager;
    }



    //**************************************************************************
    // ASR management START

    /**
     *  Inject the listener into the MediaManageer
     */
    public void setListener(ListenerBase listener) {
        mListener = listener;
    }

    /**
     *  Remove the listener from the MediaManageer
     */
    public void removeListener(ListenerBase listener) {
        mListener = null;
    }

    // ASR management END
    //**************************************************************************



    //**************************************************************************
    // TTS management START

    static public TTSsynthesizer getTTS() {

        return TTS;
    }

    static public void setTTS(TTSsynthesizer _tts) {

        TTS = _tts;
    }

    // TTS management END
    //**************************************************************************



    //**************************************************************************
    // Language management START

    static public String getLanguage(CTutor tTutor) {

        return TCONST.langMap.get(mLangFtrMap.get(tTutor));
    }

    static public String getLanguageFeature(CTutor tTutor) {

        return mLangFtrMap.get(tTutor);
    }

    static public void setLanguageFeature(CTutor tTutor, String langFtr) {

        mLangFtrMap.put(tTutor, langFtr);

        tTutor.updateLanguageFeature(langFtr);
    }

    static public String mapLanguage(String _language) {

        return TCONST.langMap.get(_language);
    }

    // Language management END
    //**************************************************************************



    //********************************************************************
    //*************  Timer Management START

    public void createTimer(String key, type_timer owner) {

        if(mTimerMap.containsKey(key)) {
            Log.e(TAG, "Duplicate Timer Name:" + key);
            System.exit(1);
        }
        mTimerMap.put(key, owner);
    }


    public type_timer removeTimer(String key) {
        return mTimerMap.remove(key);
    }


    public type_timer mapTimer(String key) {
        return mTimerMap.get(key);
    }


    public boolean hasTimer(String key) {
        return mTimerMap.containsKey(key);
    }


    //*************  Timer Management END
    //********************************************************************




    //********************************************************************
    //*************  Timeline Management START

    public void createTimeLine(String key, type_timeline owner) {

        if(mTimeLineMap.containsKey(key)) {
            Log.e(TAG, "Duplicate Timer Name:" + key);
            System.exit(1);
        }
        mTimeLineMap.put(key, owner);
    }


    public type_timeline removeTimeLine(String key) {
        return mTimeLineMap.remove(key);
    }


    public type_timeline mapTimeLine(String key) {
        return mTimeLineMap.get(key);
    }


    public boolean hasTimeLine(String key) {
        return mTimeLineMap.containsKey(key);
    }


    //*************  Timeline Management END
    //********************************************************************




    //********************************************************************
    //*************  MediaPlayer Management START

    public mediaController attachMediaPlayer(String dataSource, IMediaListener owner) {

        mediaController controller = null;

        // First look for an unattached (cached) controller that uses this datasource and reuse it.
        //
        for(mediaController controllerInstance : mControllerSet) {

            if(!controllerInstance.isAttached() && controllerInstance.compareSource(dataSource)) {
                Log.i(TAG, "Attach to existing MediaController");

                controller = controllerInstance;
                controller.attach(owner);
                break;
            }
        }

        // If we don't find a usable controller then try and reuse the player that has been unused
        // for the longest period. i.e. assume the oldest unused audioclip has least probability of
        // being reused.
        //
        if(controller == null) {

            mediaController oldest   = null;
            long            timeTest =  Long.MAX_VALUE;

            for(mediaController controllerInstance : mControllerSet) {

                if(!controllerInstance.isAttached()) {
                    if(controllerInstance.lastUsed() < timeTest) {
                        oldest = controllerInstance;
                    }
                }
            }

            controller = oldest;

            // If we are repurposing an old controller then we need to release it and
            // reset its dataSource.
            //
            if(controller != null) {
                Log.i(TAG, "Re-purpose an existing MediaController");

                controller.releasePlayer();
                controller.createPlayer(dataSource);
            }
        }

        // If we don't find a RE-usable controller then create a new one
        //
        if(controller == null) {
            Log.i(TAG, "Creating new MediaController");

            controller = new mediaController(owner, dataSource);
        }

        return controller;
    }


    /**
     * Clean up the media controller for the given object - it is assumed that an owner may only
     * be attached to a single mediaController
     *
     * @param owner
     */
    public void  detachMediaPlayer(Object owner) {

        mediaController controller = null;

        // First look for an unattached (cached) controller that uses this datasource and reuse it.
        //
        for(mediaController controllerInstance : mControllerSet) {

            if(controllerInstance.compareOwner(owner)) {

                controllerInstance.detach();
                break;
            }
        }
    }


    public class mediaController implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

        private IMediaListener mOwner;

        private MediaPlayer  mPlayer        = null;
        private boolean      mPlaying       = false;
        private boolean      mIsReady       = false;

        private Long         lastUsed       = Long.MAX_VALUE;
        private String       mDataSource = "";

        private boolean      mDeferredStart = false;
        private boolean      mDeferredSeek  = false;
        private long         mSeekPoint     = 0;

        final static public String TAG = "mediaController";


        protected mediaController(Object owner, String _dataSource) {

            mOwner      = (IMediaListener)owner;
            mDataSource = _dataSource;

            createPlayer(mDataSource);
        }


        protected void createPlayer(String dataSource) {

            try {
                mIsReady = false;
                mPlayer  = new MediaPlayer();

                // TODO: permit local file sources - see LoadTrack in type_timeline
                AssetFileDescriptor soundData = mAssetManager.openFd(dataSource);

                mPlayer.setDataSource(soundData.getFileDescriptor(), soundData.getStartOffset(), soundData.getLength());
                soundData.close();
                mPlayer.setOnPreparedListener(this);
                mPlayer.setOnCompletionListener(this);
                mPlayer.prepareAsync();

                Log.d(TAG, "Audio Loading: " + dataSource);

            } catch (Exception e) {
                Log.e(TAG, "Audio error: " + e);
            }
        }

        public void releasePlayer() {

            if(mPlayer != null) {
                mPlayer.pause();
                mPlayer.seekTo(0);
                mPlaying = false;

                mPlayer.reset();
                mPlayer.release();
                mPlayer = null;

                mDataSource = "";
            }
        }


        protected void attach(Object _owner) {
            mOwner = (IMediaListener)_owner;
        }


        /**
         * Note that we leave the MediaPlayer in a playable state
         */
        public void detach() {
            stop();
            mOwner = null;
        }


        protected boolean isAttached() {

            return mOwner != null;
        }


        protected boolean compareSource(String _dataSource) {

            return mDataSource.equals(_dataSource);
        }


        protected Long lastUsed() {

            return lastUsed;
        }


        protected boolean compareOwner(Object _owner) {

            return mOwner == _owner;
        }


        public boolean isPlaying() {
            return mPlaying;
        }


        // TODO : need tighter control over media player lifetime - running out of resources
        // since they aren't being released.
        public void play() {

            if(!mPlaying) {
                if(mIsReady) {
                    mPlayer.start();
                    mPlaying = true;
                }
                else
                    mDeferredStart = true;
            }
        }


        public void stop() {

            pause();
            seek(0L);
        }


        public void pause() {
            if(mPlaying)
               mPlayer.pause();

            mPlaying = false;
        }


        public void seek(long frame) {

            mSeekPoint = frame;

            if(mIsReady) {
                int iframe = (int) (frame * 1000 / TCONST.FPS);

                seekTo(iframe);
            }
            else
                mDeferredSeek = true;

        }


        public void seekTo(int frameTime) {

            // No errors occur - but don't try to seek past the end
            if(mPlayer != null && frameTime < mPlayer.getDuration())
                mPlayer.seekTo(frameTime);
        }


        @Override
        public void onPrepared(MediaPlayer mp) {

            mIsReady = true;

            // If seek was called before we were ready play
            if(mDeferredSeek)
                seek(mSeekPoint);

            // If play was called before we were ready play
            if(mDeferredStart)
                        play();
        }


        /**
         * When play completes we pause the audio stream and seek to origin so it's ready to
         * play if another component want to reuse it.
         *
         * @param mp
         */
        @Override
        public void onCompletion(MediaPlayer mp) {

            // Track when each clip is last used so we can reuse cached clips intelligently
            // by re-purposing the oldest first.  i.e. We want to limit the reloading of clips
            // if possible.
            //
            lastUsed =  System.currentTimeMillis();

            try {
                if(mPlayer != null) {
                    pause();
                    seekTo(0);
                }
            }
            catch(Exception e) {
                Log.e(TAG, "Audio state error:" + e);
            }

            // Allow the owning node to do type specific processing of completion event.
            //
            mOwner.onCompletion();
        }
    }

    //*************  MediaPlayer Management END
    //********************************************************************

}