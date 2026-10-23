package dev.scriptor.kengine

import java.nio.ByteBuffer
import kotlin.math.sqrt

interface Vec4fc {

    companion object {
        const val SIZE_BYTES = 4 * Float.SIZE_BYTES
    }

    val x: Float
    val y: Float
    val z: Float
    val w: Float

    val xy: Vec2fc
    val xyz: Vec3fc

    val length
        get() = sqrt(x * x + y * y + z * z + w * w)

    operator fun plus(v: Vec4fc): Vec4f = Vec4f(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vec4fc): Vec4f = Vec4f(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun times(t: Float): Vec4f = Vec4f(x * t, y * t, z * t, w * t)
    operator fun div(t: Float): Vec4f = Vec4f(x / t, y / t, z / t, w / t)
    operator fun rem(t: Float): Vec4f = Vec4f(x % t, y % t, z % t, w % t)

    infix fun dot(v: Vec4fc): Float = x * v.x + y * v.y + z * v.z + w * v.w
}

data class Vec4f(
    override var x: Float,
    override var y: Float,
    override var z: Float,
    override var w: Float,
) : Vec4fc {

    override val xy: Vec2fc
        get() = object : Vec2fc {
            override val x: Float
                get() = this@Vec4f.x
            override val y: Float
                get() = this@Vec4f.y
        }

    override val xyz: Vec3fc
        get() = object : Vec3fc {
            override val x: Float
                get() = this@Vec4f.x
            override val y: Float
                get() = this@Vec4f.y
            override val z: Float
                get() = this@Vec4f.z

            override val xy: Vec2fc
                get() = object : Vec2fc {
                    override val x: Float
                        get() = this@Vec4f.x
                    override val y: Float
                        get() = this@Vec4f.y
                }
        }

    constructor() : this(0.0f, 0.0f, 0.0f, 0.0f)
    constructor(s: Float) : this(s, s, s, s)
    constructor(xyz: Vec3fc, w: Float) : this(xyz.x, xyz.y, xyz.z, w)
    constructor(xy: Vec2fc, z: Float, w: Float) : this(xy.x, xy.y, z, w)

    operator fun plusAssign(v: Vec4fc) {
        x += v.x
        y += v.y
        z += v.z
        w += v.w
    }

    operator fun minusAssign(v: Vec4fc) {
        x -= v.x
        y -= v.y
        z -= v.z
        w -= v.w
    }

    operator fun timesAssign(t: Float) {
        x *= t
        y *= t
        z *= t
        w *= t
    }

    operator fun divAssign(t: Float) {
        x /= t
        y /= t
        z /= t
        w /= t
    }

    operator fun remAssign(t: Float) {
        x %= t
        y %= t
        z %= t
        w %= t
    }

    fun timesPlusAssign(v: Vec4fc, t: Float): Vec4f {
        x += v.x * t
        y += v.y * t
        z += v.z * t
        w += v.w * t
        return this
    }

    override fun toString(): String = "($x, $y, $z, $w)"
}

operator fun Float.times(v: Vec4fc): Vec4f = v * this

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
