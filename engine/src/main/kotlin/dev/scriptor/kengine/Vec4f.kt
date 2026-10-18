package dev.scriptor.kengine

import kotlin.math.sqrt

data class Vec4f(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    val length
        get() = sqrt(x * x + y * y + z * z + w * w)

    operator fun plus(v: Vec4f): Vec4f = Vec4f(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vec4f): Vec4f = Vec4f(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun times(t: Float): Vec4f = Vec4f(x * t, y * t, z * t, w * t)
    operator fun div(t: Float): Vec4f = Vec4f(x / t, y / t, z / t, w / t)
    operator fun rem(t: Float): Vec4f = Vec4f(x % t, y % t, z % t, w % t)

    infix fun dot(v: Vec4f): Float = x * v.x + y * v.y + z * v.z + w * v.w
}

operator fun Float.times(v: Vec4f): Vec4f = v * this
