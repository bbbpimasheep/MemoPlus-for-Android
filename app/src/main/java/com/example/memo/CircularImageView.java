package com.example.memo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CircularImageView extends androidx.appcompat.widget.AppCompatImageView {
    private Paint paint ;

    public CircularImageView(Context context) { super(context); }
    public CircularImageView(Context context, AttributeSet attrs) { super(context, attrs); }
    public CircularImageView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        @SuppressLint("DrawAllocation") Path path = new Path();
        float centerX = (float) getWidth() / 2;
        float centerY = (float) getHeight() / 2;
        float radius = Math.min(centerX, centerY);
        path.addCircle(centerX, centerY, radius, Path.Direction.CW);
        canvas.clipPath(path);
        super.onDraw(canvas);
    }
}
