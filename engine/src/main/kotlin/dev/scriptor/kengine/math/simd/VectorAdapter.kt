package dev.scriptor.kengine.math.simd

import jdk.incubator.vector.*
import jdk.incubator.vector.VectorOperators.*

sealed interface VectorAdapter<E : Number, A : Any> {
    val species: VectorSpecies<E>

    fun sizeof(array: A): Int

    fun load(element: E): Vector<E>
    fun load(array: A, offset: Int, mask: VectorMask<E>): Vector<E>
    fun store(array: A, offset: Int, mask: VectorMask<E>, vector: Vector<E>)

    fun lanewise(op: Unary, v0: Vector<E>): Vector<E> =
        v0.lanewise(op)

    fun lanewise(op: Binary, v0: Vector<E>, v1: Vector<E>): Vector<E> =
        v0.lanewise(op, v1)

    fun lanewise(op: Associative, v0: Vector<E>, v1: Vector<E>): Vector<E> =
        v0.lanewise(op, v1)

    fun lanewise(op: Ternary, v0: Vector<E>, v1: Vector<E>, v2: Vector<E>): Vector<E> =
        v0.lanewise(op, v1, v2)

    fun test(op: Test, v0: Vector<E>): VectorMask<E> =
        v0.test(op)

    fun compare(op: Comparison, v0: Vector<E>, v1: Vector<E>): VectorMask<E> =
        v0.compare(op, v1)

    fun select(mask: VectorMask<E>, v0: Vector<E>, v1: Vector<E>): Vector<E> =
        v1.blend(v0, mask)

    fun and(v0: VectorMask<E>, v1: VectorMask<E>): VectorMask<E> =
        v0.and(v1)

    fun or(v0: VectorMask<E>, v1: VectorMask<E>): VectorMask<E> =
        v0.or(v1)
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
