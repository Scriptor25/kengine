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

    val length
        get() = sqrt(x * x + y * y + z * z + w * w)

    val xy: Vec2f
        get() = Vec2f(x, y)
    val xyz: Vec3f
        get() = Vec3f(x, y, z)

    operator fun plus(v: Vec4fc): Vec4f = Vec4f(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vec4fc): Vec4f = Vec4f(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun times(t: Float): Vec4f = Vec4f(x * t, y * t, z * t, w * t)
    operator fun div(t: Float): Vec4f = Vec4f(x / t, y / t, z / t, w / t)
    operator fun rem(t: Float): Vec4f = Vec4f(x % t, y % t, z % t, w % t)

    infix fun dot(v: Vec4fc): Float = x * v.x + y * v.y + z * v.z + w * v.w
}

class Vec4f
internal constructor(val value: FloatArray) : Vec4fc {

    override var x: Float
        get() = value[0]
        set(value) {
            this.value[0] = value
        }
    override var y: Float
        get() = value[1]
        set(value) {
            this.value[1] = value
        }
    override var z: Float
        get() = value[2]
        set(value) {
            this.value[2] = value
        }
    override var w: Float
        get() = value[3]
        set(value) {
            this.value[3] = value
        }

    override val xy: Vec2f
        get() = Vec2f(value)
    override val xyz: Vec3f
        get() = Vec3f(value)

    constructor() : this(0.0f, 0.0f, 0.0f, 0.0f)
    constructor(s: Float) : this(s, s, s, s)
    constructor(xyz: Vec3fc, w: Float) : this(xyz.x, xyz.y, xyz.z, w)
    constructor(x: Float, y: Float, z: Float, w: Float) : this(floatArrayOf(x, y, z, w))

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
