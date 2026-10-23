package dev.scriptor.kengine.math

import java.nio.ByteBuffer
import kotlin.math.sqrt

interface Vec2ic {

    companion object {
        const val SIZE_BYTES = 2 * Int.SIZE_BYTES
    }

    val x: Int
    val y: Int

    val length
        get() = sqrt((x * x + y * y).toFloat())

    operator fun plus(v: Vec2ic): Vec2i = Vec2i(x + v.x, y + v.y)
    operator fun minus(v: Vec2ic): Vec2i = Vec2i(x - v.x, y - v.y)
    operator fun times(t: Int): Vec2i = Vec2i(x * t, y * t)
    operator fun div(t: Int): Vec2i = Vec2i(x / t, y / t)
    operator fun rem(t: Int): Vec2i = Vec2i(x % t, y % t)

    infix fun dot(v: Vec2ic): Int = x * v.x + y * v.y
    infix fun cross(v: Vec2ic): Int = x * v.y - y * v.x
}

data class Vec2i(
    override var x: Int,
    override var y: Int,
) : Vec2ic {

    constructor() : this(0, 0)
    constructor(s: Int) : this(s, s)
    constructor(v: Vec2ic) : this(v.x, v.y)

    operator fun plusAssign(v: Vec2ic) {
        x += v.x
        y += v.y
    }

    operator fun minusAssign(v: Vec2ic) {
        x -= v.x
        y -= v.y
    }

    operator fun timesAssign(t: Int) {
        x *= t
        y *= t
    }

    operator fun divAssign(t: Int) {
        x /= t
        y /= t
    }

    operator fun remAssign(t: Int) {
        x %= t
        y %= t
    }

    fun fma(v: Vec2ic, t: Int): Vec2i {
        x += v.x * t
        y += v.y * t
        return this
    }

    override fun toString(): String = "($x, $y)"
}

operator fun Int.times(v: Vec2ic): Vec2i = v * this

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
