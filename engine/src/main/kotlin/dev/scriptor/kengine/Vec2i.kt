package dev.scriptor.kengine

import kotlin.math.sqrt

data class Vec2i(
    val x: Int,
    val y: Int,
) {
    val length
        get() = sqrt((x * x + y * y).toFloat())

    operator fun plus(v: Vec2i): Vec2i = Vec2i(x + v.x, y + v.y)
    operator fun minus(v: Vec2i): Vec2i = Vec2i(x - v.x, y - v.y)
    operator fun times(t: Int): Vec2i = Vec2i(x * t, y * t)
    operator fun div(t: Int): Vec2i = Vec2i(x / t, y / t)
    operator fun rem(t: Int): Vec2i = Vec2i(x % t, y % t)

    infix fun dot(v: Vec2i): Int = x * v.x + y * v.y
    infix fun cross(v: Vec2i): Int = x * v.y - y * v.x
}

operator fun Int.times(v: Vec2i): Vec2i = v * this
