package com.chet.mi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** Decorative, non-interactive radar; no bitmap allocations during frames. */
public final class RadarBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent = Color.rgb(219, 43, 68);
    private long started = SystemClock.uptimeMillis();
    public RadarBackgroundView(Context context, AttributeSet attrs) { super(context, attrs); setClickable(false); setFocusable(false);  }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w=getWidth(), h=getHeight(), cx=w*0.72f, cy=h*0.23f;
        final float r=Math.min(w*0.45f,h*0.24f);
        if (r<=0) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx,cy,r*1.8f,new int[]{0x222F0A14,0x0A6A1328,0x00000000},null,Shader.TileMode.CLAMP));
        canvas.drawCircle(cx,cy,r*1.8f,paint); paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Math.max(1f,getResources().getDisplayMetrics().density));
        for(int i=1;i<=4;i++){paint.setColor(0x295D2634);canvas.drawCircle(cx,cy,r*i/4f,paint);}
        paint.setColor(0x185D2634); canvas.drawLine(cx-r,cy,cx+r,cy,paint);canvas.drawLine(cx,cy-r,cx,cy+r,paint);
        final float angle=((SystemClock.uptimeMillis()-started)%10000L)*0.0006283185f;
        for(int i=18;i>=0;i--){float a=angle-i*0.045f;paint.setColor(Color.argb(Math.max(0,65-i*3),219,43,68));paint.setStrokeWidth(2f);canvas.drawLine(cx,cy,cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,paint);}
        paint.setStyle(Paint.Style.FILL);paint.setColor(accent);canvas.drawCircle(cx,cy,3f,paint);
        if(getWindowVisibility()==VISIBLE && isShown()) postInvalidateDelayed(80L);
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();started=SystemClock.uptimeMillis();invalidate();}
}
