package cmu.xprize.rt_component;

import android.view.View;

public interface IVManListener {

    public View getImageView();

    public void publishTargetWord(String word);
    public void publishTargetWordIndex(int index);
    public void publishTargetSentence(String sentence);

}
