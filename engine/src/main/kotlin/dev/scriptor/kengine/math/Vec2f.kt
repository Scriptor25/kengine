package dev.scriptor.kengine.math

import java.nio.ByteBuffer
import kotlin.math.sqrt

class Vec2f(
    val x: Float,
    val y: Float,
) {
    companion object {
        const val SIZE_BYTES = 2 * Float.SIZE_BYTES
    }

    constructor() : this(0.0f, 0.0f)
    constructor(s: Float) : this(s, s)
    constructor(v: Vec2f) : this(v.x, v.y)

    val length
        get() = sqrt(x * x + y * y)

    operator fun get(i: Int): Float = when (i) {
        0 -> x
        1 -> y
        else -> error("index $i out of bounds")
    }

    operator fun plus(v: Vec2f): Vec2f = Vec2f(x + v.x, y + v.y)
    operator fun minus(v: Vec2f): Vec2f = Vec2f(x - v.x, y - v.y)
    operator fun times(v: Vec2f): Vec2f = Vec2f(x * v.x, y * v.y)
    operator fun times(t: Float): Vec2f = Vec2f(x * t, y * t)
    operator fun div(t: Float): Vec2f = Vec2f(x / t, y / t)
    operator fun rem(t: Float): Vec2f = Vec2f(x % t, y % t)

    infix fun dot(v: Vec2f): Float = x * v.x + y * v.y
    infix fun cross(v: Vec2f): Float = x * v.y - y * v.x

    override fun toString(): String = "($x, $y)"
}

operator fun Float.times(v: Vec2f): Vec2f = v * this

fun ByteBuffer.getVec2f(position: Int): Vec2f {
    return Vec2f(
        getFloat(position),
        getFloat(position + Float.SIZE_BYTES),
    )
}

fun ByteBuffer.putVec2f(x: Float, y: Float): ByteBuffer {
    putFloat(x)
    putFloat(y)
    return this
}
