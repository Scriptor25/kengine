package dev.scriptor.kengine.math.simd

import jdk.incubator.vector.*

sealed interface VectorAdapter<E : Number, A : Any> {
    val species: VectorSpecies<E>

    fun sizeof(array: A): Int

    fun load(element: E): Vector<E>
    fun load(array: A, offset: Int, mask: VectorMask<E>): Vector<E>
    fun store(array: A, offset: Int, mask: VectorMask<E>, vector: Vector<E>)
}

object IntVectorAdapter : VectorAdapter<Int, IntArray> {
    override val species: VectorSpecies<Int> = IntVector.SPECIES_PREFERRED

    override fun sizeof(
        array: IntArray,
    ): Int = array.size

    override fun load(
        element: Int,
    ): Vector<Int> = IntVector.broadcast(species, element)

    override fun load(
        array: IntArray,
        offset: Int,
        mask: VectorMask<Int>,
    ): Vector<Int> = IntVector.fromArray(species, array, offset, mask)

    override fun store(
        array: IntArray,
        offset: Int,
        mask: VectorMask<Int>,
        vector: Vector<Int>,
    ) = when {
        vector is IntVector ->
            vector.intoArray(array, offset, mask)

        else -> error("invalid value")
    }
}

object FloatVectorAdapter : VectorAdapter<Float, FloatArray> {
    override val species: VectorSpecies<Float> = FloatVector.SPECIES_PREFERRED

    override fun sizeof(
        array: FloatArray,
    ): Int = array.size

    override fun load(
        element: Float,
    ): Vector<Float> = FloatVector.broadcast(species, element)

    override fun load(
        array: FloatArray,
        offset: Int,
        mask: VectorMask<Float>,
    ): Vector<Float> = FloatVector.fromArray(species, array, offset, mask)

    override fun store(
        array: FloatArray,
        offset: Int,
        mask: VectorMask<Float>,
        vector: Vector<Float>,
    ) = when {
        vector is FloatVector ->
            vector.intoArray(array, offset, mask)

        else -> error("invalid value")
    }
}
