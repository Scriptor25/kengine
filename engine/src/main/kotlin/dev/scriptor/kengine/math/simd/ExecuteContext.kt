package dev.scriptor.kengine.math.simd

import jdk.incubator.vector.Vector
import jdk.incubator.vector.VectorMask

interface ExecuteContext<E : Number, A : Any> {
    val adapter: VectorAdapter<E, A>
    val vectors: Array<Vector<E>?>
    val masks: Array<VectorMask<E>?>
    val outputs: Array<out A>
}
