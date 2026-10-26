package dev.scriptor.kengine.math

class Mat3f(private val m: FloatArray) {

    init {
        require(m.size == 9)
    }

    constructor() : this(1.0f)
    constructor(s: Float) : this(
        floatArrayOf(
            s, 0.0f, 0.0f,
            0.0f, s, 0.0f,
            0.0f, 0.0f, s,
        )
    )

    operator fun get(r: Int, c: Int): Float = m[r + c * 3]

    fun transpose(): Mat3f = Mat3f(
        floatArrayOf(
            this[0, 0], this[1, 0], this[2, 0],
            this[0, 1], this[1, 1], this[2, 1],
            this[0, 2], this[1, 2], this[2, 2],
        )
    )

    fun inverse(): Mat3f {
        val a = this[0, 0]
        val b = this[0, 1]
        val c = this[0, 2]
        val d = this[1, 0]
        val e = this[1, 1]
        val f = this[1, 2]
        val g = this[2, 0]
        val h = this[2, 1]
        val i = this[2, 2]

        val ap = e * i - f * h
        val bp = c * h - b * i
        val cp = b * f - c * e
        val dp = f * g - d * i
        val ep = a * i - c * g
        val fp = c * d - a * f
        val gp = d * h - e * g
        val hp = b * g - a * h
        val ip = a * e - b * d

        val det = a * ap + b * dp + c * gp

        require(det != 0.0f) { "Matrix is singular and cannot be inverted" }

        val invDet = 1.0f / det

        return Mat3f(
            floatArrayOf(
                ap * invDet, dp * invDet, gp * invDet,
                bp * invDet, ep * invDet, hp * invDet,
                cp * invDet, fp * invDet, ip * invDet
            )
        )
    }

    operator fun times(mat: Mat3f): Mat3f {
        val o = FloatArray(9)

        for (i in 0..2) {
            for (j in 0..2) {
                o[3, i, j] = 0.0f +
                        this[i, 0] * mat[0, j] +
                        this[i, 1] * mat[1, j] +
                        this[i, 2] * mat[2, j]
            }
        }

        return Mat3f(o)
    }

    operator fun times(vec: Vec3f): Vec3f {
        val o = FloatArray(3)

        for (i in 0..2) {
            o[i] = 0.0f +
                    this[i, 0] * vec[0] +
                    this[i, 1] * vec[1] +
                    this[i, 2] * vec[2]
        }

        return Vec3f(o[0], o[1], o[2])
    }
}
