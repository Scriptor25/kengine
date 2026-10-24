package dev.scriptor.kengine.math

import java.nio.ByteBuffer
import kotlin.math.sqrt

value class Vec4i(
    val x: Int,
    val y: Int,
    val z: Int,
    val w: Int,
) {
    companion object {
        const val SIZE_BYTES = 4 * Int.SIZE_BYTES
    }

    val xy: Vec2i
        get() = Vec2i(x, y)

    val xyz: Vec3i
        get() = Vec3i(x, y, z)

    val length
        get() = sqrt((x * x + y * y + z * z + w * w).toFloat())

    constructor() : this(0, 0, 0, 0)
    constructor(s: Int) : this(s, s, s, s)
    constructor(xyz: Vec3i, w: Int) : this(xyz.x, xyz.y, xyz.z, w)
    constructor(v: Vec4i) : this(v.x, v.y, v.z, v.w)

    operator fun plus(v: Vec4i): Vec4i = Vec4i(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vec4i): Vec4i = Vec4i(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun times(t: Int): Vec4i = Vec4i(x * t, y * t, z * t, w * t)
    operator fun div(t: Int): Vec4i = Vec4i(x / t, y / t, z / t, w / t)
    operator fun rem(t: Int): Vec4i = Vec4i(x % t, y % t, z % t, w % t)

    infix fun dot(v: Vec4i): Int = x * v.x + y * v.y + z * v.z + w * v.w

    override fun toString(): String = "($x, $y, $z, $w)"
}

operator fun Int.times(v: Vec4i): Vec4i = v * this

fun ByteBuffer.getVec4i(position: Int): Vec4i {
    return Vec4i(
        getInt(position),
        getInt(position + Int.SIZE_BYTES),
        getInt(position + Int.SIZE_BYTES * 2),
        getInt(position + Int.SIZE_BYTES * 3),
    )
}

fun ByteBuffer.putVec4i(x: Int, y: Int, z: Int, w: Int): ByteBuffer {
    putInt(x)
    putInt(y)
    putInt(z)
    putInt(w)
    return this
}
