package dev.scriptor.kengine.math

import java.nio.ByteBuffer
import kotlin.math.sqrt

value class Vec2i(
    val x: Int,
    val y: Int,
) {
    companion object {
        const val SIZE_BYTES = 2 * Int.SIZE_BYTES
    }

    constructor() : this(0, 0)
    constructor(s: Int) : this(s, s)
    constructor(v: Vec2i) : this(v.x, v.y)

    val length
        get() = sqrt((x * x + y * y).toFloat())

    operator fun plus(v: Vec2i): Vec2i = Vec2i(x + v.x, y + v.y)
    operator fun minus(v: Vec2i): Vec2i = Vec2i(x - v.x, y - v.y)
    operator fun times(v: Vec2i): Vec2i = Vec2i(x * v.x, y * v.y)
    operator fun times(t: Int): Vec2i = Vec2i(x * t, y * t)
    operator fun div(t: Int): Vec2i = Vec2i(x / t, y / t)
    operator fun rem(t: Int): Vec2i = Vec2i(x % t, y % t)

    infix fun dot(v: Vec2i): Int = x * v.x + y * v.y
    infix fun cross(v: Vec2i): Int = x * v.y - y * v.x

    override fun toString(): String = "($x, $y)"
}

operator fun Int.times(v: Vec2i): Vec2i = v * this

fun ByteBuffer.getVec2i(position: Int): Vec2i {
    return Vec2i(
        getInt(position),
        getInt(position + Int.SIZE_BYTES),
    )
}

fun ByteBuffer.putVec2i(x: Int, y: Int): ByteBuffer {
    putInt(x)
    putInt(y)
    return this
}
