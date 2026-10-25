package com.fly.pkg

object WindowPositionUtils {

    fun clampPosition(
        x: Int,
        y: Int,
        screenW: Int,
        screenH: Int,
        viewW: Int,
        viewH: Int,
    ): Pair<Int, Int> {
        val maxX = (screenW - viewW).coerceAtLeast(0)
        val maxY = (screenH - viewH).coerceAtLeast(0)
        return x.coerceIn(0, maxX) to y.coerceIn(0, maxY)
    }
}
