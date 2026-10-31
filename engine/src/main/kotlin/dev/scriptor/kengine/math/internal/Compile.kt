package dev.scriptor.kengine.math.internal

import java.util.*

class Compile<E : Number, A : Any> {

    private var next = 0
    private val registers = IdentityHashMap<Any, Register>()
    private val instructions = mutableListOf<Instruction<E, A>>()

    private fun allocate(): Register = Register(next++)

    operator fun invoke(node: Node<E, A>): Program<E, A> {
        next = 0
        registers.clear()
        instructions.clear()

        val result = visit(node)

        val allocation = SIMDRegisterAllocator(
            instructions,
            intArrayOf(result.id),
            next,
        ).allocate()

        return Program(
            instructions.map { it.rewrite(allocation.mapping) },
            ValueOrArray.WithValue(Register(allocation.mapping[result.id])),
            allocation.vectorCount,
            allocation.maskCount,
        )
    }

    operator fun invoke(exports: List<Node<E, A>>): Program<E, A> {
        next = 0
        registers.clear()
        instructions.clear()

        val results = exports
            .map(::visit)
            .toTypedArray()

        val allocation = SIMDRegisterAllocator(
            instructions,
            results.map { it.id }.toIntArray(),
            next,
        ).allocate()

        return Program(
            instructions.map { it.rewrite(allocation.mapping) },
            ValueOrArray.WithArray(results.map { Register(allocation.mapping[it.id]) }.toTypedArray()),
            allocation.vectorCount,
            allocation.maskCount,
        )
    }

    private fun visit(node: Node<E, A>): Register {
        registers[node]?.let { return it }

        val register = allocate()

        instructions += when (node) {
            is ArgumentNode -> LoadArgumentInstruction(register, node.index)

            is ScalarNode -> LoadScalarInstruction(register, node.value)

            is ArrayNode -> LoadArrayInstruction(register, node.value)

            is UnaryNode -> {
                val fst = visit(node.fst)

                UnaryInstruction(
                    register,
                    node.operator,
                    fst,
                )
            }

            is BinaryNode -> {
                val fst = visit(node.fst)
                val snd = visit(node.snd)

                BinaryInstruction(
                    register,
                    node.operator,
                    fst,
                    snd,
                )
            }

            is AssociativeNode -> {
                val fst = visit(node.fst)
                val snd = visit(node.snd)

                AssociativeInstruction(
                    register,
                    node.operator,
                    fst,
                    snd,
                )
            }

            is TernaryNode -> {
                val fst = visit(node.fst)
                val snd = visit(node.snd)
                val trd = visit(node.trd)

                TernaryInstruction(
                    register,
                    node.operator,
                    fst,
                    snd,
                    trd,
                )
            }

            is SelectNode -> {
                val mask = visit(node.mask)
                val fst = visit(node.fst)
                val snd = visit(node.snd)

                SelectInstruction(
                    register,
                    mask,
                    fst,
                    snd,
                )
            }
        }

        registers[node] = register
        return register
    }

    private fun visit(mask: Mask<E, A>): Register {
        registers[mask]?.let { return it }

        val register = allocate()

        instructions += when (mask) {
            is TestMask -> {
                val fst = visit(mask.fst)

                TestInstruction(
                    register,
                    mask.operator,
                    fst,
                )
            }

            is ComparisonMask -> {
                val fst = visit(mask.fst)
                val snd = visit(mask.snd)

                ComparisonInstruction(
                    register,
                    mask.operator,
                    fst,
                    snd,
                )
            }

            is AndMask -> {
                val fst = visit(mask.fst)
                val snd = visit(mask.snd)

                AndInstruction(
                    register,
                    fst,
                    snd,
                )
            }

            is OrMask -> {
                val fst = visit(mask.fst)
                val snd = visit(mask.snd)

                OrInstruction(
                    register,
                    fst,
                    snd,
                )
            }
        }

        registers[mask] = register
        return register
    }
}
