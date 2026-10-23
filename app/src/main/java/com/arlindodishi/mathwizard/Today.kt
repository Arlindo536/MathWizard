package com.arlindodishi.mathwizard

import java.util.TimeZone

/** Days since 1970-01-01 in the device's time zone (java.time needs API 26; minSdk is 24). */
fun epochDayNow(nowMillis: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Long =
    Math.floorDiv(nowMillis + zone.getOffset(nowMillis), 86_400_000L)
