package dev.scriptor.kengine.math.internal

import jdk.incubator.vector.*
import jdk.incubator.vector.VectorOperators.*

sealed interface VectorAdapter<E : Number, A : Any> {
    val species: VectorSpecies<E>

    fun sizeof(array: A): Int
    fun load(element: E): Vector<E>
    fun load(array: A, offset: Int, mask: VectorMask<E>): Vector<E>
    fun store(array: A, offset: Int, mask: VectorMask<E>, vector: Vector<E>)
    fun unary(operator: Unary, fst: Vector<E>): Vector<E>
    fun binary(operator: Binary, fst: Vector<E>, snd: Vector<E>): Vector<E>
    fun associative(operator: Associative, fst: Vector<E>, snd: Vector<E>): Vector<E>
    fun ternary(operator: Ternary, fst: Vector<E>, snd: Vector<E>, trd: Vector<E>): Vector<E>
    fun test(operator: Test, fst: Vector<E>): VectorMask<E>
    fun compare(operator: Comparison, fst: Vector<E>, snd: Vector<E>): VectorMask<E>
    fun select(mask: VectorMask<E>, fst: Vector<E>, snd: Vector<E>): Vector<E>

    fun and(fst: VectorMask<E>, snd: VectorMask<E>): VectorMask<E> = fst.and(snd)
    fun or(fst: VectorMask<E>, snd: VectorMask<E>): VectorMask<E> = fst.or(snd)
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

    override fun unary(
        operator: Unary,
        fst: Vector<Float>,
    ): Vector<Float> = when {
        fst is FloatVector ->
            fst.lanewise(operator)

        else -> error("invalid value")
    }

    override fun binary(
        operator: Binary,
        fst: Vector<Float>,
        snd: Vector<Float>,
    ): Vector<Float> = when {
        fst is FloatVector && snd is FloatVector ->
            fst.lanewise(operator, snd)

        else -> error("invalid value")
    }

    override fun associative(
        operator: Associative,
        fst: Vector<Float>,
        snd: Vector<Float>,
    ): Vector<Float> = when {
        fst is FloatVector && snd is FloatVector ->
            fst.lanewise(operator, snd)

        else -> error("invalid value")
    }

    override fun ternary(
        operator: Ternary,
        fst: Vector<Float>,
        snd: Vector<Float>,
        trd: Vector<Float>,
    ): Vector<Float> = when {
        fst is FloatVector && snd is FloatVector && trd is FloatVector ->
            fst.lanewise(operator, snd, trd)

        else -> error("invalid value")
    }

    override fun test(
        operator: Test,
        fst: Vector<Float>,
    ): VectorMask<Float> = when {
        fst is FloatVector ->
            fst.test(operator)

        else -> error("invalid value")
    }

    override fun compare(
        operator: Comparison,
        fst: Vector<Float>,
        snd: Vector<Float>,
    ): VectorMask<Float> = when {
        fst is FloatVector && snd is FloatVector ->
            fst.compare(operator, snd)

        else -> error("invalid value")
    }

    override fun select(
        mask: VectorMask<Float>,
        fst: Vector<Float>,
        snd: Vector<Float>,
    ): Vector<Float> = when {
        fst is FloatVector && snd is FloatVector ->
            snd.blend(fst, mask)

        else -> error("invalid value")
    }
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

    override fun unary(
        operator: Unary,
        fst: Vector<Int>,
    ): Vector<Int> = when {
        fst is IntVector ->
            fst.lanewise(operator)

        else -> error("invalid value")
    }

    override fun binary(
        operator: Binary,
        fst: Vector<Int>,
        snd: Vector<Int>,
    ): Vector<Int> = when {
        fst is IntVector && snd is IntVector ->
            fst.lanewise(operator, snd)

        else -> error("invalid value")
    }

    override fun associative(
        operator: Associative,
        fst: Vector<Int>,
        snd: Vector<Int>,
    ): Vector<Int> = when {
        fst is IntVector && snd is IntVector ->
            fst.lanewise(operator, snd)

        else -> error("invalid value")
    }

    override fun ternary(
        operator: Ternary,
        fst: Vector<Int>,
        snd: Vector<Int>,
        trd: Vector<Int>,
    ): Vector<Int> = when {
        fst is IntVector && snd is IntVector && trd is IntVector ->
            fst.lanewise(operator, snd, trd)

        else -> error("invalid value")
    }

    override fun test(
        operator: Test,
        fst: Vector<Int>,
    ): VectorMask<Int> = when {
        fst is IntVector ->
            fst.test(operator)

        else -> error("invalid value")
    }

    override fun compare(
        operator: Comparison,
        fst: Vector<Int>,
        snd: Vector<Int>,
    ): VectorMask<Int> = when {
        fst is IntVector && snd is IntVector ->
            fst.compare(operator, snd)

        else -> error("invalid value")
    }

    override fun select(
        mask: VectorMask<Int>,
        fst: Vector<Int>,
        snd: Vector<Int>,
    ): Vector<Int> = when {
        fst is IntVector && snd is IntVector ->
            snd.blend(fst, mask)

        else -> error("invalid value")
    }
}
