package dev.scriptor.kengine

import kotlin.math.sqrt

data class Vec3f(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    constructor(v: Vec2f, z: Float) : this(v.x, v.y, z)

    val length
        get() = sqrt(x * x + y * y + z * z)

    operator fun plus(v: Vec3f): Vec3f = Vec3f(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vec3f): Vec3f = Vec3f(x - v.x, y - v.y, z - v.z)
    operator fun times(t: Float): Vec3f = Vec3f(x * t, y * t, z * t)
    operator fun div(t: Float): Vec3f = Vec3f(x / t, y / t, z / t)
    operator fun rem(t: Float): Vec3f = Vec3f(x % t, y % t, z % t)

    infix fun dot(v: Vec3f): Float = x * v.x + y * v.y + z * v.z
    infix fun cross(v: Vec3f): Vec3f = Vec3f(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x,
    )
}

operator fun Float.times(v: Vec3f): Vec3f = v * this
