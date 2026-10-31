package dev.scriptor.kengine.math.internal

import jdk.incubator.vector.Vector
import jdk.incubator.vector.VectorMask

class Program<E : Number, A : Any>(
    val instructions: List<Instruction<E, A>>,
    val result: ValueOrArray<Register>,
    val vectorCount: Int,
    val maskCount: Int,
)

fun <E : Number, A : Any> Program<E, A>.getSize(
    adapter: VectorAdapter<E, A>,
    arguments: Array<out Any>,
): Int {
    var size = 0

    var i = 0
    while (i < instructions.size) {
        val s = when (val instruction = instructions[i++]) {
            is LoadArgumentInstruction -> when (val value = arguments[instruction.index]) {
                is BooleanArray,
                is CharArray,
                is ByteArray,
                is ShortArray,
                is IntArray,
                is LongArray,
                is FloatArray,
                is DoubleArray,
                is Array<*> -> adapter.sizeof(value as A)

                else -> null
            }

            is LoadArrayInstruction ->
                adapter.sizeof(instruction.value)

            else -> null
        } ?: continue

        require(size == 0 || size == s) {
            "all SIMD input values must have the same length"
        }

        if (size == 0)
            size = s
    }

    return if (size == 0) 1 else size
}

fun <E : Number, A : Any> Program<E, A>.allocate(output: ValueOrArray<A>): ExecuteContext<E, A> {
    val vectors = arrayOfNulls<Vector<E>>(vectorCount)
    val masks = arrayOfNulls<VectorMask<E>>(maskCount)

    return object : ExecuteContext<E, A> {
        override val vectors: Array<Vector<E>?> = vectors
        override val masks: Array<VectorMask<E>?> = masks
        override val output: ValueOrArray<A> = output
    }
}

operator fun Program<Float, FloatArray>.invoke(
    vararg arguments: Any,
    context: ExecuteContext<Float, FloatArray>,
) {
    execute(
        this,
        FloatVectorAdapter,
        context,
        arguments,
    )
}

operator fun Program<Int, IntArray>.invoke(
    vararg arguments: Any,
    context: ExecuteContext<Int, IntArray>,
) {
    execute(
        this,
        IntVectorAdapter,
        context,
        arguments,
    )
}
