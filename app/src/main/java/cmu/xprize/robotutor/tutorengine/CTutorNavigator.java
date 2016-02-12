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


import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import cmu.xprize.robotutor.tutorengine.graph.scene_descriptor;
import cmu.xprize.robotutor.tutorengine.graph.vars.TScope;
import cmu.xprize.robotutor.tutorengine.util.JSON_Helper;


public class CTutorNavigator implements ITutorNavigator{

    private static TScope                     mRootScope;

    private boolean                           traceMode     = false;
    private int                               _sceneCnt     = 0;
    private boolean                           _inNavigation = false;
    private String                            _xType;

    protected ITutorScene                     mParent;
    protected CTutor                          mTutor;
    protected String                          mTutorName;
    protected ITutorLogManager                mLogManager;
    protected CTutorAnimator                  mTutorAnimator;

    // json loadable
    static public scene_descriptor[]          navigatedata;


    // State data
    static private HashMap<String, scene_descriptor> _navMap = new HashMap<String, scene_descriptor>();
    static private int                               _scenePrev;
    static private int                               _sceneCurr;
    static private boolean                           _fSceneGraph = false;


    final private String       TAG       = "CTutorNavigator";



    /**
     */
    public CTutorNavigator(CTutor tutor, String name, TScope tutorScope) {

        mRootScope = new TScope(name + "-SceneNavigator", tutorScope);      // Use a unique namespace

        mTutor     = tutor;
        mTutorName = name;
        _sceneCurr = 0;
        _scenePrev = 0;

        loadNavigatorDescr();

        mTutorAnimator = new CTutorAnimator(CTutor.mTutorName, tutorScope);
    }

    // Initialize the pointer to the tutor root scene
    //
    public void initTutorContainer(ITutorSceneImpl rootScene) {

        navigatedata[0].instance = rootScene;
    }

    @Override
    public CTutorAnimator getAnimator() {
        return mTutorAnimator;
    }


    /**
     *
     * @return The result maps child names to Views
     */
    static public HashMap getChildMap() {

        return navigatedata[_sceneCurr].children;
    }


    /**
     *
     * @param sceneName
     * @return  The result maps child names to Views
     */
    static public HashMap getChildMapByName(String sceneName) {

        return _navMap.get(sceneName).children;
    }


//***************** Navigation Behaviors *******************************

    // Intra Scene Navigation


    /**
     * Used to set the nextButton 
     */
    static public void setButtonBehavior(String action) {
        if(action == TCONST.GOTONEXTSCENE) _fSceneGraph = true;
                                     else  _fSceneGraph = false;
    }


    /**
     * gotoNextScene Event driven entry point
     */
    public void onButtonNext() {

        // debounce the next button - i.e. disallow multiple clicks on same next instance
        // protect against recurrent calls

        if(_inNavigation)
                     return;

        _inNavigation = true;

        // The next button can target either the scenegraph or the animationgraph.
        // i.e. You either want it to trigger the next step in the animationGraph or the sceneGraph
        // reset _fSceneGraph if you want the next button to drive the animationGraph
        //
        if(_fSceneGraph || mTutorAnimator.next().equals(TCONST.NONE)) {
            gotoNextScene();
        }
        else
        {
            _inNavigation = false;
        }
    }


    /**
     */
    private void traceGraphEdge() {
//
//        var nextScene:CGraphScene;
//        var scene:CWOZSceneSequence = _rootGraph.sceneInstance() as CWOZSceneSequence;
//
//        // debounce the next button - i.e. disallow multiple clicks on same next instance
//        // protect against recurrent calls
//
//        if(_inNavigation)
//                     return;
//
//        _inNavigation = true;
//
//        // The next button can target either the scenegraph or the animationgraph.
//        // i.e. You either want it to trigger the next step in the animationGraph or the sceneGraph
//        // reset _fSceneGraph if you want the next button to drive the animationGraph
//        //
//        if(_fSceneGraph || scene == null || scene.nextGraphAnimation(true) == null)
//        {
//            nextScene = _rootGraph.nextScene();
//
//            if(_currScene != nextScene && nextScene != null)
//            {
//                _history.push(_rootGraph.node, nextScene);
//            }
//
//            else if(nextScene == null)
//                enQueueTerminateEvent();
//
//            // Do the scene Transition
//
//            _xType = "WOZNEXT";
//
//            if(_currScene != nextScene && nextScene != null)
//            {
//                seekToScene(nextScene);
//            }
//
//            // We aren't going to be navigating so reset the flag to allow
//            // future attempts.
//
//            else
//            {
//                _inNavigation = false;
//            }
//        }
//        else
//        {
//            _inNavigation = false;
//        }
    }


    //*********************************************
    //*********************************************
    //*********************************************
    // Inter Scene Navigation
    //

    //*************** Navigator getter setters -
    // these within a subclass to set the root of a navigation sequence
    protected int getScenePrev() {
        return _scenePrev;
    }
    protected void setScenePrev(int scenePrevINT) {
        _scenePrev = scenePrevINT;
    }


    protected int  getSceneCurr() {
        return _sceneCurr;
    }
    protected void setSceneCurr(int sceneCurrINT) {
        _sceneCurr = sceneCurrINT;
    }


    protected int  sceneCurrINC() {
        String             features;
        ArrayList<String>  featSet= new ArrayList<String>();
        Boolean            match = false;

        _sceneCurr++;

        // If new scene has features, check that it is being used in the current tutor feature set
        // Note: You must ensure that there is a match for the last scene in the sequence

        while((features = navigatedata[_sceneCurr].features) != null)
        {
            // If this scene is not in the feature set for the tutor then check the next one.

            if(!CTutor.testFeatureSet(features)) _sceneCurr++;
            else break;
        }

        return _sceneCurr;
    }


    protected int sceneCurrDEC() {
        String             features;
        ArrayList<String>  featSet= new ArrayList<String>();
        Boolean            match = false;

        _sceneCurr--;

        // If new scene has features, check that it is being used in the current tutor feature set
        // Note: You must ensure that there is a match for the last scene in the sequence

        while((features = navigatedata[_sceneCurr].features) != null)
        {
            // If this scene is not in the feature set for the tutor then check the next one.

            if(!CTutor.testFeatureSet(features)) _sceneCurr--;
            else break;
        }

        return _sceneCurr;
    }



    private int findSceneOrd(String tarScene) {
        
        if(traceMode) Log.i(TAG, "findSceneOrd: " + tarScene);

        // returns the scene ordinal in the sequence array or 0
        //
        return _navMap.get(tarScene).index;
    }
    


    public void goToScene(String tarScene) {

        if(traceMode) Log.i(TAG, "goToScene: ");

        int    ordScene;
        String newScene = "";
        String redScene = "";

        //@@ Mod Sep 27 2011 - protect against recurrent calls

        if(_inNavigation)
            return;

        _inNavigation = true;
        _xType        = TCONST.WOZGOTO;

        // Find the ordinal for the requested scene Label
        //
        ordScene = findSceneOrd(tarScene);

        // If we don't find the requested scene just skip it
        //
        if(ordScene >= 0)
        {
            if(traceMode) Log.i(TAG, "Nav GoTo Found: " + tarScene);

            // remember current frame

            _scenePrev = _sceneCurr;

            switch(redScene = navigatedata[_sceneCurr].instance.preExitScene("WOZGOTO", _sceneCurr))
            {
                case TCONST.CANCELNAV: 						// Do not allow scene to change
                    _inNavigation = false;

                    return;

                case TCONST.OKNAV: 							// Move to GOTO scene
                    _sceneCurr = ordScene;
                    break;

                default: 								// Goto the scene defined by the current scene
                    _sceneCurr = findSceneOrd(redScene);
            }

            // Do scene Specific initialization - scene returns the Label of the desired target scene
            // This allows the scene to do redirection
            // We allow iterative redirection
            //
            for(redScene = navigatedata[_sceneCurr].id; !redScene.equals(newScene) ; )
            {
                //*** Create scene on demand
                //
                if(navigatedata[_sceneCurr].instance == null)
                {
                    mTutor.instantiateScene(navigatedata[_sceneCurr]);
                }

                newScene = redScene;

                redScene = navigatedata[_sceneCurr].instance.preEnterScene(navigatedata[_sceneCurr], TCONST.WOZGOTO);

                //@@@ NOTE: either discontinue support for redirection through PreEnterScene - or manage scene creation and destruction here

                if(redScene.equals(TCONST.WOZNEXT))
                {
                    sceneCurrINC();
                    redScene = navigatedata[_sceneCurr].id;
                }
                if(redScene.equals(TCONST.WOZBACK))
                {
                    sceneCurrDEC();
                    redScene = navigatedata[_sceneCurr].id;
                }
                // Find the ordinal for the requested scene Label
                //
                else
                    _sceneCurr = findSceneOrd(redScene);
            }

            //@@ Action Logging
//            var logData:Object = {'navevent':'navgoto', 'curscene':scenePrev, 'newscene':redScene};
//            //var xmlVal:XML = <navgoto curscene={scenePrev} newscene={redScene}/>
//
//            gLogR.logNavEvent(logData);
            //@@ Action Logging

            // On exit behaviors

            navigatedata[_scenePrev].instance.onExitScene();

            // Initialize the stategraph for the new scene


            // Do the scene transitions

//            prntTutor.xitions.addEventListener(Event.COMPLETE, doEnterScene);
//            prntTutor.xitions.gotoScene(redScene);
        }
    }


    /**
     * gotoNextScene manual entry point
     */
    public void gotoNextScene() {

        if(traceMode) Log.i(TAG, "gotoNextScene: ");

        String newScene = "";
        String redScene = "";

        //@@ Mod Sep 27 2011 - protect against recurrent calls

        if(_inNavigation)
            return;

        _inNavigation = true;

        // The next button can target either the scenegraph or the animationgraph.
        // i.e. You either want it to trigger the next step in the animationGraph or the sceneGraph
        // reset _fSceneGraph if you want the next button to drive the animationGraph
        //
        if(_fSceneGraph || _sceneCurr == 0 || mTutorAnimator.next().equals(TCONST.NONE)) {

            if (_sceneCurr < _sceneCnt) {

                // remember current frame
                //
                if (traceMode)
                    Log.d(TAG, "scenePrev: " + _scenePrev + "  - sceneCurr: " + _sceneCurr);
                _scenePrev = _sceneCurr;

                // Do scene Specific termination
                //
                if (traceMode)
                    Log.d(TAG, "navigatedata[_sceneCurr]: " + navigatedata[_sceneCurr].id);

                navigatedata[_scenePrev].instance.onExitScene();

                // increment the current scene - this is feature reactive
                sceneCurrINC();

                if (navigatedata[_sceneCurr].instance == null) {
                    mTutor.instantiateScene(navigatedata[_sceneCurr]);
                }

                //@@ Action Logging
                //            var logData:Object = {'navevent':'navnext', 'curscene':_scenePrev, 'newscene':redScene};
                //            //var xmlVal:XML = <navnext curscene={_scenePrev} newscene={redScene}/>
                //
                //            gLogR.logNavEvent(logData);
                //@@ Action Logging

                // On exit behaviors


                // Do the scene transitions
                //            prntTutor.xitions.addEventListener(Event.COMPLETE, doEnterScene);
                //            prntTutor.xitions.gotoScene(redScene);

                doEnterScene();
            }
        }
    }


    // Performed immediately after scene is fully onscreen
    //@@ Mod Jul 18 2013 - public -> private
    //
    private void doEnterScene() {
        if(traceMode) Log.d(TAG, "doEnterScene: " + _sceneCurr);

        // increment the global frame ID - for logging

        CTutor.incFrameNdx();

        //## Mod Sep 12 2013 - This is a special case to handle the first preenter event for an animationGraph.
        //                     The root node of the animation graph is parsed in the preEnter stage of the scene
        //                     creation so the scene is not yet on stage. This call ensures that the scene
        //                     associated with the animation object has been instantiated.
        //
        //	TODO: This should be rationalized with the standard preEnter when all the preEnter customizations
        //        in CWOZScene derivatives have been moved to the XML (JSON) spec.
        //
        navigatedata[_sceneCurr].instance.onEnterScene();

        mTutorAnimator.enterScene(navigatedata[_sceneCurr].id);

        //@@ Mod Sep 27 2011 - protect against recursive calls

        _inNavigation = false;
    }


    //************* Event Handlers

    public void questionStart()
    {
        if(traceMode) Log.d(TAG, "Start of Question: ");

    }


    public void questionComplete()
    {
        if(traceMode) Log.d(TAG, "Question Complete: ");

    }


    public void goBackScene()
    {
        if(traceMode) Log.d(TAG, "Force Decrement Question: ");

//        gotoPrevScene();
    }


    public void goNextScene()
    {
        if(traceMode) Log.d(TAG, "Force Increment Question: ");

        gotoNextScene();

    }


    public void goToNamedScene(String name)
    {
        if(traceMode) Log.d(TAG, "Force Increment Question: ");

        goToScene(name);
    }


    //************ Serialization


    /**
     * Load the Tutor specification from JSON file data
     * from assets/tutors/<tutorname>/navigator_descriptor.json
     *
     * This is only used here until we have the scenegraph implementation in place.
     * This provides a simple linear or mapped access to scenes.
     *
     */
    private void loadNavigatorDescr() {

        try {
            loadJSON(new JSONObject(JSON_Helper.cacheData(TCONST.TUTORROOT + "/" + mTutorName + "/" + TCONST.SNDESC)), mRootScope);
        } catch (JSONException e) {
            Log.d(TAG, "Error" );
        }
    }

    public void loadJSON(JSONObject jsonObj, TScope scope) {
        int i1 = 0;

        JSON_Helper.parseSelf(jsonObj, this, scope);

        // shortcut to length
        _sceneCnt = navigatedata.length;

        // Generate a hash map for all the scenes in the tutor
        for(scene_descriptor scene : navigatedata) {
            scene.index = i1++;
            _navMap.put(scene.id, scene);
        }
    }

}