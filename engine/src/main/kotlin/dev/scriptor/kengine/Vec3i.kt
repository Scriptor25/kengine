package dev.scriptor.kengine

import java.nio.ByteBuffer
import kotlin.math.sqrt

interface Vec3ic {

    companion object {
        const val SIZE_BYTES = 3 * Int.SIZE_BYTES
    }

    val x: Int
    val y: Int
    val z: Int

    val xy: Vec2ic

    val length
        get() = sqrt((x * x + y * y + z * z).toFloat())

    operator fun plus(v: Vec3ic): Vec3i = Vec3i(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vec3ic): Vec3i = Vec3i(x - v.x, y - v.y, z - v.z)
    operator fun times(t: Int): Vec3i = Vec3i(x * t, y * t, z * t)
    operator fun div(t: Int): Vec3i = Vec3i(x / t, y / t, z / t)
    operator fun rem(t: Int): Vec3i = Vec3i(x % t, y % t, z % t)

    infix fun dot(v: Vec3ic): Int = x * v.x + y * v.y + z * v.z
    infix fun cross(v: Vec3ic): Vec3i = Vec3i(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x,
    )
}

data class Vec3i(
    override var x: Int,
    override var y: Int,
    override var z: Int,
) : Vec3ic {

    override val xy: Vec2ic
        get() = object : Vec2ic {
            override val x: Int
                get() = this@Vec3i.x
            override val y: Int
                get() = this@Vec3i.y
        }

    constructor() : this(0, 0, 0)
    constructor(s: Int) : this(s, s, s)
    constructor(xy: Vec2ic, z: Int) : this(xy.x, xy.y, z)

    operator fun plusAssign(v: Vec3ic) {
        x += v.x
        y += v.y
        z += v.z
    }

    operator fun minusAssign(v: Vec3ic) {
        x -= v.x
        y -= v.y
        z -= v.z
    }

    operator fun timesAssign(t: Int) {
        x *= t
        y *= t
        z *= t
    }

    operator fun divAssign(t: Int) {
        x /= t
        y /= t
        z /= t
    }

    operator fun remAssign(t: Int) {
        x %= t
        y %= t
        z %= t
    }

    fun timesPlusAssign(v: Vec3ic, t: Int): Vec3i {
        x += v.x * t
        y += v.y * t
        z += v.z * t
        return this
    }

    override fun toString(): String = "($x, $y, $z)"
}

operator fun Int.times(v: Vec3ic): Vec3i = v * this

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
