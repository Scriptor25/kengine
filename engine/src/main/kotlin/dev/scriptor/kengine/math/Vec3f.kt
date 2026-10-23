package dev.scriptor.kengine.math

import java.nio.ByteBuffer
import kotlin.math.sqrt

interface Vec3fc {

    companion object {
        const val SIZE_BYTES = 3 * Float.SIZE_BYTES
    }

    val x: Float
    val y: Float
    val z: Float

    val xy: Vec2fc

    val length
        get() = sqrt(x * x + y * y + z * z)

    operator fun get(i: Int): Float = when (i) {
        0 -> x
        1 -> y
        2 -> z
        else -> error("index $i out of bounds")
    }

    operator fun plus(v: Vec3fc): Vec3f = Vec3f(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vec3fc): Vec3f = Vec3f(x - v.x, y - v.y, z - v.z)
    operator fun times(t: Float): Vec3f = Vec3f(x * t, y * t, z * t)
    operator fun div(t: Float): Vec3f = Vec3f(x / t, y / t, z / t)
    operator fun rem(t: Float): Vec3f = Vec3f(x % t, y % t, z % t)

    infix fun dot(v: Vec3fc): Float = x * v.x + y * v.y + z * v.z
    infix fun cross(v: Vec3fc): Vec3f = Vec3f(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x,
    )
}

data class Vec3f(
    override var x: Float,
    override var y: Float,
    override var z: Float,
) : Vec3fc {

    override val xy: Vec2fc
        get() = object : Vec2fc {
            override val x: Float
                get() = this@Vec3f.x
            override val y: Float
                get() = this@Vec3f.y
        }

    constructor() : this(0.0f, 0.0f, 0.0f)
    constructor(s: Float) : this(s, s, s)
    constructor(xy: Vec2fc, z: Float) : this(xy.x, xy.y, z)
    constructor(v: Vec3fc) : this(v.x, v.y, v.z)

    operator fun plusAssign(v: Vec3fc) {
        x += v.x
        y += v.y
        z += v.z
    }

    operator fun minusAssign(v: Vec3fc) {
        x -= v.x
        y -= v.y
        z -= v.z
    }

    operator fun timesAssign(t: Float) {
        x *= t
        y *= t
        z *= t
    }

    operator fun divAssign(t: Float) {
        x /= t
        y /= t
        z /= t
    }

    operator fun remAssign(t: Float) {
        x %= t
        y %= t
        z %= t
    }

    fun fma(v: Vec3fc, t: Float): Vec3f {
        x += v.x * t
        y += v.y * t
        z += v.z * t
        return this
    }

    override fun toString(): String = "($x, $y, $z)"
}

operator fun Float.times(v: Vec3fc): Vec3f = v * this

fun ByteBuffer.getVec3f(position: Int): Vec3f {
    return Vec3f(
        getFloat(position),
        getFloat(position + Float.SIZE_BYTES),
        getFloat(position + Float.SIZE_BYTES * 2),
    )
}

fun ByteBuffer.putVec3f(x: Float, y: Float, z: Float): ByteBuffer {
    putFloat(x)
    putFloat(y)
    putFloat(z)
    return this
}
