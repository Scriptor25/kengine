package dev.scriptor.kengine.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

interface Mat4fc {

    operator fun get(i: Int, j: Int): Float

    fun transpose(): Mat4f = Mat4f(
        floatArrayOf(
            this[0, 0], this[1, 0], this[2, 0], this[3, 0],
            this[0, 1], this[1, 1], this[2, 1], this[3, 1],
            this[0, 2], this[1, 2], this[2, 2], this[3, 2],
            this[0, 3], this[1, 3], this[2, 3], this[3, 3],
        )
    )

    operator fun times(mat: Mat4fc): Mat4f {
        val o = Mat4f(FloatArray(16))

        for (i in 0..3) {
            for (j in 0..3) {
                o[i, j] = 0.0f +
                        this[i, 0] * mat[0, j] +
                        this[i, 1] * mat[1, j] +
                        this[i, 2] * mat[2, j] +
                        this[i, 3] * mat[3, j]
            }
        }

        return o
    }

    operator fun times(vec: Vec4fc): Vec4f {
        val o = Vec4f()

        for (i in 0..3) {
            o[i] = 0.0f +
                    this[i, 0] * vec[0] +
                    this[i, 1] * vec[1] +
                    this[i, 2] * vec[2] +
                    this[i, 3] * vec[3]
        }

        return o
    }

    companion object {

        private const val DEG_TO_RAD = (Math.PI / 180.0).toFloat()

        fun perspective(
            fovY: Float,
            aspect: Float,
            near: Float,
            far: Float,
        ): Mat4f {
            val f = 1.0f / tan(fovY * DEG_TO_RAD * 0.5f)

            return Mat4f(
                floatArrayOf(
                    f / aspect, 0.0f, 0.0f, 0.0f,
                    0.0f, f, 0.0f, 0.0f,
                    0.0f, 0.0f, -(far + near) / (far - near), -1.0f,
                    0.0f, 0.0f, -(2.0f * far * near) / (far - near), 0.0f,
                )
            )
        }

        fun orthographic(
            left: Float,
            right: Float,
            bottom: Float,
            top: Float,
            near: Float,
            far: Float,
        ): Mat4f {
            return Mat4f(
                floatArrayOf(
                    2f / (right - left), 0f, 0f, 0f,
                    0f, 2f / (top - bottom), 0f, 0f,
                    0f, 0f, -2f / (far - near), 0f,

                    -(right + left) / (right - left),
                    -(top + bottom) / (top - bottom),
                    -(far + near) / (far - near),
                    1f,
                )
            )
        }

        fun translation(
            x: Float,
            y: Float,
            z: Float
        ): Mat4f = Mat4f().apply {
            this[0, 3] = x
            this[1, 3] = y
            this[2, 3] = z
        }

        fun scaling(
            x: Float,
            y: Float,
            z: Float
        ): Mat4f = Mat4f(
            floatArrayOf(
                x, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                0f, 0f, z, 0f,
                0f, 0f, 0f, 1f,
            )
        )

        fun rotationX(angle: Float): Mat4f {
            val c = cos(angle * DEG_TO_RAD)
            val s = sin(angle * DEG_TO_RAD)

            return Mat4f(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, c, s, 0f,
                    0f, -s, c, 0f,
                    0f, 0f, 0f, 1f,
                )
            )
        }

        fun rotationY(angle: Float): Mat4f {
            val c = cos(angle * DEG_TO_RAD)
            val s = sin(angle * DEG_TO_RAD)

            return Mat4f(
                floatArrayOf(
                    c, 0f, -s, 0f,
                    0f, 1f, 0f, 0f,
                    s, 0f, c, 0f,
                    0f, 0f, 0f, 1f,
                )
            )
        }

        fun rotationZ(angle: Float): Mat4f {
            val c = cos(angle * DEG_TO_RAD)
            val s = sin(angle * DEG_TO_RAD)

            return Mat4f(
                floatArrayOf(
                    c, s, 0f, 0f,
                    -s, c, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f,
                )
            )
        }

        fun lookAt(
            eye: Vec3fc,
            center: Vec3fc,
            up: Vec3fc
        ): Mat4f {
            val fx = center[0] - eye[0]
            val fy = center[1] - eye[1]
            val fz = center[2] - eye[2]

            val fl = sqrt(fx * fx + fy * fy + fz * fz)

            val f0 = fx / fl
            val f1 = fy / fl
            val f2 = fz / fl

            val sx = f1 * up[2] - f2 * up[1]
            val sy = f2 * up[0] - f0 * up[2]
            val sz = f0 * up[1] - f1 * up[0]

            val sl = sqrt(sx * sx + sy * sy + sz * sz)

            val s0 = sx / sl
            val s1 = sy / sl
            val s2 = sz / sl

            val u0 = s1 * f2 - s2 * f1
            val u1 = s2 * f0 - s0 * f2
            val u2 = s0 * f1 - s1 * f0

            return Mat4f(
                floatArrayOf(
                    s0, u0, -f0, 0f,
                    s1, u1, -f1, 0f,
                    s2, u2, -f2, 0f,

                    -(s0 * eye[0] + s1 * eye[1] + s2 * eye[2]),
                    -(u0 * eye[0] + u1 * eye[1] + u2 * eye[2]),
                    f0 * eye[0] + f1 * eye[1] + f2 * eye[2],
                    1f,
                )
            )
        }

        fun trs(
            translation: Vec3fc,
            rotation: Vec3fc,
            scale: Vec3fc
        ): Mat4f {
            return translation(translation[0], translation[1], translation[2]) *
                    rotationZ(rotation[2]) *
                    rotationY(rotation[1]) *
                    rotationX(rotation[0]) *
                    scaling(scale[0], scale[1], scale[2])
        }
    }
}

class Mat4f(val m: FloatArray) : Mat4fc {

    init {
        require(m.size == 16)
    }

    constructor() : this(1.0f)
    constructor(s: Float) : this(
        floatArrayOf(
            s, 0.0f, 0.0f, 0.0f,
            0.0f, s, 0.0f, 0.0f,
            0.0f, 0.0f, s, 0.0f,
            0.0f, 0.0f, 0.0f, s,
        )
    )

    override fun get(i: Int, j: Int): Float = m[i + j * 4]

    operator fun set(i: Int, j: Int, value: Float) {
        m[i + j * 4] = value
    }
}
