package com.alexilyin.android.a32_imageutilslib;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.ColorInt;

import java.util.Stack;

/**
 * Created by user on 29.03.16.
 */
public class MyPainter {

    Stack<Segment> segmentStack;
    Bitmap tmpBitmap;
    int bitmapHeight;
    int bitmapWidth;


    public Bitmap makeAlphaMask(int init_x, int init_y, Bitmap sourceBitmap, @ColorInt int alphaColor) {

        tmpBitmap = sourceBitmap.copy(sourceBitmap.getConfig(), true);
        bitmapHeight = sourceBitmap.getHeight();
        bitmapWidth = sourceBitmap.getWidth();
        segmentStack = new Stack<>();

        Bitmap alphaBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);


        @ColorInt
        int seekColor = sourceBitmap.getPixel(init_x, init_y);
        @ColorInt int paintColor = Color.argb(1, 1, 1, 1); // Magic!

        // Initial segment
        segmentStack.push(
                new Segment(init_x, init_y)
                        .extendLeftBorder()
                        .extendRightBorder()
        );

        // Loop over stack
        while (!segmentStack.empty()) {

            Segment next = segmentStack.pop();
            next.paint(tmpBitmap, paintColor);
            next.paint(alphaBitmap, alphaColor);

            if ((next.direction == FillDirection.UP || next.direction == FillDirection.BOTH) &&
                    next.y - 1 >= 0)
                checkLine(next.leftX, next.rightX, next.y - 1, seekColor, FillDirection.UP);

            if ((next.direction == FillDirection.DOWN || next.direction == FillDirection.BOTH) &&
                    next.y + 1 < bitmapHeight)
                checkLine(next.leftX, next.rightX, next.y + 1, seekColor, FillDirection.DOWN);
        }

        tmpBitmap = null;
        segmentStack = null;
        return alphaBitmap;
    }

    // Split line to Segments and push to stack
    private void checkLine(int leftX, int rightX, int y, @ColorInt int seekColor, FillDirection direction) {

        Segment tmpSegment = null;

        // checking line
        for (int x = leftX; x <= rightX; x++) {

            // Open segment
            if (tmpSegment == null &&   // no opened segment
                    isEqualColor(x, y, seekColor)) {   // equal color found

                // Create new segment
                tmpSegment = new Segment(x, y);
            }

            // Close segment
            if (tmpSegment != null &&   // inside segment
                    (!isEqualColor(x, y, seekColor) || x == rightX)) {
                // right border found || reached last pixel

                // Close segment and push into stack
                tmpSegment.rightX = x;
                if (x > 0)
                    tmpSegment.rightX--;

                tmpSegment
                        .extendLeftBorder()
                        .extendRightBorder();

                if (tmpSegment.leftX < leftX || tmpSegment.rightX > rightX)
                    tmpSegment.direction = FillDirection.BOTH;
                else tmpSegment.direction = direction;

                segmentStack.push(tmpSegment);
                tmpSegment = null;
            }
        }
    }

    private boolean isEqualColor(int x, int y, @ColorInt int color) {
        return isEqualColor(tmpBitmap.getPixel(x, y), color);
    }

    private boolean isEqualColor(@ColorInt int color1, @ColorInt int color2) {
        return color1 == color2;
    }

// =============================================================================================
// Inner Classes
// =============================================================================================

    private enum FillDirection {UP, DOWN, BOTH}

    // Segment is a horizontal 1px line filled with single color
    private class Segment {

        int leftX;
        int rightX;
        int y;
        FillDirection direction;

        public Segment(int leftX, int rightX, int y, FillDirection direction) {
            this.direction = direction;
            this.y = y;
            this.rightX = rightX;
            this.leftX = leftX;
        }

        public Segment(int x, int y) {
            this(x, x, y, FillDirection.BOTH);
        }

        private Segment extendLeftBorder() {
            int color = tmpBitmap.getPixel(leftX, y);
            while (leftX - 1 >= 0 &&
                    isEqualColor(leftX - 1, y, color)) {
                leftX--;
            }
            return this;
        }

        private Segment extendRightBorder() {
            int color = tmpBitmap.getPixel(rightX, y);
            while (rightX + 1 < bitmapWidth &&
                    isEqualColor(rightX + 1, y, color)) {
                rightX++;
            }
            return this;
        }

        private Segment paint(Bitmap b, @ColorInt int color) {
            for (int x = leftX; x <= rightX; x++)
                b.setPixel(x, y, color);
            return this;
        }

    }

}