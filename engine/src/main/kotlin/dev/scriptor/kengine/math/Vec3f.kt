package dev.scriptor.kengine.math

import java.nio.ByteBuffer
import kotlin.math.sqrt

value class Vec3f(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    companion object {
        const val SIZE_BYTES = 3 * Float.SIZE_BYTES
    }

    val xy: Vec2f
        get() = Vec2f(x, y)

    val length
        get() = sqrt(x * x + y * y + z * z)

    val unit: Vec3f
        get() = this / length

    constructor() : this(0.0f, 0.0f, 0.0f)
    constructor(s: Float) : this(s, s, s)
    constructor(xy: Vec2f, z: Float) : this(xy.x, xy.y, z)
    constructor(v: Vec3f) : this(v.x, v.y, v.z)

    operator fun get(i: Int): Float = when (i) {
        0 -> x
        1 -> y
        2 -> z
        else -> error("index $i out of bounds")
    }

    operator fun plus(v: Vec3f): Vec3f = Vec3f(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vec3f): Vec3f = Vec3f(x - v.x, y - v.y, z - v.z)
    operator fun times(v: Vec3f): Vec3f = Vec3f(x * v.x, y * v.y, z * v.z)
    operator fun times(t: Float): Vec3f = Vec3f(x * t, y * t, z * t)
    operator fun div(t: Float): Vec3f = Vec3f(x / t, y / t, z / t)
    operator fun rem(t: Float): Vec3f = Vec3f(x % t, y % t, z % t)

    infix fun dot(v: Vec3f): Float = x * v.x + y * v.y + z * v.z
    infix fun cross(v: Vec3f): Vec3f = Vec3f(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x,
    )

    override fun toString(): String = "($x, $y, $z)"
}

operator fun Float.times(v: Vec3f): Vec3f = v * this

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
