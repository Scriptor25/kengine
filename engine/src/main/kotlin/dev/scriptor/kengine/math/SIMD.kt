package dev.scriptor.kengine.math

import dev.scriptor.kengine.math.internal.*
import jdk.incubator.vector.VectorOperators.*

class SIMD<E : Number, A : Any> {

    val exports: List<Node<E, A>>
        field = mutableListOf<Node<E, A>>()

    fun E.asNode(): Node<E, A> = ScalarNode(this)

    fun A.asNode(): Node<E, A> = ArrayNode(this)

    // plus

    operator fun E.plus(other: A): Node<E, A> =
        this.asNode() + other.asNode()

    operator fun E.plus(other: Node<E, A>): Node<E, A> =
        this.asNode() + other

    operator fun A.plus(other: E): Node<E, A> =
        this.asNode() + other.asNode()

    operator fun A.plus(other: A): Node<E, A> =
        this.asNode() + other.asNode()

    operator fun A.plus(other: Node<E, A>): Node<E, A> =
        this.asNode() + other

    operator fun Node<E, A>.plus(other: E): Node<E, A> =
        this + other.asNode()

    operator fun Node<E, A>.plus(other: A): Node<E, A> =
        this + other.asNode()

    // minus

    operator fun E.minus(other: A): Node<E, A> =
        this.asNode() - other.asNode()

    operator fun E.minus(other: Node<E, A>): Node<E, A> =
        this.asNode() - other

    operator fun A.minus(other: E): Node<E, A> =
        this.asNode() - other.asNode()

    operator fun A.minus(other: A): Node<E, A> =
        this.asNode() - other.asNode()

    operator fun A.minus(other: Node<E, A>): Node<E, A> =
        this.asNode() - other

    operator fun Node<E, A>.minus(other: E): Node<E, A> =
        this - other.asNode()

    operator fun Node<E, A>.minus(other: A): Node<E, A> =
        this - other.asNode()

    // times

    operator fun E.times(other: A): Node<E, A> =
        this.asNode() * other.asNode()

    operator fun E.times(other: Node<E, A>): Node<E, A> =
        this.asNode() * other

    operator fun A.times(other: E): Node<E, A> =
        this.asNode() * other.asNode()

    operator fun A.times(other: A): Node<E, A> =
        this.asNode() * other.asNode()

    operator fun A.times(other: Node<E, A>): Node<E, A> =
        this.asNode() * other

    operator fun Node<E, A>.times(other: E): Node<E, A> =
        this * other.asNode()

    operator fun Node<E, A>.times(other: A): Node<E, A> =
        this * other.asNode()

    // div

    operator fun E.div(other: A): Node<E, A> =
        this.asNode() / other.asNode()

    operator fun E.div(other: Node<E, A>): Node<E, A> =
        this.asNode() / other

    operator fun A.div(other: E): Node<E, A> =
        this.asNode() / other.asNode()

    operator fun A.div(other: A): Node<E, A> =
        this.asNode() / other.asNode()

    operator fun A.div(other: Node<E, A>): Node<E, A> =
        this.asNode() / other

    operator fun Node<E, A>.div(other: E): Node<E, A> =
        this / other.asNode()

    operator fun Node<E, A>.div(other: A): Node<E, A> =
        this / other.asNode()

    private fun create(
        operator: Unary,
        fst: Node<E, A>,
    ): UnaryNode<E, A> {
        return UnaryNode(operator, fst)
    }

    private fun create(
        operator: Binary,
        fst: Node<E, A>,
        snd: Node<E, A>,
    ): BinaryNode<E, A> {
        return BinaryNode(operator, fst, snd)
    }

    private fun create(
        operator: Associative,
        fst: Node<E, A>,
        snd: Node<E, A>,
    ): AssociativeNode<E, A> {
        return AssociativeNode(operator, fst, snd)
    }

    private fun create(
        operator: Ternary,
        fst: Node<E, A>,
        snd: Node<E, A>,
        trd: Node<E, A>
    ): TernaryNode<E, A> {
        return TernaryNode(operator, fst, snd, trd)
    }

    private fun create(
        operator: Test,
        fst: Node<E, A>,
    ): TestMask<E, A> {
        return TestMask(operator, fst)
    }

    private fun create(
        operator: Comparison,
        fst: Node<E, A>,
        snd: Node<E, A>,
    ): ComparisonMask<E, A> {
        return ComparisonMask(operator, fst, snd)
    }

    fun Node<E, A>.abs(): Node<E, A> {
        return create(ABS, this)
    }

    operator fun Node<E, A>.unaryMinus(): Node<E, A> {
        return create(NEG, this)
    }

    fun sin(fst: Node<E, A>): Node<E, A> {
        return create(SIN, fst)
    }

    fun cos(fst: Node<E, A>): Node<E, A> {
        return create(COS, fst)
    }

    fun tan(fst: Node<E, A>): Node<E, A> {
        return create(TAN, fst)
    }

    fun asin(fst: Node<E, A>): Node<E, A> {
        return create(ASIN, fst)
    }

    fun acos(fst: Node<E, A>): Node<E, A> {
        return create(ACOS, fst)
    }

    fun atan(fst: Node<E, A>): Node<E, A> {
        return create(ATAN, fst)
    }

    fun exp(fst: Node<E, A>): Node<E, A> {
        return create(EXP, fst)
    }

    fun log(fst: Node<E, A>): Node<E, A> {
        return create(LOG, fst)
    }

    fun log10(fst: Node<E, A>): Node<E, A> {
        return create(LOG10, fst)
    }

    fun sqrt(fst: Node<E, A>): Node<E, A> {
        return create(SQRT, fst)
    }

    fun cbrt(fst: Node<E, A>): Node<E, A> {
        return create(CBRT, fst)
    }

    fun sinh(fst: Node<E, A>): Node<E, A> {
        return create(SINH, fst)
    }

    fun cosh(fst: Node<E, A>): Node<E, A> {
        return create(COSH, fst)
    }

    fun tanh(fst: Node<E, A>): Node<E, A> {
        return create(TANH, fst)
    }

    fun expm1(fst: Node<E, A>): Node<E, A> {
        return create(EXPM1, fst)
    }

    fun log1p(fst: Node<E, A>): Node<E, A> {
        return create(LOG1P, fst)
    }

    operator fun Node<E, A>.plus(snd: Node<E, A>): Node<E, A> {
        return create(ADD, this, snd)
    }

    operator fun Node<E, A>.minus(snd: Node<E, A>): Node<E, A> {
        return create(SUB, this, snd)
    }

    operator fun Node<E, A>.times(snd: Node<E, A>): Node<E, A> {
        return create(MUL, this, snd)
    }

    operator fun Node<E, A>.div(snd: Node<E, A>): Node<E, A> {
        return create(DIV, this, snd)
    }

    fun min(fst: Node<E, A>, snd: Node<E, A>): Node<E, A> {
        return create(MIN, fst, snd)
    }

    fun max(fst: Node<E, A>, snd: Node<E, A>): Node<E, A> {
        return create(MAX, fst, snd)
    }

    fun firstNonzero(fst: Node<E, A>, snd: Node<E, A>): Node<E, A> {
        return create(FIRST_NONZERO, fst, snd)
    }

    fun atan2(fst: Node<E, A>, snd: Node<E, A>): Node<E, A> {
        return create(ATAN2, fst, snd)
    }

    fun pow(fst: Node<E, A>, snd: Node<E, A>): Node<E, A> {
        return create(POW, fst, snd)
    }

    fun hypot(fst: Node<E, A>, snd: Node<E, A>): Node<E, A> {
        return create(HYPOT, fst, snd)
    }

    fun bitwiseBlend(fst: Node<E, A>, snd: Node<E, A>, trd: Node<E, A>): Node<E, A> {
        return create(BITWISE_BLEND, fst, snd, trd)
    }

    fun fma(fst: Node<E, A>, snd: Node<E, A>, trd: Node<E, A>): Node<E, A> {
        return create(FMA, fst, snd, trd)
    }

    fun Node<E, A>.isDefault(): Mask<E, A> {
        return create(IS_DEFAULT, this)
    }

    fun Node<E, A>.isNegative(): Mask<E, A> {
        return create(IS_NEGATIVE, this)
    }

    fun Node<E, A>.isFinite(): Mask<E, A> {
        return create(IS_FINITE, this)
    }

    fun Node<E, A>.isNaN(): Mask<E, A> {
        return create(IS_NAN, this)
    }

    fun Node<E, A>.isInfinite(): Mask<E, A> {
        return create(IS_INFINITE, this)
    }

    infix fun Node<E, A>.eq(snd: Node<E, A>): Mask<E, A> {
        return create(EQ, this, snd)
    }

    infix fun Node<E, A>.ne(snd: Node<E, A>): Mask<E, A> {
        return create(NE, this, snd)
    }

    infix fun Node<E, A>.lt(snd: Node<E, A>): Mask<E, A> {
        return create(LT, this, snd)
    }

    infix fun Node<E, A>.le(snd: Node<E, A>): Mask<E, A> {
        return create(LE, this, snd)
    }

    infix fun Node<E, A>.gt(snd: Node<E, A>): Mask<E, A> {
        return create(GT, this, snd)
    }

    infix fun Node<E, A>.ge(snd: Node<E, A>): Mask<E, A> {
        return create(GE, this, snd)
    }

    infix fun Node<E, A>.ult(snd: Node<E, A>): Mask<E, A> {
        return create(ULT, this, snd)
    }

    infix fun Node<E, A>.ule(snd: Node<E, A>): Mask<E, A> {
        return create(ULE, this, snd)
    }

    infix fun Node<E, A>.ugt(snd: Node<E, A>): Mask<E, A> {
        return create(UGT, this, snd)
    }

    infix fun Node<E, A>.uge(snd: Node<E, A>): Mask<E, A> {
        return create(UGE, this, snd)
    }

    infix fun Mask<E, A>.and(snd: Mask<E, A>): Mask<E, A> {
        return AndMask(this, snd)
    }

    infix fun Mask<E, A>.or(snd: Mask<E, A>): Mask<E, A> {
        return OrMask(this, snd)
    }

    fun select(mask: Mask<E, A>, fst: Node<E, A>, snd: Node<E, A>): Node<E, A> {
        return SelectNode(mask, fst, snd)
    }

    fun export(node: Node<E, A>) {
        exports += node
    }
}

class ArgumentList<E : Number, A : Any>(
    val array: Array<Node<E, A>>,
) {
    operator fun component1(): Node<E, A> = array[0]
    operator fun component2(): Node<E, A> = array[1]
    operator fun component3(): Node<E, A> = array[2]
    operator fun component4(): Node<E, A> = array[3]
    operator fun component5(): Node<E, A> = array[4]
    operator fun component6(): Node<E, A> = array[5]
    operator fun component7(): Node<E, A> = array[6]
    operator fun component8(): Node<E, A> = array[7]
    operator fun component9(): Node<E, A> = array[8]
    operator fun component10(): Node<E, A> = array[9]
    operator fun component11(): Node<E, A> = array[10]
    operator fun component12(): Node<E, A> = array[11]
}

fun <E : Number, A : Any> simd(
    node: Node<E, A>,
): Program<E, A> {
    return Compile<E, A>()(node)
}

fun <E : Number, A : Any> simd(
    exports: List<Node<E, A>>,
): Program<E, A> {
    return Compile<E, A>()(exports)
}

fun <E : Number, A : Any> simd(
    count: Int,
    block: SIMD<E, A>.(ArgumentList<E, A>) -> Unit,
): Program<E, A> {
    val context = SIMD<E, A>()
    val arguments = Array<Node<E, A>>(count) { ArgumentNode(it) }
    context.block(ArgumentList(arguments))
    return simd(context.exports)
}

fun simdF(
    count: Int,
    block: SIMD<Float, FloatArray>.(ArgumentList<Float, FloatArray>) -> Unit,
): Program<Float, FloatArray> {
    return simd(count, block)
}

fun simdI(
    count: Int,
    block: SIMD<Int, IntArray>.(ArgumentList<Int, IntArray>) -> Unit,
): Program<Int, IntArray> {
    return simd(count, block)
}
