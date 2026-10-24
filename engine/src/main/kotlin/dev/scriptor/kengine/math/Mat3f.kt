package dev.scriptor.kengine.math

interface Mat3fc {

    operator fun get(i: Int, j: Int): Float

    fun transpose(): Mat3f = Mat3f(
        floatArrayOf(
            this[0, 0], this[1, 0], this[2, 0],
            this[0, 1], this[1, 1], this[2, 1],
            this[0, 2], this[1, 2], this[2, 2],
        )
    )

    operator fun times(mat: Mat3fc): Mat3f {
        val o = Mat3f(FloatArray(9))

        for (i in 0..2) {
            for (j in 0..2) {
                o[i, j] = 0.0f +
                        this[i, 0] * mat[0, j] +
                        this[i, 1] * mat[1, j] +
                        this[i, 2] * mat[2, j]
            }
        }

        return o
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

class Mat3f(val m: FloatArray) : Mat3fc {

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

    override fun get(i: Int, j: Int): Float = m[i + j * 3]

    operator fun set(i: Int, j: Int, value: Float) {
        m[i + j * 3] = value
    }
}
