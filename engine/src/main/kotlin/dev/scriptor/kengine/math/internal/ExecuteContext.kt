package dev.scriptor.kengine.math.internal

import jdk.incubator.vector.Vector
import jdk.incubator.vector.VectorMask

interface ExecuteContext<E : Number, A : Any> {
    val vectors: Array<Vector<E>?>
    val masks: Array<VectorMask<E>?>
    val output: ValueOrArray<A>
}
