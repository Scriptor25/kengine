package dev.scriptor.kengine

import java.nio.ByteBuffer
import kotlin.math.sqrt

interface Vec3fc {

    companion object {
        const val SIZE_BYTES = 3 * Float.SIZE_BYTES
    }

    val x: Float
    val y: Float
    val z: Float

    val length
        get() = sqrt(x * x + y * y + z * z)

    val xy: Vec2f
        get() = Vec2f(x, y)

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

class Vec3f
internal constructor(val value: FloatArray) : Vec3fc {

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

    override val xy: Vec2f
        get() = Vec2f(value)

    constructor() : this(0.0f, 0.0f, 0.0f)
    constructor(s: Float) : this(s, s, s)
    constructor(xy: Vec2fc, z: Float) : this(xy.x, xy.y, z)
    constructor(x: Float, y: Float, z: Float) : this(floatArrayOf(x, y, z))

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

    fun timesPlusAssign(v: Vec3fc, t: Float): Vec3f {
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
