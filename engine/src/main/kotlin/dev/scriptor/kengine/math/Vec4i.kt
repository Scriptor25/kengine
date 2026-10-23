package dev.scriptor.kengine.math

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

    val xy: Vec2ic
    val xyz: Vec3ic

    val length
        get() = sqrt((x * x + y * y + z * z + w * w).toFloat())

    operator fun plus(v: Vec4ic): Vec4i = Vec4i(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vec4ic): Vec4i = Vec4i(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun times(t: Int): Vec4i = Vec4i(x * t, y * t, z * t, w * t)
    operator fun div(t: Int): Vec4i = Vec4i(x / t, y / t, z / t, w / t)
    operator fun rem(t: Int): Vec4i = Vec4i(x % t, y % t, z % t, w % t)

    infix fun dot(v: Vec4ic): Int = x * v.x + y * v.y + z * v.z + w * v.w
}

data class Vec4i(
    override var x: Int,
    override var y: Int,
    override var z: Int,
    override var w: Int,
) : Vec4ic {

    override val xy: Vec2ic
        get() = object : Vec2ic {
            override val x: Int
                get() = this@Vec4i.x
            override val y: Int
                get() = this@Vec4i.y
        }

    override val xyz: Vec3ic
        get() = object : Vec3ic {
            override val x: Int
                get() = this@Vec4i.x
            override val y: Int
                get() = this@Vec4i.y
            override val z: Int
                get() = this@Vec4i.z

            override val xy: Vec2ic
                get() = object : Vec2ic {
                    override val x: Int
                        get() = this@Vec4i.x
                    override val y: Int
                        get() = this@Vec4i.y
                }
        }

    constructor() : this(0, 0, 0, 0)
    constructor(s: Int) : this(s, s, s, s)
    constructor(xyz: Vec3ic, w: Int) : this(xyz.x, xyz.y, xyz.z, w)
    constructor(v: Vec4ic) : this(v.x, v.y, v.z, v.w)

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

    fun fma(v: Vec4ic, t: Int): Vec4i {
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
