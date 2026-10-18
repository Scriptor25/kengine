package dev.scriptor.kengine

import kotlin.math.sqrt

data class Vec4i(
    val x: Int,
    val y: Int,
    val z: Int,
    val w: Int,
) {
    val length
        get() = sqrt((x * x + y * y + z * z + w * w).toFloat())

    operator fun plus(v: Vec4i): Vec4i = Vec4i(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vec4i): Vec4i = Vec4i(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun times(t: Int): Vec4i = Vec4i(x * t, y * t, z * t, w * t)
    operator fun div(t: Int): Vec4i = Vec4i(x / t, y / t, z / t, w / t)
    operator fun rem(t: Int): Vec4i = Vec4i(x % t, y % t, z % t, w % t)

    infix fun dot(v: Vec4i): Int = x * v.x + y * v.y + z * v.z + w * v.w
}

operator fun Int.times(v: Vec4i): Vec4i = v * this
