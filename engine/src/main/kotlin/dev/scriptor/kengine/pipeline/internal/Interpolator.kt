package dev.scriptor.kengine.pipeline.internal

import dev.scriptor.kengine.math.Vec2f
import dev.scriptor.kengine.math.Vec3f
import dev.scriptor.kengine.math.Vec4f

sealed interface Interpolator<T : Any> {

    operator fun invoke(a: T, b: T, t: Float): T
    operator fun invoke(a: T, wa: Float, b: T, wb: Float, c: T, wc: Float): T

    object FLOAT : Interpolator<Float> {

        override fun invoke(
            a: Float,
            b: Float,
            t: Float,
        ): Float = a + (b - a) * t

        override fun invoke(
            a: Float,
            wa: Float,
            b: Float,
            wb: Float,
            c: Float,
            wc: Float,
        ): Float = a * wa + b * wb + c * wc
    }

    object VEC2F : Interpolator<Vec2f> {

        override fun invoke(
            a: Vec2f,
            b: Vec2f,
            t: Float,
        ): Vec2f = Vec2f(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
        )

        override fun invoke(
            a: Vec2f,
            wa: Float,
            b: Vec2f,
            wb: Float,
            c: Vec2f,
            wc: Float,
        ): Vec2f = Vec2f(
            a.x * wa + b.x * wb + c.x * wc,
            a.y * wa + b.y * wb + c.y * wc,
        )
    }

    object VEC3F : Interpolator<Vec3f> {

        override fun invoke(
            a: Vec3f,
            b: Vec3f,
            t: Float,
        ): Vec3f = Vec3f(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t,
        )

        override fun invoke(
            a: Vec3f,
            wa: Float,
            b: Vec3f,
            wb: Float,
            c: Vec3f,
            wc: Float,
        ): Vec3f = Vec3f(
            a.x * wa + b.x * wb + c.x * wc,
            a.y * wa + b.y * wb + c.y * wc,
            a.z * wa + b.z * wb + c.z * wc,
        )
    }

    object VEC4F : Interpolator<Vec4f> {

        override fun invoke(
            a: Vec4f,
            b: Vec4f,
            t: Float,
        ): Vec4f = Vec4f(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t,
            a.w + (b.w - a.w) * t,
        )

        override fun invoke(
            a: Vec4f,
            wa: Float,
            b: Vec4f,
            wb: Float,
            c: Vec4f,
            wc: Float,
        ): Vec4f = Vec4f(
            a.x * wa + b.x * wb + c.x * wc,
            a.y * wa + b.y * wb + c.y * wc,
            a.z * wa + b.z * wb + c.z * wc,
            a.w * wa + b.w * wb + c.w * wc,
        )
    }
}
