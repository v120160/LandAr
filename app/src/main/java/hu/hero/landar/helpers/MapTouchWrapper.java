package hu.hero.landar.helpers;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import static java.lang.Math.sqrt;


public class MapTouchWrapper extends FrameLayout {
    private int      touchSlop = 0;
    private Point    down = null;
    private Callable listener = null;


    public interface Callable {
        void invoke(Point param);
    }

    public MapTouchWrapper(Context context) {
        super(context);
        setup(context);
    }

    public MapTouchWrapper(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setup(context);
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
}
