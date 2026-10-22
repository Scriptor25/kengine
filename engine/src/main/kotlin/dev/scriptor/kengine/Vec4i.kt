package dev.scriptor.kengine

import java.nio.ByteBuffer
import kotlin.math.sqrt

interface Vec4ic {

    companion object {
        const val SIZE_BYTES = 4 * Int.SIZE_BYTES
    }

    val x: Int
    val y: Int
    val z: Int
    val w: Int

    val length
        get() = sqrt((x * x + y * y + z * z + w * w).toFloat())

    val xy: Vec2i
        get() = Vec2i(x, y)
    val xyz: Vec3i
        get() = Vec3i(x, y, z)

    operator fun plus(v: Vec4ic): Vec4i = Vec4i(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vec4ic): Vec4i = Vec4i(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun times(t: Int): Vec4i = Vec4i(x * t, y * t, z * t, w * t)
    operator fun div(t: Int): Vec4i = Vec4i(x / t, y / t, z / t, w / t)
    operator fun rem(t: Int): Vec4i = Vec4i(x % t, y % t, z % t, w % t)

    infix fun dot(v: Vec4ic): Int = x * v.x + y * v.y + z * v.z + w * v.w
}

class Vec4i
internal constructor(val value: IntArray) : Vec4ic {

    override var x: Int
        get() = value[0]
        set(value) {
            this.value[0] = value
        }
    override var y: Int
        get() = value[1]
        set(value) {
            this.value[1] = value
        }
    override var z: Int
        get() = value[2]
        set(value) {
            this.value[2] = value
        }
    override var w: Int
        get() = value[3]
        set(value) {
            this.value[3] = value
        }

    override val xy: Vec2i
        get() = Vec2i(value)
    override val xyz: Vec3i
        get() = Vec3i(value)

    constructor() : this(0, 0, 0, 0)
    constructor(s: Int) : this(s, s, s, s)
    constructor(xyz: Vec3ic, w: Int) : this(xyz.x, xyz.y, xyz.z, w)
    constructor(x: Int, y: Int, z: Int, w: Int) : this(intArrayOf(x, y, z, w))

    operator fun plusAssign(v: Vec4ic) {
        x += v.x
        y += v.y
        z += v.z
        w += v.w
    }

    operator fun minusAssign(v: Vec4ic) {
        x -= v.x
        y -= v.y
        z -= v.z
        w -= v.w
    }

    operator fun timesAssign(t: Int) {
        x *= t
        y *= t
        z *= t
        w *= t
    }

    operator fun divAssign(t: Int) {
        x /= t
        y /= t
        z /= t
        w /= t
    }

    operator fun remAssign(t: Int) {
        x %= t
        y %= t
        z %= t
        w %= t
    }

    fun timesPlusAssign(v: Vec4ic, t: Int): Vec4i {
        x += v.x * t
        y += v.y * t
        z += v.z * t
        w += v.w * t
        return this
    }

    override fun toString(): String = "($x, $y, $z, $w)"
}

operator fun Int.times(v: Vec4ic): Vec4i = v * this

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
