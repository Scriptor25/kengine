package dev.scriptor.kengine.math.simd

import jdk.incubator.vector.VectorMask

inline fun <reified E : Number, reified A : Any> execute(
    program: Program<E, A>,
    adapter: VectorAdapter<E, A>,
    context: ExecuteContext<E, A>,
    arguments: Array<out Any>,
) {
    // TODO: allow mixed element type vectors

    val size = program.getSize(adapter, arguments)

    val vectors = context.vectors
    val masks = context.masks
    val output = context.output

    require(output.size == program.result.size)

    var i = 0
    while (i < output.size)
        require(adapter.sizeof(output[i++]) == size)

    val length = minOf(
        adapter.species.length()
    )

    for (offset in 0 until size step length) {
        val lanes = minOf(length, size - offset)

        val mask: VectorMask<E> = adapter.species.indexInRange(offset, size)

        var i = 0
        while (i < program.instructions.size) {
            val instruction = program.instructions[i++]
            val ptr = instruction.register.id
            when (instruction) {
                is LoadArgumentInstruction ->
                    vectors[ptr] = when (val argument = arguments[instruction.index]) {
                        is A -> adapter.load(argument, offset, mask)
                        is E -> adapter.load(argument)
                        else -> error("invalid argument")
                    }

                is LoadScalarInstruction ->
                    vectors[ptr] = adapter.load(instruction.value)

                is LoadArrayInstruction ->
                    vectors[ptr] = adapter.load(instruction.value, offset, mask)

                is UnaryInstruction -> {
                    val fst = vectors[instruction.fst.id]!!

                    vectors[ptr] = fst.lanewise(instruction.operator)
                }

                is BinaryInstruction -> {
                    val fst = vectors[instruction.fst.id]!!
                    val snd = vectors[instruction.snd.id]!!

                    vectors[ptr] = fst.lanewise(instruction.operator, snd)
                }

                is AssociativeInstruction -> {
                    val fst = vectors[instruction.fst.id]!!
                    val snd = vectors[instruction.snd.id]!!

                    vectors[ptr] = fst.lanewise(instruction.operator, snd)
                }

                is TernaryInstruction -> {
                    val fst = vectors[instruction.fst.id]!!
                    val snd = vectors[instruction.snd.id]!!
                    val trd = vectors[instruction.trd.id]!!

                    vectors[ptr] = fst.lanewise(instruction.operator, snd, trd)
                }

                is SelectInstruction -> {
                    val mask = masks[instruction.mask.id]!!
                    val fst = vectors[instruction.fst.id]!!
                    val snd = vectors[instruction.snd.id]!!

                    vectors[ptr] = snd.blend(fst, mask)
                }

                is TestInstruction -> {
                    val fst = vectors[instruction.fst.id]!!

                    masks[ptr] = fst.test(instruction.operator)
                }

                is ComparisonInstruction -> {
                    val fst = vectors[instruction.fst.id]!!
                    val snd = vectors[instruction.snd.id]!!

                    masks[ptr] = fst.compare(instruction.operator, snd)
                }

                is AndInstruction -> {
                    val fst = masks[instruction.fst.id]!!
                    val snd = masks[instruction.snd.id]!!

                    masks[ptr] = fst.and(snd)
                }

                is OrInstruction -> {
                    val fst = masks[instruction.fst.id]!!
                    val snd = masks[instruction.snd.id]!!

                    masks[ptr] = fst.or(snd)
                }
            }
        }

        var index = 0
        while (index < program.result.size) {
            adapter.store(
                output[index],
                offset,
                mask,
                vectors[program.result[index].id]!!,
            )
            index++
        }
    }
}
