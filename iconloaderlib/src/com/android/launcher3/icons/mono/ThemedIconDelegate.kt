/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.icons.mono

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Rect
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.icons.FastBitmapDrawable
import com.android.launcher3.icons.FastBitmapDrawableDelegate
import com.android.launcher3.icons.FastBitmapDrawableDelegate.DelegateFactory
import com.android.launcher3.icons.GraphicsUtils.getColorMultipliedFilter
import com.android.launcher3.icons.GraphicsUtils.resizeToContentSize
import com.android.launcher3.icons.IconShape
import com.android.launcher3.icons.R

/** Drawing delegate handle monochrome themed app icons */
class ThemedIconDelegate(
    constantState: ThemedIconInfo,
    val bitmapInfo: BitmapInfo,
    val paint: Paint,
) : FastBitmapDrawableDelegate {

    private val colorFg = constantState.colorFg
    private val useNosIcons = constantState.useNosIcons

    // The foreground/monochrome icon for the app
    private val monoIcon = constantState.mono
    private val monoPaint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = getColorMultipliedFilter(colorFg, paint.colorFilter)
        }

    private val shapeBounds = Rect(0, 0, bitmapInfo.icon.width, bitmapInfo.icon.height)
    private val monoBounds = Rect()

    init {
        paint.color = constantState.colorBg
    }

    override fun drawContent(
        info: BitmapInfo,
        iconShape: IconShape,
        canvas: Canvas,
        bounds: Rect,
        paint: Paint,
    ) {
        canvas.drawBitmap(iconShape.shadowLayer, null, bounds, paint)

        canvas.resizeToContentSize(bounds, iconShape.pathSize.toFloat()) {
            clipPath(iconShape.path)
            drawPaint(paint)
            if (useNosIcons) {
                monoBounds.set(shapeBounds)
                monoBounds.inset(shapeBounds.width() / 4, shapeBounds.height() / 4)
                drawBitmap(monoIcon, null, monoBounds, monoPaint)
            } else {
                drawBitmap(monoIcon, null, shapeBounds, monoPaint)
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        monoPaint.alpha = alpha
    }

    override fun updateFilter(filter: ColorFilter?) {
        monoPaint.colorFilter = getColorMultipliedFilter(colorFg, filter)
    }

    override fun isThemed() = true

    override fun getIconColor(info: BitmapInfo) = colorFg

    companion object {
        const val TAG: String = "ThemedIconDrawable"

        /** Get an int array representing background and foreground colors for themed icons */
        @JvmStatic
        fun getColors(context: Context): ColorList {
            val res = context.resources
            if (useNosThemedIcons(context)) {
                val night =
                    (res.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
                val bg =
                    res.getColor(
                        if (night) android.R.color.system_neutral1_900
                        else android.R.color.system_neutral1_100
                    )
                val fg =
                    res.getColor(
                        if (night) android.R.color.system_neutral1_50
                        else android.R.color.system_neutral1_900
                    )
                return ColorList(
                    iconBackgroundColor = bg,
                    iconForegroundColor = fg,
                    iconAdaptiveBackgroundColor = bg,
                    badgeBackgroundColor = bg,
                    badgeForegroundColor = fg,
                )
            }
            return ColorList(
                iconBackgroundColor = res.getColor(R.color.themed_icon_background_color),
                iconForegroundColor = res.getColor(R.color.themed_icon_color),
                iconAdaptiveBackgroundColor =
                    res.getColor(R.color.themed_icon_adaptive_background_color),
                badgeBackgroundColor = res.getColor(R.color.themed_badge_icon_background_color),
                badgeForegroundColor = res.getColor(R.color.themed_badge_icon_color),
            )
        }

        @JvmStatic
        fun useNosThemedIcons(context: Context): Boolean =
            try {
                Settings.Secure.getInt(
                    context.contentResolver, SETTING_NOS_THEMED_ICONS, 0) != 0
            } catch (e: Exception) {
                false
            }

        const val SETTING_NOS_THEMED_ICONS = "nos_themed_icons"
    }
}

class ThemedIconInfo(
    val mono: Bitmap,
    val colorBg: Int,
    val colorFg: Int,
    val useNosIcons: Boolean = false,
) : DelegateFactory {

    override fun newDelegate(
        bitmapInfo: BitmapInfo,
        iconShape: IconShape,
        paint: Paint,
        host: FastBitmapDrawable,
    ) = ThemedIconDelegate(this, bitmapInfo, paint)
}

data class ColorList(
    val iconBackgroundColor: Int,
    val iconForegroundColor: Int,
    val iconAdaptiveBackgroundColor: Int,
    val badgeBackgroundColor: Int,
    val badgeForegroundColor: Int,
)
