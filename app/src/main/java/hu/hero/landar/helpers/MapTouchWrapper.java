package hu.hero.landar.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import static java.lang.Math.sqrt;


public class MapTouchWrapper extends RoundCornerLayout {
    private  Activity mActitity;
    private int      mWidth;
    private int      mHeight;
    private int      touchSlop = 0;
    private Point    down = null;
    private Callable listener = null;
    private int mOrientationl = Configuration.ORIENTATION_PORTRAIT;


    public interface Callable {
        void invoke(Point param);
    }

    public MapTouchWrapper(Context context) {
        super(context);
        mActitity = (Activity)context;
        setup(context);
    }

    public MapTouchWrapper(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mActitity = (Activity)context;
//        setup(context);

        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            //取得實際尺寸後，用以設定圓角尺寸
            if( mOrientationl == Configuration.ORIENTATION_PORTRAIT  ){  // 直的
                int length = mActitity.getWindow().getDecorView().getHeight();
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams( LayoutParams.MATCH_PARENT , length/3 );
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                setLayoutParams(params);
                setCornerEnabled(true, true, false, false);
                setRadius( getWidth() / 2);
            }else{        //  橫的
                int length = mActitity.getWindow().getDecorView().getWidth();
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams( length/3  , LayoutParams.MATCH_PARENT );
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                setLayoutParams(params);
                setCornerEnabled(true, false, true, false);
                setRadius( getHeight() / 2);
            }
            Log.d("胡征懷","width="+ mWidth+" , height="+mHeight);
        });
    }

    /*
        設備旋轉時，由 MainActivity 呼叫，改變圓角
    */
    public void setRotate( int rotate ){
        mOrientationl = rotate;
    }

    private void setup(Context context) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
    }

    public void setup( Callable listener ){
        this.listener = listener;
    }

    private double distance( Point p1 , Point p2 ){
        double xDiff = (p1.x - p2.x);
        double yDiff = (p1.y - p2.y);
        return sqrt(xDiff * xDiff + yDiff * yDiff);
    }
/*
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if( listener == null )
            return false;
        int x = (int)ev.getX();
        int y = (int)ev.getY();
        Point tapped = new Point(x,y);
        switch( ev.getAction() ){
            case MotionEvent.ACTION_DOWN:
                down = tapped;
                break;
            case MotionEvent.ACTION_MOVE:
                if( down!=null && distance(down,tapped)>=touchSlop)
                    down = null;
                break;
            case MotionEvent.ACTION_UP:
                if( down!=null && distance(down,tapped)<touchSlop ){
                    listener.invoke(tapped);
                    return true;
                }
        }
        return false;
    }

 */


}
