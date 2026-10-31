package dev.scriptor.kengine.math.simd

import jdk.incubator.vector.Vector
import jdk.incubator.vector.VectorMask

class Program<E : Number, A : Any>(
    val instructions: List<Instruction<E, A>>,
    val result: ValueOrArray<Register>,
    val vectorCount: Int,
    val maskCount: Int,
)

inline fun <reified E : Number, reified A : Any> Program<E, A>.getSize(
    adapter: VectorAdapter<E, A>,
    arguments: Array<out Any>,
): Int {
    var size = 0

    var i = 0
    while (i < instructions.size) {
        val s = when (val instruction = instructions[i++]) {
            is LoadArgumentInstruction ->
                when (val argument = arguments[instruction.index]) {
                    is A -> adapter.sizeof(argument)
                    is E -> null
                    else -> error("invalid argument")
                }

            is LoadArrayInstruction ->
                adapter.sizeof(instruction.value)

            else -> null
        } ?: continue

        if (size == 0) size = s
        else require(size == s) {
            "all SIMD input values must have the same length"
        }
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
