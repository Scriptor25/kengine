package dev.scriptor.kengine.math

import java.nio.ByteBuffer
import kotlin.math.sqrt

interface Vec2fc {

    companion object {
        const val SIZE_BYTES = 2 * Float.SIZE_BYTES
    }

    val x: Float
    val y: Float

    val length
        get() = sqrt(x * x + y * y)

    operator fun get(i: Int): Float = when (i) {
        0 -> x
        1 -> y
        else -> error("index $i out of bounds")
    }

    operator fun plus(v: Vec2fc): Vec2f = Vec2f(x + v.x, y + v.y)
    operator fun minus(v: Vec2fc): Vec2f = Vec2f(x - v.x, y - v.y)
    operator fun times(t: Float): Vec2f = Vec2f(x * t, y * t)
    operator fun div(t: Float): Vec2f = Vec2f(x / t, y / t)
    operator fun rem(t: Float): Vec2f = Vec2f(x % t, y % t)

    infix fun dot(v: Vec2fc): Float = x * v.x + y * v.y
    infix fun cross(v: Vec2fc): Float = x * v.y - y * v.x
}

data class Vec2f(
    override var x: Float,
    override var y: Float,
) : Vec2fc {

    constructor() : this(0.0f, 0.0f)
    constructor(s: Float) : this(s, s)
    constructor(v: Vec2fc) : this(v.x, v.y)

    operator fun plusAssign(v: Vec2fc) {
        x += v.x
        y += v.y
    }

    operator fun minusAssign(v: Vec2fc) {
        x -= v.x
        y -= v.y
    }

    operator fun timesAssign(t: Float) {
        x *= t
        y *= t
    }

    operator fun divAssign(t: Float) {
        x /= t
        y /= t
    }

    operator fun remAssign(t: Float) {
        x %= t
        y %= t
    }

    fun fma(v: Vec2fc, t: Float): Vec2f {
        x += v.x * t
        y += v.y * t
        return this
    }

    override fun toString(): String = "($x, $y)"
}

operator fun Float.times(v: Vec2fc): Vec2f = v * this

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
