package dev.scriptor.kengine.math

import jdk.incubator.vector.*
import jdk.incubator.vector.Vector
import jdk.incubator.vector.VectorOperators.*
import java.util.*

private interface VectorAdapter<E : Number, A : Any> {
    val species: VectorSpecies<E>

    fun allocate(size: Int): A
    fun sizeof(array: A): Int

    fun load(element: E): Any
    fun load(array: A, offset: Int, mask: VectorMask<E>): Any

    fun store(array: A, offset: Int, mask: VectorMask<E>, vector: Any)

    fun fn(operator: Unary, fst: Any): Any
    fun fn(operator: Binary, fst: Any, snd: Any): Any
    fun fn(operator: Associative, fst: Any, snd: Any): Any
    fun fn(operator: Ternary, fst: Any, snd: Any, trd: Any): Any
    fun fn(operator: Test, fst: Any): VectorMask<E>
    fun fn(operator: Comparison, fst: Any, snd: Any): VectorMask<E>
}

private object FloatVectorAdapter : VectorAdapter<Float, FloatArray> {
    override val species: VectorSpecies<Float> = FloatVector.SPECIES_PREFERRED

    override fun allocate(
        size: Int,
    ): FloatArray = FloatArray(size)

    override fun sizeof(
        array: FloatArray,
    ): Int = array.size

    override fun load(
        element: Float,
    ): Any = FloatVector.broadcast(species, element)

    override fun load(
        array: FloatArray,
        offset: Int,
        mask: VectorMask<Float>,
    ): Any = FloatVector.fromArray(species, array, offset, mask)

    override fun store(
        array: FloatArray,
        offset: Int,
        mask: VectorMask<Float>,
        vector: Any,
    ) = when {
        vector is FloatVector ->
            vector.intoArray(array, offset, mask)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Unary,
        fst: Any,
    ): Any = when {
        fst is FloatVector ->
            fst.lanewise(operator)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Binary,
        fst: Any,
        snd: Any,
    ): Any = when {
        fst is FloatVector && snd is FloatVector ->
            fst.lanewise(operator, snd)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Associative,
        fst: Any,
        snd: Any,
    ): Any = when {
        fst is FloatVector && snd is FloatVector ->
            fst.lanewise(operator, snd)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Ternary,
        fst: Any,
        snd: Any,
        trd: Any,
    ): Any = when {
        fst is FloatVector && snd is FloatVector && trd is FloatVector ->
            fst.lanewise(operator, snd, trd)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Test,
        fst: Any,
    ): VectorMask<Float> = when {
        fst is FloatVector ->
            fst.test(operator)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Comparison,
        fst: Any,
        snd: Any,
    ): VectorMask<Float> = when {
        fst is FloatVector && snd is FloatVector ->
            fst.compare(operator, snd)

        else -> error("invalid value")
    }
}

private object IntVectorAdapter : VectorAdapter<Int, IntArray> {
    override val species: VectorSpecies<Int> = IntVector.SPECIES_PREFERRED

    override fun allocate(
        size: Int,
    ): IntArray = IntArray(size)

    override fun sizeof(
        array: IntArray,
    ): Int = array.size

    override fun load(
        element: Int,
    ): Any = IntVector.broadcast(species, element)

    override fun load(
        array: IntArray,
        offset: Int,
        mask: VectorMask<Int>,
    ): Any = IntVector.fromArray(species, array, offset, mask)

    override fun store(
        array: IntArray,
        offset: Int,
        mask: VectorMask<Int>,
        vector: Any,
    ) = when {
        vector is IntVector ->
            vector.intoArray(array, offset, mask)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Unary,
        fst: Any,
    ): Any = when {
        fst is IntVector ->
            fst.lanewise(operator)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Binary,
        fst: Any,
        snd: Any,
    ): Any = when {
        fst is IntVector && snd is IntVector ->
            fst.lanewise(operator, snd)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Associative,
        fst: Any,
        snd: Any,
    ): Any = when {
        fst is IntVector && snd is IntVector ->
            fst.lanewise(operator, snd)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Ternary,
        fst: Any,
        snd: Any,
        trd: Any,
    ): Any = when {
        fst is IntVector && snd is IntVector && trd is IntVector ->
            fst.lanewise(operator, snd, trd)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Test,
        fst: Any,
    ): VectorMask<Int> = when {
        fst is IntVector ->
            fst.test(operator)

        else -> error("invalid value")
    }

    override fun fn(
        operator: Comparison,
        fst: Any,
        snd: Any,
    ): VectorMask<Int> = when {
        fst is IntVector && snd is IntVector ->
            fst.compare(operator, snd)

        else -> error("invalid value")
    }
}

sealed interface SIMDNode<E : Number, A : Any>

private class ScalarNode<E : Number, A : Any>(
    val value: E,
) : SIMDNode<E, A>

private class ArrayNode<E : Number, A : Any>(
    val value: A,
) : SIMDNode<E, A>

private class UnaryNode<E : Number, A : Any>(
    val operator: Unary,
    val fst: SIMDNode<E, A>,
) : SIMDNode<E, A>

private class BinaryNode<E : Number, A : Any>(
    val operator: Binary,
    val fst: SIMDNode<E, A>,
    val snd: SIMDNode<E, A>,
) : SIMDNode<E, A>

private class AssociativeNode<E : Number, A : Any>(
    val operator: Associative,
    val fst: SIMDNode<E, A>,
    val snd: SIMDNode<E, A>,
) : SIMDNode<E, A>

private class TernaryNode<E : Number, A : Any>(
    val operator: Ternary,
    val fst: SIMDNode<E, A>,
    val snd: SIMDNode<E, A>,
    val trd: SIMDNode<E, A>,
) : SIMDNode<E, A>

private class TestNode<E : Number, A : Any>(
    val operator: Test,
    val fst: SIMDNode<E, A>,
) : SIMDNode<E, A>

private class ComparisonNode<E : Number, A : Any>(
    val operator: Comparison,
    val fst: SIMDNode<E, A>,
    val snd: SIMDNode<E, A>,
) : SIMDNode<E, A>

class SIMD<E : Number, A : Any> {

    fun E.asNode(): SIMDNode<E, A> = ScalarNode(this)

    fun A.asNode(): SIMDNode<E, A> = ArrayNode(this)

    // plus

    operator fun E.plus(other: A): SIMDNode<E, A> =
        this.asNode() + other.asNode()

    operator fun E.plus(other: SIMDNode<E, A>): SIMDNode<E, A> =
        this.asNode() + other

    operator fun A.plus(other: E): SIMDNode<E, A> =
        this.asNode() + other.asNode()

    operator fun A.plus(other: A): SIMDNode<E, A> =
        this.asNode() + other.asNode()

    operator fun A.plus(other: SIMDNode<E, A>): SIMDNode<E, A> =
        this.asNode() + other

    operator fun SIMDNode<E, A>.plus(other: E): SIMDNode<E, A> =
        this + other.asNode()

    operator fun SIMDNode<E, A>.plus(other: A): SIMDNode<E, A> =
        this + other.asNode()

    // minus

    operator fun E.minus(other: A): SIMDNode<E, A> =
        this.asNode() - other.asNode()

    operator fun E.minus(other: SIMDNode<E, A>): SIMDNode<E, A> =
        this.asNode() - other

    operator fun A.minus(other: E): SIMDNode<E, A> =
        this.asNode() - other.asNode()

    operator fun A.minus(other: A): SIMDNode<E, A> =
        this.asNode() - other.asNode()

    operator fun A.minus(other: SIMDNode<E, A>): SIMDNode<E, A> =
        this.asNode() - other

    operator fun SIMDNode<E, A>.minus(other: E): SIMDNode<E, A> =
        this - other.asNode()

    operator fun SIMDNode<E, A>.minus(other: A): SIMDNode<E, A> =
        this - other.asNode()

    // times

    operator fun E.times(other: A): SIMDNode<E, A> =
        this.asNode() * other.asNode()

    operator fun E.times(other: SIMDNode<E, A>): SIMDNode<E, A> =
        this.asNode() * other

    operator fun A.times(other: E): SIMDNode<E, A> =
        this.asNode() * other.asNode()

    operator fun A.times(other: A): SIMDNode<E, A> =
        this.asNode() * other.asNode()

    operator fun A.times(other: SIMDNode<E, A>): SIMDNode<E, A> =
        this.asNode() * other

    operator fun SIMDNode<E, A>.times(other: E): SIMDNode<E, A> =
        this * other.asNode()

    operator fun SIMDNode<E, A>.times(other: A): SIMDNode<E, A> =
        this * other.asNode()

    // div

    operator fun E.div(other: A): SIMDNode<E, A> =
        this.asNode() / other.asNode()

    operator fun E.div(other: SIMDNode<E, A>): SIMDNode<E, A> =
        this.asNode() / other

    operator fun A.div(other: E): SIMDNode<E, A> =
        this.asNode() / other.asNode()

    operator fun A.div(other: A): SIMDNode<E, A> =
        this.asNode() / other.asNode()

    operator fun A.div(other: SIMDNode<E, A>): SIMDNode<E, A> =
        this.asNode() / other

    operator fun SIMDNode<E, A>.div(other: E): SIMDNode<E, A> =
        this / other.asNode()

    operator fun SIMDNode<E, A>.div(other: A): SIMDNode<E, A> =
        this / other.asNode()

    private fun create(
        operator: Unary,
        fst: SIMDNode<E, A>,
    ): UnaryNode<E, A> {
        return UnaryNode(operator, fst)
    }

    private fun create(
        operator: Binary,
        fst: SIMDNode<E, A>,
        snd: SIMDNode<E, A>,
    ): BinaryNode<E, A> {
        return BinaryNode(operator, fst, snd)
    }

    private fun create(
        operator: Associative,
        fst: SIMDNode<E, A>,
        snd: SIMDNode<E, A>,
    ): AssociativeNode<E, A> {
        return AssociativeNode(operator, fst, snd)
    }

    private fun create(
        operator: Ternary,
        fst: SIMDNode<E, A>,
        snd: SIMDNode<E, A>,
        trd: SIMDNode<E, A>
    ): TernaryNode<E, A> {
        return TernaryNode(operator, fst, snd, trd)
    }

    private fun create(
        operator: Test,
        fst: SIMDNode<E, A>,
    ): TestNode<E, A> {
        return TestNode(operator, fst)
    }

    private fun create(
        operator: Comparison,
        fst: SIMDNode<E, A>,
        snd: SIMDNode<E, A>,
    ): ComparisonNode<E, A> {
        return ComparisonNode(operator, fst, snd)
    }

    fun SIMDNode<E, A>.abs(): SIMDNode<E, A> {
        return create(ABS, this)
    }

    operator fun SIMDNode<E, A>.unaryMinus(): SIMDNode<E, A> {
        return create(NEG, this)
    }

    fun sin(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(SIN, fst)
    }

    fun cos(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(COS, fst)
    }

    fun tan(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(TAN, fst)
    }

    fun asin(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(ASIN, fst)
    }

    fun acos(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(ACOS, fst)
    }

    fun atan(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(ATAN, fst)
    }

    fun exp(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(EXP, fst)
    }

    fun log(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(LOG, fst)
    }

    fun log10(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(LOG10, fst)
    }

    fun sqrt(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(SQRT, fst)
    }

    fun cbrt(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(CBRT, fst)
    }

    fun sinh(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(SINH, fst)
    }

    fun cosh(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(COSH, fst)
    }

    fun tanh(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(TANH, fst)
    }

    fun expm1(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(EXPM1, fst)
    }

    fun log1p(fst: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(LOG1P, fst)
    }

    operator fun SIMDNode<E, A>.plus(snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(ADD, this, snd)
    }

    operator fun SIMDNode<E, A>.minus(snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(SUB, this, snd)
    }

    operator fun SIMDNode<E, A>.times(snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(MUL, this, snd)
    }

    operator fun SIMDNode<E, A>.div(snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(DIV, this, snd)
    }

    fun min(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(MIN, fst, snd)
    }

    fun max(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(MAX, fst, snd)
    }

    fun firstNonzero(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(FIRST_NONZERO, fst, snd)
    }

    fun atan2(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(ATAN2, fst, snd)
    }

    fun pow(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(POW, fst, snd)
    }

    fun hypot(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(HYPOT, fst, snd)
    }

    fun bitwiseBlend(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>, trd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(BITWISE_BLEND, fst, snd, trd)
    }

    fun fma(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>, trd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(FMA, fst, snd, trd)
    }

    fun SIMDNode<E, A>.isDefault(): SIMDNode<E, A> {
        return create(IS_DEFAULT, this)
    }

    fun SIMDNode<E, A>.isNegative(): SIMDNode<E, A> {
        return create(IS_NEGATIVE, this)
    }

    fun SIMDNode<E, A>.isFinite(): SIMDNode<E, A> {
        return create(IS_FINITE, this)
    }

    fun SIMDNode<E, A>.isNaN(): SIMDNode<E, A> {
        return create(IS_NAN, this)
    }

    fun SIMDNode<E, A>.isInfinite(): SIMDNode<E, A> {
        return create(IS_INFINITE, this)
    }

    fun compareEQ(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(EQ, fst, snd)
    }

    fun compareNE(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(NE, fst, snd)
    }

    fun compareLT(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(LT, fst, snd)
    }

    fun compareLE(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(LE, fst, snd)
    }

    fun compareGT(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(GT, fst, snd)
    }

    fun compareGE(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(GE, fst, snd)
    }

    fun compareULT(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(ULT, fst, snd)
    }

    fun compareULE(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(ULE, fst, snd)
    }

    fun compareUGT(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(UGT, fst, snd)
    }

    fun compareUGE(fst: SIMDNode<E, A>, snd: SIMDNode<E, A>): SIMDNode<E, A> {
        return create(UGE, fst, snd)
    }
}

@JvmInline
private value class SIMDRegister(
    val id: Int,
)

private sealed interface SIMDInstruction<E : Number, A : Any> {
    val register: SIMDRegister
}

private data class SIMDProgram<E : Number, A : Any>(
    val instructions: List<SIMDInstruction<E, A>>,
    val result: SIMDRegister,
    val count: Int,
) {
    fun getSize(adapter: VectorAdapter<E, A>): Int {
        val sizes = instructions.mapNotNull {
            when (it) {
                is LoadArrayInstruction ->
                    adapter.sizeof(it.value)

                else -> null
            }
        }

        require(sizes.distinct().size <= 1) {
            "all SIMD input values must have the same length"
        }

        return sizes.firstOrNull() ?: 1
    }
}

private class LoadScalarInstruction<E : Number, A : Any>(
    override val register: SIMDRegister,
    val value: E,
) : SIMDInstruction<E, A>

private class LoadArrayInstruction<E : Number, A : Any>(
    override val register: SIMDRegister,
    val value: A,
) : SIMDInstruction<E, A>

private class UnaryInstruction<E : Number, A : Any>(
    override val register: SIMDRegister,
    val operator: Unary,
    val fst: SIMDRegister,
) : SIMDInstruction<E, A>

private class BinaryInstruction<E : Number, A : Any>(
    override val register: SIMDRegister,
    val operator: Binary,
    val fst: SIMDRegister,
    val snd: SIMDRegister,
) : SIMDInstruction<E, A>

private class AssociativeInstruction<E : Number, A : Any>(
    override val register: SIMDRegister,
    val operator: Associative,
    val fst: SIMDRegister,
    val snd: SIMDRegister,
) : SIMDInstruction<E, A>

private class TernaryInstruction<E : Number, A : Any>(
    override val register: SIMDRegister,
    val operator: Ternary,
    val fst: SIMDRegister,
    val snd: SIMDRegister,
    val trd: SIMDRegister,
) : SIMDInstruction<E, A>

private class TestInstruction<E : Number, A : Any>(
    override val register: SIMDRegister,
    val operator: Test,
    val fst: SIMDRegister,
) : SIMDInstruction<E, A>

private class ComparisonInstruction<E : Number, A : Any>(
    override val register: SIMDRegister,
    val operator: Comparison,
    val fst: SIMDRegister,
    val snd: SIMDRegister,
) : SIMDInstruction<E, A>

private class SIMDCompile<E : Number, A : Any> {

    private var next = 0
    private val registers = IdentityHashMap<SIMDNode<E, A>, SIMDRegister>()
    private val instructions = mutableListOf<SIMDInstruction<E, A>>()

    private fun allocate(): SIMDRegister = SIMDRegister(next++)

    operator fun invoke(root: SIMDNode<E, A>): SIMDProgram<E, A> {
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

    private fun visit(node: SIMDNode<E, A>): SIMDRegister {
        registers[node]?.let { return it }

        val register = allocate()

        instructions += when (node) {
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

            is TestNode -> {
                val fst = visit(node.fst)

                TestInstruction(
                    register,
                    node.operator,
                    fst,
                )
            }

            is ComparisonNode -> {
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

private class SIMDExecute<E : Number, A : Any>(
    val program: SIMDProgram<E, A>,
    val adapter: VectorAdapter<E, A>,
) {
    operator fun invoke(): A {
        val size = program.getSize(adapter)

        val registers = arrayOfNulls<Any>(program.count)
        val output = adapter.allocate(size)

        for (offset in 0 until size step adapter.species.length()) {
            val mask: VectorMask<E> = adapter.species.indexInRange(offset, size)

            for (instruction in program.instructions) {
                registers[instruction.register.id] = when (instruction) {
                    is LoadScalarInstruction ->
                        adapter.load(instruction.value)

                    is LoadArrayInstruction ->
                        adapter.load(instruction.value, offset, mask)

                    is UnaryInstruction -> {
                        val fst = registers[instruction.fst.id]

                        when {
                            fst is Vector<*> ->
                                adapter.fn(instruction.operator, fst)

                            else -> error("unsupported operand")
                        }
                    }

                    is BinaryInstruction -> {
                        val fst = registers[instruction.fst.id]
                        val snd = registers[instruction.snd.id]

                        when {
                            fst is Vector<*> && snd is Vector<*> ->
                                adapter.fn(instruction.operator, fst, snd)

                            else -> error("unsupported operand")
                        }
                    }

                    is AssociativeInstruction -> {
                        val fst = registers[instruction.fst.id]
                        val snd = registers[instruction.snd.id]

                        when {
                            fst is Vector<*> && snd is Vector<*> ->
                                adapter.fn(instruction.operator, fst, snd)

                            else -> error("unsupported operand")
                        }
                    }

                    is TernaryInstruction -> {
                        val fst = registers[instruction.fst.id]
                        val snd = registers[instruction.snd.id]
                        val trd = registers[instruction.trd.id]

                        when {
                            fst is Vector<*> && snd is Vector<*> && trd is Vector<*> ->
                                adapter.fn(instruction.operator, fst, snd, trd)

                            else -> error("unsupported operand")
                        }
                    }

                    is TestInstruction -> {
                        val fst = registers[instruction.fst.id]

                        when {
                            fst is Vector<*> ->
                                adapter.fn(instruction.operator, fst)

                            else -> error("unsupported operand")
                        }
                    }

                    is ComparisonInstruction -> {
                        val fst = registers[instruction.fst.id]
                        val snd = registers[instruction.snd.id]

                        when {
                            fst is Vector<*> && snd is Vector<*> ->
                                adapter.fn(instruction.operator, fst, snd)

                            else -> error("unsupported operand")
                        }
                    }
                }
            }

            when (val result = registers[program.result.id]) {
                is Vector<*> ->
                    adapter.store(output, offset, mask, result)

                else -> error("unsupported operand")
            }
        }

        return output
    }
}

private fun <E : Number, A : Any> simd(
    adapter: VectorAdapter<E, A>,
    node: SIMDNode<E, A>,
): A {
    val compile = SIMDCompile<E, A>()
    val program = compile(node)
    val execute = SIMDExecute(program, adapter)
    return execute()
}

private fun <E : Number, A : Any> simd(
    adapter: VectorAdapter<E, A>,
    block: SIMD<E, A>.() -> SIMDNode<E, A>,
): A {
    return simd(
        adapter,
        SIMD<E, A>().run(block),
    )
}

fun simdF(block: SIMD<Float, FloatArray>.() -> SIMDNode<Float, FloatArray>): FloatArray {
    return simd(FloatVectorAdapter, block)
}

fun simdI(block: SIMD<Int, IntArray>.() -> SIMDNode<Int, IntArray>): IntArray {
    return simd(IntVectorAdapter, block)
}
