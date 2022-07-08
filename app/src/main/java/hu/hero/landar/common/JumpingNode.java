package hu.hero.landar.common;

import android.animation.ObjectAnimator;
import android.view.animation.LinearInterpolator;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;

public class JumpingNode extends Node {
    private float jumpingSpeedMultiplier = 1.0f;  //  跳動速度
    private float jumpingHeight = 10.0f;           //  總跳動高度
    private float jumpingPace = 6.5f;           //   每秒跳動高度

    private ObjectAnimator jumpingAnimation = null;

    public JumpingNode(){}

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);

        if (jumpingAnimation == null){
            return;
        }

        if ( jumpingSpeedMultiplier == 0.0f){
            jumpingAnimation.pause();
        }else {
            jumpingAnimation.resume();
            float animatedFraction = jumpingAnimation.getAnimatedFraction();
            jumpingAnimation.setDuration(getAnimationDuration());
            jumpingAnimation.setCurrentFraction(animatedFraction);
        }
//        lastSpeedMultiplier = speedMultiplier;
    }

    /** Sets Jumping speed */
    public void setJumpingPace(float jumpingPace) {
        this.jumpingPace = jumpingPace;
    }

    @Override
    public void onActivate() {
        startAnimation();
    }

    @Override
    public void onDeactivate() {
        stopAnimation();
    }

    private long getAnimationDuration() {
        return (long) (1000 * jumpingHeight*2 / (jumpingPace * getSpeedMultiplier()));
    }

    private float getSpeedMultiplier() {
        return jumpingSpeedMultiplier;
    }

    private void startAnimation() {
        if (jumpingAnimation != null) {
            return;
        }
        jumpingAnimation = createAnimator();
        jumpingAnimation.setTarget(this);
        jumpingAnimation.setDuration(getAnimationDuration());
        jumpingAnimation.start();
    }

    private void stopAnimation() {
        if (jumpingAnimation == null) {
            return;
        }
        jumpingAnimation.cancel();
        jumpingAnimation = null;
    }

    /** Returns an ObjectAnimator that makes this node rotate. */
    private ObjectAnimator createAnimator() {
        // Node's setLocalPosition method accepts Vector3 as parameters.
        // First, set up orientations that will animate a circle.
        Vector3 v1 = new Vector3(0.0f, 0.0f, 0.0f);
        Vector3 v2 = new Vector3(0.0f, jumpingHeight, 0.0f);
        Vector3 v3 = new Vector3(0.0f, 0.0f, 0.0f);

        ObjectAnimator jumpingAnimation = new ObjectAnimator();
        jumpingAnimation.setObjectValues(v1,v2,v3);

        // Next, give it the localRotation property.
        jumpingAnimation.setPropertyName("localPosition");

        // Use Sceneform's Vector3Evaluator.
        jumpingAnimation.setEvaluator(new Vector3Evaluator());

        //  Allow orbitAnimation to repeat forever
        jumpingAnimation.setRepeatCount(ObjectAnimator.INFINITE);
        jumpingAnimation.setRepeatMode(ObjectAnimator.RESTART);
        jumpingAnimation.setInterpolator(new LinearInterpolator());
        jumpingAnimation.setAutoCancel(true);

        return jumpingAnimation;
    }
}