package dev.scriptor.kengine.math

import java.nio.ByteBuffer
import kotlin.math.sqrt

value class Vec3i(
    val x: Int,
    val y: Int,
    val z: Int,
) {
    companion object {
        const val SIZE_BYTES = 3 * Int.SIZE_BYTES
    }

    val xy: Vec2i
        get() = Vec2i(x, y)

    val length
        get() = sqrt((x * x + y * y + z * z).toFloat())

    constructor() : this(0, 0, 0)
    constructor(s: Int) : this(s, s, s)
    constructor(xy: Vec2i, z: Int) : this(xy.x, xy.y, z)
    constructor(v: Vec3i) : this(v.x, v.y, v.z)

    operator fun plus(v: Vec3i): Vec3i = Vec3i(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vec3i): Vec3i = Vec3i(x - v.x, y - v.y, z - v.z)
    operator fun times(t: Int): Vec3i = Vec3i(x * t, y * t, z * t)
    operator fun div(t: Int): Vec3i = Vec3i(x / t, y / t, z / t)
    operator fun rem(t: Int): Vec3i = Vec3i(x % t, y % t, z % t)

    infix fun dot(v: Vec3i): Int = x * v.x + y * v.y + z * v.z
    infix fun cross(v: Vec3i): Vec3i = Vec3i(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x,
    )

    override fun toString(): String = "($x, $y, $z)"
}

operator fun Int.times(v: Vec3i): Vec3i = v * this

fun ByteBuffer.getVec3i(position: Int): Vec3i {
    return Vec3i(
        getInt(position),
        getInt(position + Int.SIZE_BYTES),
        getInt(position + Int.SIZE_BYTES * 2),
    )
}

fun ByteBuffer.putVec3i(x: Int, y: Int, z: Int): ByteBuffer {
    putInt(x)
    putInt(y)
    putInt(z)
    return this
}
