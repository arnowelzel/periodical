/*
 * Periodical calendar cell class
 * Copyright (C) 2012-2025 Arno Welzel
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

import android.annotation.SuppressLint;
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
import android.util.TypedValue;
import android.widget.Button;

import androidx.core.content.ContextCompat;

/**
 * Custom button class to display the calendar cells
 */
@SuppressLint("AppCompatCustomView")
public class CalendarCell extends Button {
    /**
     * flag for "is current day"
     */
    private boolean isCurrent;
    /**
     * entry type as in database
     */
    private int type;
    /**
     * displayed day of month (1-31)
     */
    private int day;
    /**
     * month (1-12)
     */
    private int month;
    /**
     * year including century
     */
    private int year;
    /**
     * day of cycle (1-n, 0 to hide)
     */
    private int dayofcycle;
    /**
     * intensity during period (1-4)
     */
    private int intensity;
    /**
     * flag for intercourse
     */
    private boolean intercourse;
    /**
     * flag for notes
     */
    private boolean notes;

    /**
     * Display metrics
     */
    private final DisplayMetrics metrics;
    /**
     * Rectangle of the cell canvas
     */
    private final RectF rectCanvas;
    /**
     * Paint for the label (day of month)
     */
    private final Paint paintLabel;
    /**
     * Paint for the intensity markers
     */
    private final Paint paintIntensity;
    /**
     * Background paint for the cell
     */
    private final Paint paintBackground;
    /**
     * Paint for the cell if it focused
     */
    private final Paint paintFocus;
    /**
     * Paint for the "is current day" oval marker
     */
    private final Paint paintOval;
    /**
     * First rectangle for the "is current day" oval marker
     */
    private final RectF rectOval1;
    /**
     * Second rectangle for the "is current day" oval marker
     */
    private final RectF rectOval2;
    /**
     * Rectangle for the label (day of month)
     */
    private final Rect rectLabel;
    /**
     * Gradient for entries of type "confirmed period"
     */
    private LinearGradient gradientPeriodConfirmed;
    /**
     * Gradient for entries of type "predicted period"
     */
    private LinearGradient gradientPeriodPredicted;
    /**
     * Gradient for entries of type "predicted fertility" and "ovulation"
     */
    private LinearGradient gradientFertilityPredicted;
    /**
     * Gradient for entries of type "predicted fertility standard"
     */
    private final LinearGradient gradientFertilityStandardPredicted;
    /**
     * Gradient for entries of type "predicted fertility in the future" and "ovulation in the future
     */
    private LinearGradient gradientFertilityFuture;
    /**
     * Gradient for entries of type "predicted fertility standard in the future"
     */
    private final LinearGradient gradientFertilityStandardFuture;
    /**
     * Gradient for entries of type "infertile day predicted"
     */
    private LinearGradient gradientInfertilePredicted;
    /**
     * Gradient for entries of type "infertile day predicted in the future"
     */
    private LinearGradient gradientInfertileFuture;
    /**
     * Gradient for empty entries
     */
    private final LinearGradient gradientEmpty;
    /**
     * Rectangle for overlays
     */
    private final Rect rectOverlay;
    /**
     * Bitmap for entries of type "period"  and "predicted period"
     */
    private final Bitmap bitmapPeriod;
    /**
     * Bitmap for entries of type "ovulation"
     */
    private final Bitmap bitmapOvulation;
    /**
     * Bitmap for entries of type "ovulation in the future"
     */
    private final Bitmap bitmapOvulationFuture;
    /**
     * Bitmap for entries with flag "intercourse"
     */
    private final Bitmap bitmapIntercourse;
    /**
     * Bitmap for entries with flag "intercourse" (black variant)
     */
    private final Bitmap bitmapIntercourseBlack;
    /**
     * Bitmap for entries with flag "notes"
     */
    private final Bitmap bitmapNotes;
    /**
     * Bitmap for entries with flag "notes" (black variant)
     */
    private final Bitmap bitmapNotesBlack;
    /**
     * Paint for bitmaps
     */
    private final Paint paintBitmap;

    /**
     * Colors
     */
    int colorPeriodConfirmed = ContextCompat.getColor(this.getContext(), R.color.colorPeriodConfirmed);
    int colorPeriodPredicted = ContextCompat.getColor(this.getContext(), R.color.colorPeriodPredicted);
    int colorFertilityPredicted = ContextCompat.getColor(this.getContext(), R.color.colorFertilityPredicted);
    int colorFertilityStandardPredicted = ContextCompat.getColor(this.getContext(), R.color.colorFertilityStandardPredicted);
    int colorFertilityFuture = ContextCompat.getColor(this.getContext(), R.color.colorFertilityFuture);
    int colorFertilityStandardFuture = ContextCompat.getColor(this.getContext(), R.color.colorFertilityStandardFuture);
    int colorInfertilePredicted = ContextCompat.getColor(this.getContext(), R.color.colorInfertilePredicted);
    int colorInfertileFuture = ContextCompat.getColor(this.getContext(), R.color.colorInfertileFuture);
    int colorEmpty = ContextCompat.getColor(this.getContext(), R.color.colorEmpty);
    int colorBlack = ContextCompat.getColor(this.getContext(), R.color.colorBlack);
    int colorWhite = ContextCompat.getColor(this.getContext(), R.color.colorWhite);
    int colorFocus = ContextCompat.getColor(this.getContext(), R.color.colorFocus);
    int colorOpacity = ContextCompat.getColor(this.getContext(), R.color.colorOpacity);

    /**
     * Constructor
     *
     * @param context Application context
     * @param attrs   Resource attributes
     */
    public CalendarCell(Context context, AttributeSet attrs) {
        super(context, attrs);

        type = DayEntry.EMPTY;
        day = 1;
        month = 1;
        year = 1;
        intercourse = false;

        //noinspection ConstantConditions
        metrics = getContext().getResources().getDisplayMetrics();

        // Get current size of the canvas
        rectCanvas = new RectF();

        // Create resources needed for drawing
        paintLabel = new Paint();
        paintLabel.setAntiAlias(true);
        paintLabel.setSubpixelText(true);
        paintLabel.setColor(Color.BLACK);
        paintLabel.setTextAlign(Align.LEFT);
        paintIntensity = new Paint();
        paintIntensity.setStyle(Style.FILL);
        paintIntensity.setColor(colorWhite);
        paintBackground = new Paint();
        paintOval = new Paint();
        paintFocus = new Paint();
        paintFocus.setAntiAlias(true);
        rectOval1 = new RectF();
        rectOval2 = new RectF();
        rectLabel = new Rect();
        gradientPeriodConfirmed = makeCellGradient(colorPeriodConfirmed, colorPeriodConfirmed);
        gradientPeriodPredicted = makeCellGradient(colorPeriodPredicted, colorPeriodPredicted);
        gradientFertilityPredicted = makeCellGradient(colorFertilityPredicted, colorFertilityPredicted);
        gradientFertilityStandardPredicted = makeCellGradient(colorFertilityStandardPredicted, colorFertilityStandardPredicted);
        gradientFertilityFuture = makeCellGradient(colorFertilityFuture, colorFertilityFuture);
        gradientFertilityStandardFuture = makeCellGradient(colorFertilityStandardFuture, colorFertilityStandardFuture);
        gradientInfertilePredicted = makeCellGradient(colorInfertilePredicted, colorInfertilePredicted);
        gradientInfertileFuture = makeCellGradient(colorInfertileFuture, colorInfertileFuture);
        gradientEmpty = makeCellGradient(colorEmpty, colorEmpty);

        // Overlays
        rectOverlay = new Rect();
        paintBitmap = new Paint();
        paintBitmap.setStyle(Style.FILL);
        paintBitmap.setFilterBitmap(true);
        bitmapPeriod = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_start);
        bitmapOvulation = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_ovulation);
        bitmapOvulationFuture = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_ovulation_predicted);
        bitmapIntercourse = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_intercourse);
        bitmapIntercourseBlack = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_intercourse_black);
        bitmapNotes = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_notes);
        bitmapNotesBlack = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_notes_black);
    }

    /**
     * Handle size changes to adapt size specific elements
     *
     * @param w    Current width
     * @param h    Current height
     * @param oldw Old width
     * @param oldh Old height
     */
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rectCanvas.set(0, 0, w, h);
        gradientPeriodConfirmed = makeCellGradient(colorPeriodConfirmed, colorPeriodConfirmed);
        gradientPeriodPredicted = makeCellGradient(colorPeriodPredicted, colorPeriodPredicted);
        gradientFertilityPredicted = makeCellGradient(colorFertilityPredicted, colorFertilityPredicted);
        gradientFertilityFuture = makeCellGradient(colorFertilityFuture, colorFertilityFuture);
        gradientInfertilePredicted = makeCellGradient(colorInfertilePredicted, colorInfertilePredicted);
        gradientInfertileFuture = makeCellGradient(colorInfertileFuture, colorInfertileFuture);
    }

    /**
     * Custom draw
     *
     * @param canvas The canvas to draw on
     */
    @SuppressLint("DefaultLocale")
    protected void onDraw(Canvas canvas) {
        LinearGradient gradient = gradientEmpty;
        int colorLabel = colorWhite;
        String label;

        // Adjust overlay size depending on orientation
        int overlaysize = 18;
        if (rectCanvas.width() > rectCanvas.height()) {
            overlaysize = 14;
        }

        // Draw background, depending on state
        if (isPressed()) {
            // If cell is pressed, then fill with solid color
            paintFocus.setStyle(Style.FILL);
            paintFocus.setColor(colorFocus);
            canvas.drawRoundRect(rectCanvas, 3 * metrics.density, 3 * metrics.density, paintFocus);
            colorLabel = colorOpacity;
        } else {
            // normal state (or focused), then draw color
            // depending on entry type

            switch (type) {
                case DayEntry.PERIOD_START: // Start of period
                case DayEntry.PERIOD_CONFIRMED: // Confirmed period day
                    gradient = gradientPeriodConfirmed;
                    colorLabel = colorWhite;
                    break;
                case DayEntry.PERIOD_PREDICTED: // Predicted period day
                    gradient = gradientPeriodPredicted;
                    colorLabel = colorOpacity;
                    break;
                case DayEntry.FERTILITY_PREDICTED: // Calculated fertile day
                case DayEntry.OVULATION_PREDICTED: // Calculated day of ovulation
                    gradient = gradientFertilityPredicted;
                    colorLabel = colorWhite;
                    break;
                case DayEntry.FERTILITY_FUTURE: // Calculated fertile day in the future
                case DayEntry.OVULATION_FUTURE: // Calculated day of ovulation in the future
                    gradient = gradientFertilityFuture;
                    colorLabel = colorOpacity;
                    break;
                case DayEntry.FERTILITY_STANDARD_PREDICTED: // Calculated fertile standard day
                    gradient = gradientFertilityStandardPredicted;
                    colorLabel = colorWhite;
                    break;
                case DayEntry.FERTILITY_STANDARD_FUTURE: // Calculated fertile day in the future
                    gradient = gradientFertilityStandardFuture;
                    colorLabel = colorOpacity;
                    break;
                case DayEntry.INFERTILE_PREDICTED:        // Calculated infertile day
                    gradient = gradientInfertilePredicted;
                    colorLabel = colorOpacity;
                    break;
                case DayEntry.INFERTILE_FUTURE: // Calculated infertile day in the future
                    gradient = gradientInfertileFuture;
                    colorLabel = colorOpacity;
                    break;
            }

            // Draw background
            paintBackground.setDither(true);
            paintBackground.setShader(gradient);
            paintBackground.setStyle(Style.FILL);
            paintBackground.setAntiAlias(true);

            canvas.drawRoundRect(rectCanvas, 3 * metrics.density, 3 * metrics.density, paintBackground);

            // Draw period start indicator
            rectOverlay.set((int) (4 * metrics.density),
                    (int) rectCanvas.height() - (int) ((2 + overlaysize) * metrics.density),
                    (int) ((overlaysize + 2) * metrics.density),
                    (int) rectCanvas.height() - (int) (4 * metrics.density));
            if (type == DayEntry.PERIOD_START) {
                canvas.drawBitmap(bitmapPeriod, null, rectOverlay, paintBitmap);
            }

            // Draw ovulation indicator
            if (type == DayEntry.OVULATION_PREDICTED) {
                canvas.drawBitmap(bitmapOvulation, null, rectOverlay, paintBitmap);
            }
            if (type == DayEntry.OVULATION_FUTURE) {
                canvas.drawBitmap(bitmapOvulationFuture, null, rectOverlay, paintBitmap);
            }

            // Draw intensity indicator
            if (type == DayEntry.PERIOD_START || type == DayEntry.PERIOD_CONFIRMED) {
                for (int i = 0; i < intensity && i < 4; i++) {
                    canvas.drawCircle((6 + i * 6) * metrics.density, 6 * metrics.density,
                            2 * metrics.density, paintIntensity);
                }
            }

            // Draw intercourse indicator
            rectOverlay.set((int) rectCanvas.width() - (int) (overlaysize * metrics.density),
                    (int) (4 * metrics.density),
                    (int) rectCanvas.width() - (int) (4 * metrics.density),
                    (int) (overlaysize * metrics.density));
            if (intercourse) {
                if (colorLabel == colorWhite) {
                    canvas.drawBitmap(bitmapIntercourse, null, rectOverlay, paintBitmap);
                } else {
                    canvas.drawBitmap(bitmapIntercourseBlack, null, rectOverlay, paintBitmap);
                }
            }

            // Draw notes indicator
            rectOverlay.set((int) (rectCanvas.width() / 2 - (overlaysize * metrics.density) / 2),
                    (int) rectCanvas.height() - (int) ((2 + overlaysize) * metrics.density),
                    (int) (rectCanvas.width() / 2 + (overlaysize * metrics.density) / 2),
                    (int) rectCanvas.height() - (int) (4 * metrics.density));
            if (notes) {
                if (colorLabel == colorWhite) {
                    canvas.drawBitmap(bitmapNotes, null, rectOverlay, paintBitmap);
                } else {
                    canvas.drawBitmap(bitmapNotesBlack, null, rectOverlay, paintBitmap);
                }
            }
        }

        // Draw main label
        label = getText().toString();
        paintLabel.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, metrics));
        paintLabel.setColor(colorLabel);
        paintLabel.getTextBounds(label, 0, label.length(), rectLabel);

        canvas.drawText(label, (float) (getWidth() - rectLabel.width()) / 2,
                rectLabel.height() + (float) (getHeight() - rectLabel.height()) / 2, paintLabel);

        // Draw day of cycle, if applicable
        if (!isPressed() && dayofcycle != 0) {
            label = String.format("%d", dayofcycle);
            paintLabel.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, metrics));
            paintLabel.setColor(colorLabel);
            paintLabel.getTextBounds(label, 0, label.length(), rectLabel);

            canvas.drawText(label,
                    rectCanvas.width() - (float) rectLabel.width() - 4 * metrics.density,
                    rectCanvas.height() - (float) rectLabel.height() / 2 - 1 * metrics.density,
                    paintLabel);
        }

        if (!isPressed()) {
            // Draw the "current day" mark, if needed
            if (isCurrent) {
                paintOval.setStyle(Style.STROKE);
                paintOval.setAntiAlias(true);

                rectOval1.set(10 * metrics.density, 4 * metrics.density,
                        rectCanvas.right - 4 * metrics.density, rectCanvas.bottom - 4 * metrics.density);
                rectOval2.set(rectOval1.left - 6 * metrics.density, rectOval1.top - 1,
                        rectOval1.right, rectOval1.bottom);

                // Center oval rectangle as a square
                float delta = (rectOval1.height() - rectOval1.width()) / 2;
                if (delta > 0) {
                    rectOval1.top += delta;
                    rectOval1.bottom -= delta;
                    rectOval2.top += delta;
                    rectOval2.bottom -= delta;
                } else if (delta < 0) {
                    rectOval1.left -= delta;
                    rectOval1.right += delta;
                    rectOval2.left -= delta;
                    rectOval2.right += delta;
                }

                // Draw oval
                paintOval.setColor(colorOpacity);
                paintOval.setStrokeWidth(3 * metrics.density);
                canvas.drawArc(rectOval1, 200, 160, false, paintOval);
                canvas.drawArc(rectOval2, 0, 240, false, paintOval);

                paintOval.setColor(colorWhite);
                paintOval.setStrokeWidth(2 * metrics.density);
                canvas.drawArc(rectOval1, 200, 160, false, paintOval);
                canvas.drawArc(rectOval2, 0, 240, false, paintOval);
            }
        }

        // Draw focused or pressed state, if the button is focused
        if (isFocused()) {
            paintFocus.setStyle(Style.STROKE);
            paintFocus.setStrokeWidth(4 * metrics.density);
            paintFocus.setColor(colorFocus);
            canvas.drawRoundRect(rectCanvas, 3 * metrics.density, 3 * metrics.density, paintFocus);
        }
    }

    /**
     * Helper to create a linear gradient
     *
     * @param colorStart Color to start with
     * @param colorEnd   Color to end with
     * @return A LinearGradient with the given colors at a 45 degree angle
     */
    private LinearGradient makeCellGradient(int colorStart, int colorEnd) {
        return new LinearGradient(0, 0,
                rectCanvas.width(), rectCanvas.height(),
                colorStart, colorEnd,
                android.graphics.Shader.TileMode.CLAMP);
    }

    /**
     * Helper to update content description on all calendar cells
     */
    public void updateContentDescription() {
        GregorianCalendarExt cal = new GregorianCalendarExt();
        cal.set(getYear(), getMonth() - 1, getDay());
        String contentDescription = DateUtils.formatDateTime(getContext(), cal.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);

        switch (type) {
            case DayEntry.PERIOD_START:
                contentDescription += " - " + getResources().getString(R.string.label_period_started);
                break;
            case DayEntry.PERIOD_CONFIRMED:
                contentDescription += " - " + getResources().getString(R.string.label_period);
                break;
            case DayEntry.PERIOD_PREDICTED:
                contentDescription += " - " + getResources().getString(R.string.label_period_predicted);
                break;
            case DayEntry.FERTILITY_PREDICTED:
            case DayEntry.FERTILITY_FUTURE:
                contentDescription += " - " + getResources().getString(R.string.label_fertile);
                break;
            case DayEntry.OVULATION_PREDICTED:
            case DayEntry.OVULATION_FUTURE:
                contentDescription += " - " + getResources().getString(R.string.label_ovulation);
                break;
            case DayEntry.INFERTILE_PREDICTED:
            case DayEntry.INFERTILE_FUTURE:
                contentDescription += " - " + getResources().getString(R.string.label_infertile);
                break;
            case DayEntry.FERTILITY_STANDARD_PREDICTED:
            case DayEntry.FERTILITY_STANDARD_FUTURE:
                contentDescription += " - " + getResources().getString(R.string.label_fertile_standard);
                break;
        }
        setContentDescription(contentDescription);
    }

    /**
     * Set "is current day" flag
     *
     * @param current true if this is the current day, false otherwise
     */
    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    /**
     * Set current cell type
     *
     * @param type The type as stored in the database to define the look of the cell
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Set day of cycle
     *
     * @param dayofcycle The type as stored in the database to define the look of the cell
     */
    public void setDayofcycle(int dayofcycle) {
        this.dayofcycle = dayofcycle;
    }

    /**
     * Set intensity
     *
     * @param intensity Intensity of this day (1-4)
     */
    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    /**
     * Set "intercourse" flag
     *
     * @param intercourse true if intercourse, false otherwise
     */
    public void setIntercourse(boolean intercourse) {
        this.intercourse = intercourse;
    }

    /**
     * Set "notes" flag
     *
     * @param notes true if notes exist, false otherwise
     */

    public void setNotes(boolean notes) {
        this.notes = notes;
    }

    /**
     * Set the day to be displayed
     *
     * @param day The day of the month (1-31)
     */
    public void setDay(int day) {
        this.day = day;
    }

    /**
     * Get the displayed day
     *
     * @return The day of the month (1-31)
     */
    private int getDay() {
        return day;
    }

    /**
     * Set the month
     *
     * @param month The month (1-12)
     */
    public void setMonth(int month) {
        this.month = month;
    }

    /**
     * Get the month
     *
     * @return The month (1-12)
     */
    private int getMonth() {
        return month;
    }

    /**
     * Set the year
     *
     * @param year The year
     */
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * Get the year
     *
     * @return The year
     */
    private int getYear() {
        return year;
    }
}
