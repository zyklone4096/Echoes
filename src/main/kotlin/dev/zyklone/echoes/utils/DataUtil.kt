package dev.zyklone.echoes.utils

import java.math.RoundingMode

fun Double.scaleToString(scale: Int = 2) =
    this.toBigDecimal()
        .setScale(scale, RoundingMode.HALF_UP)
        .toString()

fun Double.formatPercent(scale: Int = 2) =
    (this * 100).scaleToString(scale) + "%"
