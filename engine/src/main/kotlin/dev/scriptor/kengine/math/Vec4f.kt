package dev.scriptor.kengine.math

import java.nio.ByteBuffer
import kotlin.math.sqrt

value class Vec4f(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    companion object {
        const val SIZE_BYTES = 4 * Float.SIZE_BYTES
    }

    val xy: Vec2f
        get() = Vec2f(x, y)

    val xyz: Vec3f
        get() = Vec3f(x, y, z)

    val length
        get() = sqrt(x * x + y * y + z * z + w * w)

    constructor() : this(0.0f, 0.0f, 0.0f, 0.0f)
    constructor(s: Float) : this(s, s, s, s)
    constructor(xyz: Vec3f, w: Float) : this(xyz.x, xyz.y, xyz.z, w)
    constructor(xy: Vec2f, z: Float, w: Float) : this(xy.x, xy.y, z, w)
    constructor(v: Vec4f) : this(v.x, v.y, v.z, v.w)

    operator fun get(i: Int): Float = when (i) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> error("index $i out of bounds")
    }

    operator fun plus(v: Vec4f): Vec4f = Vec4f(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vec4f): Vec4f = Vec4f(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun times(v: Vec4f): Vec4f = Vec4f(x * v.x, y * v.y, z * v.z, w * v.w)
    operator fun times(t: Float): Vec4f = Vec4f(x * t, y * t, z * t, w * t)
    operator fun div(t: Float): Vec4f = Vec4f(x / t, y / t, z / t, w / t)
    operator fun rem(t: Float): Vec4f = Vec4f(x % t, y % t, z % t, w % t)

    infix fun dot(v: Vec4f): Float = x * v.x + y * v.y + z * v.z + w * v.w

    fun isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()

    override fun toString(): String = "($x, $y, $z, $w)"
}

operator fun Float.times(v: Vec4f): Vec4f = v * this

fun ByteBuffer.getVec4f(position: Int): Vec4f {
    return Vec4f(
        getFloat(position),
        getFloat(position + Float.SIZE_BYTES),
        getFloat(position + Float.SIZE_BYTES * 2),
        getFloat(position + Float.SIZE_BYTES * 3),
    )
}

fun ByteBuffer.putVec4f(x: Float, y: Float, z: Float, w: Float): ByteBuffer {
    putFloat(x)
    putFloat(y)
    putFloat(z)
    putFloat(w)
    return this
}
