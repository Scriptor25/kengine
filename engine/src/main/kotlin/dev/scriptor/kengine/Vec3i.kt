package dev.scriptor.kengine

import kotlin.math.sqrt

data class Vec3i(
    val x: Int,
    val y: Int,
    val z: Int,
) {
    val length
        get() = sqrt((x * x + y * y + z * z).toFloat())

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
}

operator fun Int.times(v: Vec3i): Vec3i = v * this
