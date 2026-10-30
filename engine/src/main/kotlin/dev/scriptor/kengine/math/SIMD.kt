package dev.scriptor.kengine.math

import jdk.incubator.vector.*
import jdk.incubator.vector.Vector
import jdk.incubator.vector.VectorOperators.*
import java.util.*

private interface VectorAdapter<E : Number, A : Any, V : Vector<E>> {
    val species: VectorSpecies<E>

    fun allocate(size: Int): A
    fun sizeof(array: A): Int

    fun load(element: E): V
    fun load(array: A, offset: Int, mask: VectorMask<E>): V

    fun store(array: A, offset: Int, mask: VectorMask<E>, vector: V)

    fun fn(operator: Unary, fst: V): V
    fun fn(operator: Binary, fst: V, snd: V): V
    fun fn(operator: Associative, fst: V, snd: V): V
    fun fn(operator: Ternary, fst: V, snd: V, trd: V): V
    fun fn(operator: Test, fst: V): VectorMask<E>
    fun fn(operator: Comparison, fst: V, snd: V): VectorMask<E>
}

private object FloatVectorAdapter : VectorAdapter<Float, FloatArray, FloatVector> {
    override val species: VectorSpecies<Float> = FloatVector.SPECIES_PREFERRED

    override fun allocate(
        size: Int,
    ): FloatArray = FloatArray(size)

    override fun sizeof(
        array: FloatArray,
    ): Int = array.size

    override fun load(
        element: Float,
    ): FloatVector = FloatVector.broadcast(species, element)

    override fun load(
        array: FloatArray,
        offset: Int,
        mask: VectorMask<Float>,
    ): FloatVector = FloatVector.fromArray(species, array, offset, mask)

    override fun store(
        array: FloatArray,
        offset: Int,
        mask: VectorMask<Float>,
        vector: FloatVector,
    ) = vector.intoArray(array, offset, mask)

    override fun fn(
        operator: Unary,
        fst: FloatVector,
    ): FloatVector = fst.lanewise(operator)

    override fun fn(
        operator: Binary,
        fst: FloatVector,
        snd: FloatVector,
    ): FloatVector = fst.lanewise(operator, snd)

    override fun fn(
        operator: Associative,
        fst: FloatVector,
        snd: FloatVector,
    ): FloatVector = fst.lanewise(operator, snd)

    override fun fn(
        operator: Ternary,
        fst: FloatVector,
        snd: FloatVector,
        trd: FloatVector,
    ): FloatVector = fst.lanewise(operator, snd, trd)

    override fun fn(
        operator: Test,
        fst: FloatVector,
    ): VectorMask<Float> = fst.test(operator)

    override fun fn(
        operator: Comparison,
        fst: FloatVector,
        snd: FloatVector,
    ): VectorMask<Float> = fst.compare(operator, snd)
}

private object IntVectorAdapter : VectorAdapter<Int, IntArray, IntVector> {
    override val species: VectorSpecies<Int> = IntVector.SPECIES_PREFERRED

    override fun allocate(
        size: Int,
    ): IntArray = IntArray(size)

    override fun sizeof(
        array: IntArray,
    ): Int = array.size

    override fun load(
        element: Int,
    ): IntVector = IntVector.broadcast(species, element)

    override fun load(
        array: IntArray,
        offset: Int,
        mask: VectorMask<Int>,
    ): IntVector = IntVector.fromArray(species, array, offset, mask)

    override fun store(
        array: IntArray,
        offset: Int,
        mask: VectorMask<Int>,
        vector: IntVector,
    ) = vector.intoArray(array, offset, mask)

    override fun fn(
        operator: Unary,
        fst: IntVector,
    ): IntVector = fst.lanewise(operator)

    override fun fn(
        operator: Binary,
        fst: IntVector,
        snd: IntVector,
    ): IntVector = fst.lanewise(operator, snd)

    override fun fn(
        operator: Associative,
        fst: IntVector,
        snd: IntVector,
    ): IntVector = fst.lanewise(operator, snd)

    override fun fn(
        operator: Ternary,
        fst: IntVector,
        snd: IntVector,
        trd: IntVector,
    ): IntVector = fst.lanewise(operator, snd, trd)

    override fun fn(
        operator: Test,
        fst: IntVector,
    ): VectorMask<Int> = fst.test(operator)

    override fun fn(
        operator: Comparison,
        fst: IntVector,
        snd: IntVector,
    ): VectorMask<Int> = fst.compare(operator, snd)
}

sealed interface SIMDNode<E>

private class ScalarNode<E>(
    val value: E,
) : SIMDNode<E>

private class ArrayNode<E, A>(
    val value: A,
) : SIMDNode<E>

private class UnaryNode<E>(
    val operator: Unary,
    val fst: SIMDNode<E>,
) : SIMDNode<E>

private class BinaryNode<E>(
    val operator: Binary,
    val fst: SIMDNode<E>,
    val snd: SIMDNode<E>,
) : SIMDNode<E>

private class AssociativeNode<E>(
    val operator: Associative,
    val fst: SIMDNode<E>,
    val snd: SIMDNode<E>,
) : SIMDNode<E>

private class TernaryNode<E>(
    val operator: Ternary,
    val fst: SIMDNode<E>,
    val snd: SIMDNode<E>,
    val trd: SIMDNode<E>,
) : SIMDNode<E>

private class TestNode<E>(
    val operator: Test,
    val fst: SIMDNode<E>,
) : SIMDNode<E>

private class ComparisonNode<E>(
    val operator: Comparison,
    val fst: SIMDNode<E>,
    val snd: SIMDNode<E>,
) : SIMDNode<E>

class SIMD<E : Number, A : Any> {

    fun E.asNode(): SIMDNode<E> = ScalarNode(this)

    fun A.asNode(): SIMDNode<E> = ArrayNode(this)

    private fun <E> create(operator: Unary, fst: SIMDNode<E>): UnaryNode<E> {
        return UnaryNode(operator, fst)
    }

    private fun <E> create(operator: Binary, fst: SIMDNode<E>, snd: SIMDNode<E>): BinaryNode<E> {
        return BinaryNode(operator, fst, snd)
    }

    private fun <E> create(operator: Associative, fst: SIMDNode<E>, snd: SIMDNode<E>): AssociativeNode<E> {
        return AssociativeNode(operator, fst, snd)
    }

    private fun <E> create(operator: Ternary, fst: SIMDNode<E>, snd: SIMDNode<E>, trd: SIMDNode<E>): TernaryNode<E> {
        return TernaryNode(operator, fst, snd, trd)
    }

    private fun <E> create(operator: Test, fst: SIMDNode<E>): TestNode<E> {
        return TestNode(operator, fst)
    }

    private fun <E> create(operator: Comparison, fst: SIMDNode<E>, snd: SIMDNode<E>): ComparisonNode<E> {
        return ComparisonNode(operator, fst, snd)
    }

    fun <E> SIMDNode<E>.abs(): SIMDNode<E> {
        return create(ABS, this)
    }

    operator fun <E> SIMDNode<E>.unaryMinus(): SIMDNode<E> {
        return create(NEG, this)
    }

    fun <E> sin(fst: SIMDNode<E>): SIMDNode<E> {
        return create(SIN, fst)
    }

    fun <E> cos(fst: SIMDNode<E>): SIMDNode<E> {
        return create(COS, fst)
    }

    fun <E> tan(fst: SIMDNode<E>): SIMDNode<E> {
        return create(TAN, fst)
    }

    fun <E> asin(fst: SIMDNode<E>): SIMDNode<E> {
        return create(ASIN, fst)
    }

    fun <E> acos(fst: SIMDNode<E>): SIMDNode<E> {
        return create(ACOS, fst)
    }

    fun <E> atan(fst: SIMDNode<E>): SIMDNode<E> {
        return create(ATAN, fst)
    }

    fun <E> exp(fst: SIMDNode<E>): SIMDNode<E> {
        return create(EXP, fst)
    }

    fun <E> log(fst: SIMDNode<E>): SIMDNode<E> {
        return create(LOG, fst)
    }

    fun <E> log10(fst: SIMDNode<E>): SIMDNode<E> {
        return create(LOG10, fst)
    }

    fun <E> sqrt(fst: SIMDNode<E>): SIMDNode<E> {
        return create(SQRT, fst)
    }

    fun <E> cbrt(fst: SIMDNode<E>): SIMDNode<E> {
        return create(CBRT, fst)
    }

    fun <E> sinh(fst: SIMDNode<E>): SIMDNode<E> {
        return create(SINH, fst)
    }

    fun <E> cosh(fst: SIMDNode<E>): SIMDNode<E> {
        return create(COSH, fst)
    }

    fun <E> tanh(fst: SIMDNode<E>): SIMDNode<E> {
        return create(TANH, fst)
    }

    fun <E> expm1(fst: SIMDNode<E>): SIMDNode<E> {
        return create(EXPM1, fst)
    }

    fun <E> log1p(fst: SIMDNode<E>): SIMDNode<E> {
        return create(LOG1P, fst)
    }

    operator fun <E> SIMDNode<E>.plus(snd: SIMDNode<E>): SIMDNode<E> {
        return create(ADD, this, snd)
    }

    operator fun <E> SIMDNode<E>.minus(snd: SIMDNode<E>): SIMDNode<E> {
        return create(SUB, this, snd)
    }

    operator fun <E> SIMDNode<E>.times(snd: SIMDNode<E>): SIMDNode<E> {
        return create(MUL, this, snd)
    }

    operator fun <E> SIMDNode<E>.div(snd: SIMDNode<E>): SIMDNode<E> {
        return create(DIV, this, snd)
    }

    fun <E> min(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(MIN, fst, snd)
    }

    fun <E> max(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(MAX, fst, snd)
    }

    fun <E> firstNonzero(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(FIRST_NONZERO, fst, snd)
    }

    fun <E> atan2(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(ATAN2, fst, snd)
    }

    fun <E> pow(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(POW, fst, snd)
    }

    fun <E> hypot(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(HYPOT, fst, snd)
    }

    fun <E> bitwiseBlend(fst: SIMDNode<E>, snd: SIMDNode<E>, trd: SIMDNode<E>): SIMDNode<E> {
        return create(BITWISE_BLEND, fst, snd, trd)
    }

    fun <E> fma(fst: SIMDNode<E>, snd: SIMDNode<E>, trd: SIMDNode<E>): SIMDNode<E> {
        return create(FMA, fst, snd, trd)
    }

    fun <E> SIMDNode<E>.isDefault(): SIMDNode<E> {
        return create(IS_DEFAULT, this)
    }

    fun <E> SIMDNode<E>.isNegative(): SIMDNode<E> {
        return create(IS_NEGATIVE, this)
    }

    fun <E> SIMDNode<E>.isFinite(): SIMDNode<E> {
        return create(IS_FINITE, this)
    }

    fun <E> SIMDNode<E>.isNaN(): SIMDNode<E> {
        return create(IS_NAN, this)
    }

    fun <E> SIMDNode<E>.isInfinite(): SIMDNode<E> {
        return create(IS_INFINITE, this)
    }

    fun <E> compareEQ(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(EQ, fst, snd)
    }

    fun <E> compareNE(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(NE, fst, snd)
    }

    fun <E> compareLT(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(LT, fst, snd)
    }

    fun <E> compareLE(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(LE, fst, snd)
    }

    fun <E> compareGT(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(GT, fst, snd)
    }

    fun <E> compareGE(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(GE, fst, snd)
    }

    fun <E> compareULT(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(ULT, fst, snd)
    }

    fun <E> compareULE(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(ULE, fst, snd)
    }

    fun <E> compareUGT(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(UGT, fst, snd)
    }

    fun <E> compareUGE(fst: SIMDNode<E>, snd: SIMDNode<E>): SIMDNode<E> {
        return create(UGE, fst, snd)
    }
}

@JvmInline
private value class SIMDRegister(
    val id: Int,
)

private sealed interface SIMDInstruction {
    val register: SIMDRegister
}

private data class SIMDProgram<E : Number, A : Any, V : Vector<E>>(
    val instructions: List<SIMDInstruction>,
    val result: SIMDRegister,
    val count: Int,
) {
    fun getSize(adapter: VectorAdapter<E, A, V>): Int {
        val sizes = instructions.mapNotNull {
            when (it) {
                is LoadArrayInstruction<*> -> adapter.sizeof(it.value as A)
                else -> null
            }
        }

        require(sizes.distinct().size <= 1) {
            "all SIMD input values must have the same length"
        }

        return sizes.firstOrNull() ?: 1
    }
}

private class LoadScalarInstruction<E : Number>(
    override val register: SIMDRegister,
    val value: E,
) : SIMDInstruction

private class LoadArrayInstruction<A : Any>(
    override val register: SIMDRegister,
    val value: A,
) : SIMDInstruction

private class UnaryInstruction(
    override val register: SIMDRegister,
    val operator: Unary,
    val fst: SIMDRegister,
) : SIMDInstruction

private class BinaryInstruction(
    override val register: SIMDRegister,
    val operator: Binary,
    val fst: SIMDRegister,
    val snd: SIMDRegister,
) : SIMDInstruction

private class AssociativeInstruction(
    override val register: SIMDRegister,
    val operator: Associative,
    val fst: SIMDRegister,
    val snd: SIMDRegister,
) : SIMDInstruction

private class TernaryInstruction(
    override val register: SIMDRegister,
    val operator: Ternary,
    val fst: SIMDRegister,
    val snd: SIMDRegister,
    val trd: SIMDRegister,
) : SIMDInstruction

private class TestInstruction(
    override val register: SIMDRegister,
    val operator: Test,
    val fst: SIMDRegister,
) : SIMDInstruction

private class ComparisonInstruction(
    override val register: SIMDRegister,
    val operator: Comparison,
    val fst: SIMDRegister,
    val snd: SIMDRegister,
) : SIMDInstruction

private class SIMDCompile<E : Number, A : Any, V : Vector<E>> {

    private var next = 0
    private val registers = IdentityHashMap<SIMDNode<*>, SIMDRegister>()
    private val instructions = mutableListOf<SIMDInstruction>()

    private fun allocate(): SIMDRegister = SIMDRegister(next++)

    operator fun invoke(root: SIMDNode<*>): SIMDProgram<E, A, V> {
        next = 0
        registers.clear()
        instructions.clear()

        val result = visit(root)

        return SIMDProgram(
            instructions.toList(),
            result,
            next,
        )
    }

    private fun visit(node: SIMDNode<*>): SIMDRegister {
        registers[node]?.let { return it }

        val register = allocate()

        instructions += when (node) {
            is ScalarNode<*> -> when (node.value) {
                is Float -> LoadScalarInstruction(register, node.value)
                else -> error("unsupported scalar value")
            }

            is ArrayNode<*, *> -> when (node.value) {
                is FloatArray -> LoadArrayInstruction(register, node.value)
                else -> error("unsupported array value")
            }

            is UnaryNode<*> -> {
                val fst = visit(node.fst)

                UnaryInstruction(
                    register,
                    node.operator,
                    fst,
                )
            }

            is BinaryNode<*> -> {
                val fst = visit(node.fst)
                val snd = visit(node.snd)

                BinaryInstruction(
                    register,
                    node.operator,
                    fst,
                    snd,
                )
            }

            is AssociativeNode<*> -> {
                val fst = visit(node.fst)
                val snd = visit(node.snd)

                AssociativeInstruction(
                    register,
                    node.operator,
                    fst,
                    snd,
                )
            }

            is TernaryNode<*> -> {
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

            is TestNode<*> -> {
                val fst = visit(node.fst)

                TestInstruction(
                    register,
                    node.operator,
                    fst,
                )
            }

            is ComparisonNode<*> -> {
                val fst = visit(node.fst)
                val snd = visit(node.snd)

                ComparisonInstruction(
                    register,
                    node.operator,
                    fst,
                    snd,
                )
            }
        }

        registers[node] = register
        return register
    }
}

private class SIMDExecute<E : Number, A : Any, V : Vector<E>>(
    val program: SIMDProgram<E, A, V>,
    val adapter: VectorAdapter<E, A, V>,
) {
    operator fun invoke(): A {
        val size = program.getSize(adapter)

        val registers = arrayOfNulls<Any>(program.count)
        val output = adapter.allocate(size)

        for (offset in 0 until size step adapter.species.length()) {
            val mask: VectorMask<E> = adapter.species.indexInRange(offset, size)

            for (instruction in program.instructions) {
                registers[instruction.register.id] = when (instruction) {
                    is LoadScalarInstruction<*> ->
                        adapter.load(instruction.value as E)

                    is LoadArrayInstruction<*> ->
                        adapter.load(instruction.value as A, offset, mask)

                    is UnaryInstruction -> {
                        val fst = registers[instruction.fst.id]

                        when {
                            fst is Vector<*> ->
                                adapter.fn(instruction.operator, fst as V)

                            else -> error("unsupported operand")
                        }
                    }

                    is BinaryInstruction -> {
                        val fst = registers[instruction.fst.id]
                        val snd = registers[instruction.snd.id]

                        when {
                            fst is Vector<*> && snd is Vector<*> ->
                                adapter.fn(instruction.operator, fst as V, snd as V)

                            else -> error("unsupported operand")
                        }
                    }

                    is AssociativeInstruction -> {
                        val fst = registers[instruction.fst.id]
                        val snd = registers[instruction.snd.id]

                        when {
                            fst is Vector<*> && snd is Vector<*> ->
                                adapter.fn(instruction.operator, fst as V, snd as V)

                            else -> error("unsupported operand")
                        }
                    }

                    is TernaryInstruction -> {
                        val fst = registers[instruction.fst.id]
                        val snd = registers[instruction.snd.id]
                        val trd = registers[instruction.trd.id]

                        when {
                            fst is Vector<*> && snd is Vector<*> && trd is Vector<*> ->
                                adapter.fn(instruction.operator, fst as V, snd as V, trd as V)

                            else -> error("unsupported operand")
                        }
                    }

                    is TestInstruction -> {
                        val fst = registers[instruction.fst.id]

                        when {
                            fst is Vector<*> ->
                                adapter.fn(instruction.operator, fst as V)

                            else -> error("unsupported operand")
                        }
                    }

                    is ComparisonInstruction -> {
                        val fst = registers[instruction.fst.id]
                        val snd = registers[instruction.snd.id]

                        when {
                            fst is Vector<*> && snd is Vector<*> ->
                                adapter.fn(instruction.operator, fst as V, snd as V)

                            else -> error("unsupported operand")
                        }
                    }
                }
            }

            when (val result = registers[program.result.id]) {
                is Vector<*> ->
                    adapter.store(output, offset, mask, result as V)

                else -> error("unsupported operand")
            }
        }

        return output
    }
}

private fun <E : Number, A : Any, V : Vector<E>> simd(
    adapter: VectorAdapter<E, A, V>,
    node: SIMDNode<E>,
): A {
    val compile = SIMDCompile<E, A, V>()
    val program = compile(node)
    val execute = SIMDExecute(program, adapter)
    return execute()
}

private fun <E : Number, A : Any, V : Vector<E>> simd(
    adapter: VectorAdapter<E, A, V>,
    block: SIMD<E, A>.() -> SIMDNode<E>,
): A {
    return simd(
        adapter,
        SIMD<E, A>().run(block),
    )
}

fun simdF(block: SIMD<Float, FloatArray>.() -> SIMDNode<Float>): FloatArray {
    return simd(FloatVectorAdapter, block)
}

fun simdI(block: SIMD<Int, IntArray>.() -> SIMDNode<Int>): IntArray {
    return simd(IntVectorAdapter, block)
}
