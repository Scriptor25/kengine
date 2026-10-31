package dev.scriptor.kengine.math.internal

import jdk.incubator.vector.VectorOperators.Comparison
import jdk.incubator.vector.VectorOperators.Test

sealed interface Mask<E : Number, A : Any>

class TestMask<E : Number, A : Any>(
    val operator: Test,
    val fst: Node<E, A>,
) : Mask<E, A>

class ComparisonMask<E : Number, A : Any>(
    val operator: Comparison,
    val fst: Node<E, A>,
    val snd: Node<E, A>,
) : Mask<E, A>
