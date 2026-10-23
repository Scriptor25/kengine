package dev.scriptor.kengine.math

import jdk.incubator.vector.FloatVector
import jdk.incubator.vector.VectorSpecies

private val species: VectorSpecies<Float> = FloatVector.SPECIES_PREFERRED

operator fun FloatArray.plusAssign(v: FloatArray) {
    require(size == v.size)

    val upperbound = species.loopBound(size)
    var i = 0

    while (i < upperbound) {
        val va = FloatVector.fromArray(species, this, i)
        val vb = FloatVector.fromArray(species, v, i)

        va.add(vb).intoArray(this, i)

        i += species.length()
    }

    while (i < size) {
        this[i] += v[i]
        i++
    }
}

operator fun FloatArray.minusAssign(v: FloatArray) {
    require(size == v.size)

    val upperbound = species.loopBound(size)
    var i = 0

    while (i < upperbound) {
        val va = FloatVector.fromArray(species, this, i)
        val vb = FloatVector.fromArray(species, v, i)

        va.sub(vb).intoArray(this, i)

        i += species.length()
    }

    while (i < size) {
        this[i] -= v[i]
        i++
    }
}

operator fun FloatArray.timesAssign(t: Float) {
    val upperbound = species.loopBound(size)
    var i = 0

    while (i < upperbound) {
        val v = FloatVector.fromArray(species, this, i)

        v.mul(t).intoArray(this, i)

        i += species.length()
    }

    while (i < size) {
        this[i] *= t
        i++
    }
}

operator fun FloatArray.divAssign(t: Float) = timesAssign(1.0f / t)
