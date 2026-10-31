package dev.scriptor.kengine.math.internal

import jdk.incubator.vector.VectorMask

fun <E : Number, A : Any> execute(
    program: Program<E, A>,
    adapter: VectorAdapter<E, A>,
    context: ExecuteContext<E, A>,
    arguments: Array<out Any>,
) {
    val size = program.getSize(adapter, arguments)

    val vectors = context.vectors
    val masks = context.masks
    val output = context.output

    when (output) {
        is ValueOrArray.WithValue -> {
            require(program.result is ValueOrArray.WithValue)
            require(adapter.sizeof(output.value) == size)
        }

        is ValueOrArray.WithArray -> {
            require(program.result is ValueOrArray.WithArray)
            require(output.value.size == program.result.value.size)

            var i = 0
            while (i < output.value.size)
                require(adapter.sizeof(output.value[i++]) == size)
        }
    }

    for (offset in 0 until size step adapter.species.length()) {
        val mask: VectorMask<E> = adapter.species.indexInRange(offset, size)

        var i = 0
        while (i < program.instructions.size) {
            val instruction = program.instructions[i++]

            when (instruction) {
                is LoadArgumentInstruction ->
                    vectors[instruction.register.id] = when (val argument = arguments[instruction.index]) {
                        is Number ->
                            adapter.load(argument as E)

                        is BooleanArray,
                        is CharArray,
                        is ByteArray,
                        is ShortArray,
                        is IntArray,
                        is LongArray,
                        is FloatArray,
                        is DoubleArray,
                        is Array<*> ->
                            adapter.load(argument as A, offset, mask)

                        else -> error("unsupported operand")
                    }

                is LoadScalarInstruction ->
                    vectors[instruction.register.id] = adapter.load(instruction.value)

                is LoadArrayInstruction ->
                    vectors[instruction.register.id] = adapter.load(instruction.value, offset, mask)

                is UnaryInstruction -> {
                    val fst = vectors[instruction.fst.id]!!

                    vectors[instruction.register.id] = adapter.unary(instruction.operator, fst)
                }

                is BinaryInstruction -> {
                    val fst = vectors[instruction.fst.id]!!
                    val snd = vectors[instruction.snd.id]!!

                    vectors[instruction.register.id] = adapter.binary(instruction.operator, fst, snd)
                }

                is AssociativeInstruction -> {
                    val fst = vectors[instruction.fst.id]!!
                    val snd = vectors[instruction.snd.id]!!

                    vectors[instruction.register.id] = adapter.associative(instruction.operator, fst, snd)
                }

                is TernaryInstruction -> {
                    val fst = vectors[instruction.fst.id]!!
                    val snd = vectors[instruction.snd.id]!!
                    val trd = vectors[instruction.trd.id]!!

                    vectors[instruction.register.id] = adapter.ternary(instruction.operator, fst, snd, trd)
                }

                is SelectInstruction -> {
                    val mask = masks[instruction.mask.id]!!
                    val fst = vectors[instruction.fst.id]!!
                    val snd = vectors[instruction.snd.id]!!

                    vectors[instruction.register.id] = adapter.select(mask, fst, snd)
                }

                is TestInstruction -> {
                    val fst = vectors[instruction.fst.id]!!

                    masks[instruction.register.id] = adapter.test(instruction.operator, fst)
                }

                is ComparisonInstruction -> {
                    val fst = vectors[instruction.fst.id]!!
                    val snd = vectors[instruction.snd.id]!!

                    masks[instruction.register.id] = adapter.compare(instruction.operator, fst, snd)
                }

                is AndInstruction -> {
                    val fst = masks[instruction.fst.id]!!
                    val snd = masks[instruction.snd.id]!!

                    masks[instruction.register.id] = adapter.and(fst, snd)
                }

                is OrInstruction -> {
                    val fst = masks[instruction.fst.id]!!
                    val snd = masks[instruction.snd.id]!!

                    masks[instruction.register.id] = adapter.or(fst, snd)
                }
            }
        }

        when {
            program.result is ValueOrArray.WithValue
                    && output is ValueOrArray.WithValue -> {
                adapter.store(
                    output.value,
                    offset,
                    mask,
                    vectors[program.result.value.id]!!,
                )
            }

            program.result is ValueOrArray.WithArray
                    && output is ValueOrArray.WithArray -> {
                var i = 0
                while (i < output.value.size) {
                    adapter.store(
                        output.value[i],
                        offset,
                        mask,
                        vectors[program.result.value[i].id]!!,
                    )

                    i++
                }
            }

            else -> error("dead code")
        }
    }
}
