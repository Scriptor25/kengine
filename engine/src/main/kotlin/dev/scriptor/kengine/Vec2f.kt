package dev.scriptor.kengine

import kotlin.math.sqrt

data class Vec2f(
    val x: Float,
    val y: Float,
) {
    val length
        get() = sqrt(x * x + y * y)

    operator fun plus(v: Vec2f): Vec2f = Vec2f(x + v.x, y + v.y)
    operator fun minus(v: Vec2f): Vec2f = Vec2f(x - v.x, y - v.y)
    operator fun times(t: Float): Vec2f = Vec2f(x * t, y * t)
    operator fun div(t: Float): Vec2f = Vec2f(x / t, y / t)
    operator fun rem(t: Float): Vec2f = Vec2f(x % t, y % t)

    infix fun dot(v: Vec2f): Float = x * v.x + y * v.y
    infix fun cross(v: Vec2f): Float = x * v.y - y * v.x
}

operator fun Float.times(v: Vec2f): Vec2f = v * this
