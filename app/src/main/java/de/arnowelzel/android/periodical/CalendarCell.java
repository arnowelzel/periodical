/**
 * Periodical calendar cell class
 * Copyright (C) 2012-2015 Arno Welzel
 * 
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.arnowelzel.android.periodical;

import android.text.format.DateUtils;
import de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Button;

public class CalendarCell extends Button {
    protected boolean isCurrent;
    protected int type;
    protected int day;
    protected int month;
    protected int year;

    protected DisplayMetrics metrics;
    protected RectF rectCanvas;
    protected Paint paintLabel;
    protected Paint paintBackground;
    protected Paint paintFocus;
    protected Paint paintOval;
    protected RectF rectOval1;
    protected RectF rectOval2;
    protected Rect rectLabel;
    protected LinearGradient gradientPeriodConfirmed;
    protected LinearGradient gradientPeriodPredicted;
    protected LinearGradient gradientFertilityPredicted;
    protected LinearGradient gradientFertilityFuture;
    protected LinearGradient gradientInfertile;
    protected LinearGradient gradientEmpty;
    protected Rect rectDestination;
    protected Bitmap bitmapPeriod;
    protected Bitmap bitmapOvulation;
    protected Bitmap bitmapOvulationPredicted;
    protected Paint paintBitmap;

    protected int orientation;

    public CalendarCell(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        type = DayEntry.EMPTY;
        day = 1;
        month = 1;
        year = 1;
        
        //noinspection ConstantConditions
        metrics = getContext().getResources().getDisplayMetrics();

        // Get current size of the canvas
        rectCanvas = new RectF();
        
        // Create resources needed for drawing
        paintLabel = new Paint();
        paintLabel.setAntiAlias(true);
        paintLabel.setSubpixelText(true);
        paintLabel.setColor(Color.BLACK);
        paintLabel.setTextSize(16 * metrics.scaledDensity);
        paintLabel.setTextAlign(Align.LEFT);
        paintBackground = new Paint();
        paintOval = new Paint();
        paintFocus = new Paint();
        paintFocus.setAntiAlias(true);
        rectOval1 = new RectF();
        rectOval2 = new RectF();
        rectLabel = new Rect();
        gradientPeriodConfirmed = makeCellGradient(0xffff0000, 0xffaa0000);
        gradientPeriodPredicted = makeCellGradient(0xffffc0c0, 0xffc04040);
        gradientFertilityPredicted = makeCellGradient(0xff00c3ff, 0xff007da3);
        gradientFertilityFuture = makeCellGradient(0xff66dbff, 0xff408ba0);
        gradientInfertile = makeCellGradient(0xffffff00, 0xffaaaa00);
        gradientEmpty = makeCellGradient(0xff808080, 0xff808080);

        // Overlays
        rectDestination = new Rect();
        paintBitmap = new Paint();
        paintBitmap.setStyle(Style.FILL);
        paintBitmap.setFilterBitmap(true);
        bitmapPeriod = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_start);
        bitmapOvulation = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_ovulation);
        bitmapOvulationPredicted = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_ovulation_predicted);

        // Get current screen orientation
        if(!isInEditMode()) { // Don't try this in layout editor
            Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).
                    getDefaultDisplay();
            orientation = display.getRotation();
        }
    }
    
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rectCanvas.set(0, 0, w, h);
        gradientPeriodConfirmed = makeCellGradient(0xffff0000, 0xffaa0000);
        gradientPeriodPredicted = makeCellGradient(0xffffc0c0, 0xffc04040);
        gradientFertilityPredicted = makeCellGradient(0xff00c3ff, 0xff007da3);
        gradientFertilityFuture = makeCellGradient(0xff66dbff, 0xff408ba0);
        gradientInfertile = makeCellGradient(0xffffff00, 0xffaaaa00);
    }
    
    protected void onDraw(Canvas canvas) {
        LinearGradient gradient = gradientEmpty;
        int colorLabel = 0xffffffff;

        // Adjust overlay size depending on orientation
        int overlaysize = 18;
        if(orientation == Surface.ROTATION_90 || orientation == Surface.ROTATION_270) {
            overlaysize = 14;
        }

        // Draw background, depending on state
        if (isPressed()) {
            // If cell is pressed, then fill with solid color
            paintFocus.setStyle(Style.FILL);
            paintFocus.setColor(0xffff9e08);
            canvas.drawRoundRect(rectCanvas, 3*metrics.density, 3*metrics.density, paintFocus);
        } else {
            // normal state (or focused), then draw color
            // depending on entry type

            switch (type) {
            case DayEntry.PERIOD_START: // Start of period
            case DayEntry.PERIOD_CONFIRMED: // Confirmed period day
                gradient = gradientPeriodConfirmed;
                colorLabel = 0xffffffff;
                break;
            case DayEntry.PERIOD_PREDICTED: // Predicted period day
                gradient = gradientPeriodPredicted;
                colorLabel = 0xff000000;
                break;
            case DayEntry.FERTILITY_PREDICTED: // Calculated fertile day
            case DayEntry.OVULATION_PREDICTED: // Calculated day of ovulation
                gradient = gradientFertilityPredicted;
                colorLabel = 0xffffffff;
                break;
            case DayEntry.FERTILITY_PREDICTED_FUTURE: // Calculated fertile day in the future
            case DayEntry.OVULATION_PREDICTED_FUTURE: // Calculated day of ovulation in the future
                gradient = gradientFertilityFuture;
                colorLabel = 0xff000000;
                break;
            case DayEntry.INFERTILE:        // Calculated infertile day
            case DayEntry.INFERTILE_FUTURE: // Calculated infertile day in the future
                gradient = gradientInfertile;
                colorLabel = 0xff000000;
                break;
            }

            // Draw background
            paintBackground.setDither(true);
            paintBackground.setShader(gradient);
            paintBackground.setStyle(Style.FILL);
            paintBackground.setAntiAlias(true);
            
            canvas.drawRoundRect(rectCanvas, 3*metrics.density, 3*metrics.density, paintBackground);
            
            // Draw overlay markers depending on type
            rectDestination.set((int) (2 * metrics.density),
                    (int) rectCanvas.height() - (int) (overlaysize * metrics.density),
                    (int) (overlaysize * metrics.density),
                    (int) rectCanvas.height() - (int) (2 * metrics.density));
            if (type == DayEntry.PERIOD_START) {
                canvas.drawBitmap(bitmapPeriod, null, rectDestination, paintBitmap);
            }
            if (type == DayEntry.OVULATION_PREDICTED) {
                canvas.drawBitmap(bitmapOvulation, null, rectDestination, paintBitmap);
            }
            if (type == DayEntry.OVULATION_PREDICTED_FUTURE) {
                canvas.drawBitmap(bitmapOvulationPredicted, null, rectDestination, paintBitmap);
            }

            // Draw the "current day" mark, if needed
            if (isCurrent) {
                paintOval.setStyle(Style.STROKE);
                paintOval.setAntiAlias(true);
                
                rectOval1.set(10 * metrics.density, 4 * metrics.density,
                        rectCanvas.right - 4 * metrics.density, rectCanvas.bottom - 4 * metrics.density);
                rectOval2.set(rectOval1.left-6*metrics.density, rectOval1.top-1,
                        rectOval1.right, rectOval1.bottom);
                
                // Center oval rectangle as a square
                float delta = (rectOval1.height()-rectOval1.width())/2;
                if(delta>0) {
                    rectOval1.top += delta;
                    rectOval1.bottom -= delta;
                    rectOval2.top += delta;
                    rectOval2.bottom -= delta;
                } else if (delta<0) {
                    rectOval1.left -= delta;
                    rectOval1.right += delta;
                    rectOval2.left -= delta;
                    rectOval2.right += delta;
                }
                
                // Draw oval
                paintOval.setColor(0xff000000);
                paintOval.setStrokeWidth(3 * metrics.density);
                canvas.drawArc(rectOval1, 200, 160, false, paintOval);
                canvas.drawArc(rectOval2, 0, 240, false, paintOval);
                
                paintOval.setColor(0xffffffff);
                paintOval.setStrokeWidth(2 * metrics.density);
                canvas.drawArc(rectOval1, 200, 160, false, paintOval);
                canvas.drawArc(rectOval2, 0, 240, false, paintOval);
            }
        }

        // Draw main label
        @SuppressWarnings("ConstantConditions") String label = getText().toString();
        paintLabel.setColor(colorLabel);
        paintLabel.getTextBounds(label, 0, label.length(), rectLabel);

        canvas.drawText(label, (getWidth() - rectLabel.width()) / 2,
                rectLabel.height() + (getHeight() - rectLabel.height()) / 2, paintLabel);

        // Draw focused or pressed state, if the button is focused
        if (isFocused()) {
            paintFocus.setStyle(Style.STROKE);
            paintFocus.setStrokeWidth(4 * metrics.density);
            paintFocus.setColor(0xffff9e08);
            canvas.drawRoundRect(rectCanvas, 3*metrics.density, 3*metrics.density, paintFocus);
        }
    }
    
    // Helper to create a new linear gradient
    protected LinearGradient makeCellGradient(int colorStart, int colorEnd) {
        return new LinearGradient(0, 0,
                rectCanvas.width(), rectCanvas.height(),
                colorStart, colorEnd,
                android.graphics.Shader.TileMode.CLAMP);
    }
    
    // Helper to update content description based on current cell values
    public void updateContentDescription()
    {
        GregorianCalendarExt cal= new GregorianCalendarExt();
        cal.set(getYear(), getMonth()-1, getDay());
        String contentDescription = DateUtils.formatDateTime(getContext(), cal.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR);

        switch(type)
        {
        case DayEntry.PERIOD_START:
            contentDescription += " - "+getResources().getString(R.string.label_period_started);
            break;
        case DayEntry.PERIOD_CONFIRMED:
            contentDescription += " - "+getResources().getString(R.string.label_period);
            break;
        case DayEntry.PERIOD_PREDICTED:
            contentDescription += " - "+getResources().getString(R.string.label_period_predicted);
            break;
        case DayEntry.FERTILITY_PREDICTED:
        case DayEntry.FERTILITY_PREDICTED_FUTURE:
            contentDescription += " - "+getResources().getString(R.string.label_fertile);
            break;
        case DayEntry.OVULATION_PREDICTED:
        case DayEntry.OVULATION_PREDICTED_FUTURE:
            contentDescription += " - "+getResources().getString(R.string.label_ovulation);
            break;
        case DayEntry.INFERTILE:
        case DayEntry.INFERTILE_FUTURE:
            contentDescription += " - "+getResources().getString(R.string.label_infertile);
            break;
        }
        setContentDescription(contentDescription);
    }
    
    // Getter/setter
    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    /*
    public boolean getCurrent() {
        return isCurrent;
    }
    */

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getDay() { return day; }

    public void setMonth(int month) { this.month = month; }

    public int getMonth() {
        return month;
    }

    public void setYear(int year) { this.year = year; }

    public int getYear() { return year; }
}
