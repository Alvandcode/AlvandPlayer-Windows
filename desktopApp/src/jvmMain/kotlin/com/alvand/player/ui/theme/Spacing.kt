package com.alvand.player.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * سیستم فاصله‌بندی یکپارچه برای کل اپلیکیشن
 * بر اساس مقیاس ۴dp به عنوان واحد پایه
 */
object Spacing {
    /** واحد پایه: ۴dp */
    private const val BASE = 4

    /** ۰dp */
    val none = 0.dp
    
    /** ۴dp - xs */
    val xs = (BASE * 1).dp
    
    /** ۸dp - sm */
    val sm = (BASE * 2).dp
    
    /** ۱۲dp - md */
    val md = (BASE * 3).dp
    
    /** ۱۶dp - lg */
    val lg = (BASE * 4).dp
    
    /** ۲۴dp - xl */
    val xl = (BASE * 6).dp
    
    /** ۳۲dp - 2xl */
    val xxl = (BASE * 8).dp
    
    /** ۴۸dp - 3xl */
    val xxxl = (BASE * 12).dp
    
    /** ۶۴dp - 4xl */
    val huge = (BASE * 16).dp
    
    /** حاشیه استاندارد کارت‌ها */
    val cardPadding = md
    
    /** فاصله بین المان‌های درون کارت */
    val cardInnerGap = sm
    
    /** فاصله بین کارت‌ها در لیست */
    val listGap = sm
    
    /** حاشیه صفحه */
    val pageMargin = lg
    
    /** فاصله بین секشن‌ها */
    val sectionGap = xl
    
    /** حاشیه کانتینر اصلی */
    val containerPadding = lg
    
    /** Radius استاندارد */
    val radiusSm = 8.dp
    val radiusMd = 12.dp
    val radiusLg = 16.dp
    val radiusXl = 24.dp
    val radiusFull = 9999.dp

    /** اندازه متن استاندارد */
    val textXs = 10.sp
    val textSm = 11.sp
    val textMd = 12.sp
    val textBase = 13.sp
    val textLg = 14.sp
    val textXl = 16.sp
    val textXxl = 18.sp
    val textTitle = 24.sp
    val textDisplay = 30.sp
}