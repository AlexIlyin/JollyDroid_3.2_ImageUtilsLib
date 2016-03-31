package com.alexilyin.android.a32_imageutilslib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private SurfaceView sv;
    private SurfaceHolder holder;
    private Surface surface;

    private Button btnFillRegion;
    private Button btnPaintRegion;
    private Button btnPaintFree;
    private Switch swPaintErase;

    enum PaintTool {FILL_REGION, PAINT_INSIDE_REGION, PAINT_FREE}

    enum PaintMode {PLAIN_COLOR, ERASE}


    PaintTool paintTool;
    PaintMode paintMode;
    MyPainter painter;

    Random random = new Random();


    public static final String TAG = "happy";

    /*
     * SufraceView можно использовать только после его инициализации.
     * О ней мы узнает из колбэка
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sv = (SurfaceView) findViewById(R.id.sv);
        assert sv != null;
        holder = sv.getHolder();

        holder.addCallback(callback);

        painter = new MyPainter();

        btnFillRegion = (Button) findViewById(R.id.btnFillRegion);
        btnFillRegion.setOnClickListener(toolButtonsListener);

        btnPaintRegion = (Button) findViewById(R.id.btnPaintRegion);
        btnPaintRegion.setOnClickListener(toolButtonsListener);

        btnPaintFree = (Button) findViewById(R.id.btnPaintFree);
        btnPaintFree.setOnClickListener(toolButtonsListener);

        swPaintErase = (Switch) findViewById(R.id.swPaintErase);
        swPaintErase.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    paintMode = PaintMode.ERASE;
//                    testO();
                } else {
                    paintMode = PaintMode.PLAIN_COLOR;
                }
            }
        });

        // Set initial state
        paintTool = PaintTool.PAINT_FREE;
        paintMode = PaintMode.PLAIN_COLOR;
        paintColor = Color.RED;

    }


    View.OnClickListener toolButtonsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnFillRegion:
                    paintTool = PaintTool.FILL_REGION;
                    break;
                case R.id.btnPaintRegion:
                    paintTool = PaintTool.PAINT_INSIDE_REGION;
                    break;
                case R.id.btnPaintFree:
                    paintTool = PaintTool.PAINT_FREE;
                    break;
            }
        }
    };

    /*
     * Колбэк.
     * surfaceChanged() будет вызвано как при первом создании, так и при изменении
     */

    SurfaceHolder.Callback2 callback = new SurfaceHolder.Callback2() {
        @Override
        public void surfaceRedrawNeeded(SurfaceHolder holder) {

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            stopPaint();
            startPaint(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopPaint();
        }
    };

    /*
     * Рисовать мы будем в Bitmap, затем копировать его в SufraceView.
     * Соответственно, Bitmap надо создать.
     *
     * Переноса уже нарисованной картинки при изменении из старого битмапа в новый нет.
     */

    private Bitmap paperBitmap;
    private Canvas paperCanvas;

    private void startPaint(int width, int height) {
        surface = holder.getSurface();
        paperBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        paperCanvas = new Canvas(paperBitmap);

        tmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        tmpCanvas = new Canvas(tmpBitmap);

        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);

        sv.setOnTouchListener(touchListener);
    }

    private void stopPaint() {
        sv.setOnTouchListener(null);

        surface = null;

        if (paperBitmap != null) {
            paperBitmap.recycle();
            paperBitmap = null;
            paperCanvas = null;
        }

    }

    /*
     * Обработка касаний. При касании ставим первую точку линии,
     * дальше продолжаем.
     *
     * Касание в нескольких точках не обрабатывается.
     */
    View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

//            Log.d(TAG, "Touch event - x: " + event.getX() + ", y: " + event.getY());

            switch (paintTool) {

                case FILL_REGION:
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            pickPaintColor();
                            prepareMaskBitmap(event.getX(), event.getY());
                            actionFillRegion(event.getX(), event.getY());
                            break;
                    }
                    break;

                case PAINT_INSIDE_REGION:
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastX = event.getX();
                            lastY = event.getY();
                            pickPaintColor();
                            prepareMaskBitmap(event.getX(), event.getY());
                        case MotionEvent.ACTION_MOVE:
                            actionPaintInsideRegion(event.getX(), event.getY());
                            break;
                        case MotionEvent.ACTION_UP:
                            maskBitmap = null;
                            break;
                    }
                    break;

                case PAINT_FREE:

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastX = event.getX();
                            lastY = event.getY();
                            pickPaintColor();
                        case MotionEvent.ACTION_MOVE:
                            actionPaintFree(event.getX(), event.getY());
                            break;
                    }
                    break;
            }
            return true;
        }

    };

    /*
     * Инициализация элемента, из которого будет состоять линия
     */

    private int paintColor = 0xffff0000;
    private Paint drawPaint = new Paint();

    {
        drawPaint.setColor(paintColor);
//        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /*
     * рисуем точку и линию
     */

    float lastX, lastY;
    private Bitmap tmpBitmap;
    private Canvas tmpCanvas;
    private Bitmap maskBitmap;
    private Paint maskPaint;
    private PorterDuffXfermode porterDuffXfermode;


    private void pickPaintColor() {
//        paintColor = Color.LTGRAY;

        paintColor = Color.rgb(
                random.nextInt(255),
                random.nextInt(255),
                random.nextInt(255)
        );

        drawPaint.setColor(paintColor);
    }

    private void prepareMaskBitmap(float x, float y) {
        maskBitmap = painter.makeAlphaMask(
                Math.round(x),
                Math.round(y),
                paperBitmap,
                Color.WHITE);
    }


    private void actionFillRegion(float x, float y) {
        maskPaint.setXfermode(porterDuffXfermode);

        tmpCanvas.drawColor(paintColor);
        tmpCanvas.drawBitmap(maskBitmap, 0, 0, maskPaint);

        maskPaint.setXfermode(null);

        paperCanvas.drawBitmap(tmpBitmap, 0, 0, new Paint());
        drawPaperBitmap(x, y);

        maskBitmap = null;
    }


    private void actionPaintInsideRegion(float x, float y) {
        maskPaint.setXfermode(porterDuffXfermode);

        if (x == lastX && y == lastY)
            tmpCanvas.drawPoint(x, y, drawPaint);
        else
            tmpCanvas.drawLine(lastX, lastY, x, y, drawPaint);

        tmpCanvas.drawBitmap(maskBitmap, 0, 0, maskPaint);

        maskPaint.setXfermode(null);

        paperCanvas.drawBitmap(tmpBitmap, 0, 0, new Paint());
        drawPaperBitmap(x, y);
    }


    private void actionPaintFree(float x, float y) {

        if (x == lastX && y == lastY)
            paperCanvas.drawPoint(x, y, drawPaint);
        else
            paperCanvas.drawLine(lastX, lastY, x, y, drawPaint);

        drawPaperBitmap(x, y);
    }


    /*
     * Копирование битмапа с картинкой в Surface.
     * Прямо в Bitmap от Surface рисовать нельзя - он может прийти, не соответствующий
     * текущему отображаемому состоянию.
     *
     * http://stackoverflow.com/a/36267113/1263771
     */
    private void drawPaperBitmap(float x, float y) {
        Canvas canvas = surface.lockCanvas(null);
        canvas.drawBitmap(paperBitmap, 0, 0, null);
        surface.unlockCanvasAndPost(canvas);

        lastX = x;
        lastY = y;
    }


    private void testO() {

        int scale = 100;
        int repeatCount = 10;
        int dotsCount = 30;

        // Clean square fill
        //
        // |-------|
        // |       |
        // |       |
        // |-------|
        Log.d(TAG, "Clean square");
        Log.d(TAG, "width | height | time");
        for (int j = 0; j < repeatCount; j++)
            for (int i = 1; i <= dotsCount; i++) {

                int width = i * scale;
                int height = i * scale;

                Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                long t0 = SystemClock.currentThreadTimeMillis();
                new MyPainter().makeAlphaMask(width / 2, height / 2, b, Color.WHITE);
                long t1 = SystemClock.currentThreadTimeMillis();

                Log.d(TAG, width + "\t" + height + "\t" + (t1 - t0));

            }

        // +two circles
        //
        // |-------|
        // | o     |
        // |     o |
        // |-------|
        Log.d(TAG, "Two circles");
        Log.d(TAG, "width | height | time");
        for (int j = 0; j < repeatCount; j++)
            for (int i = 1; i <= dotsCount; i++) {

                int width = i * scale;
                int height = i * scale;

                Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                c.drawCircle(width / 4, height / 4, width / 8, drawPaint);
                c.drawCircle(width * 3 / 4, height * 3 / 4, width / 8, drawPaint);

                long t0 = SystemClock.currentThreadTimeMillis();
                new MyPainter().makeAlphaMask(width / 2, height / 2, b, Color.WHITE);
                long t1 = SystemClock.currentThreadTimeMillis();

                Log.d(TAG, width + "\t" + height + "\t" + (t1 - t0));
            }

        // Grid of uniform circles
        //
        // |-------|
        // | o o o |
        // | o o o |
        // |-------|
        Log.d(TAG, "Grid of circles");
        Log.d(TAG, "width | height | time");
        for (int j = 0; j < repeatCount; j++)
            for (int i = 2; i <= dotsCount; i += 2) {

                int width = i * scale;
                int height = i * scale;

                Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                for (int yi = 0; yi < i; yi++)
                    for (int xi = 0; xi < i; xi++)
                        c.drawCircle(xi * scale + scale / 2, yi * scale + scale / 2, scale / 4, drawPaint);

                long t0 = SystemClock.currentThreadTimeMillis();
                new MyPainter().makeAlphaMask(width / 2, height / 2, b, Color.WHITE);
                long t1 = SystemClock.currentThreadTimeMillis();

                Log.d(TAG, width + "\t" + height + "\t" + (t1 - t0));

            }
    }
}
