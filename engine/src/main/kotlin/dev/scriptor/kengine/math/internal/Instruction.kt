package dev.scriptor.kengine.math.internal

import jdk.incubator.vector.VectorOperators.*

private fun reg(mapping: IntArray, register: Register): Register =
    Register(mapping[register.id])

sealed interface Instruction<E : Number, A : Any> {
    val register: Register
    val sources: IntArray
    val type: RegisterType

    fun rewrite(mapping: IntArray): Instruction<E, A>
}

class LoadArgumentInstruction<E : Number, A : Any>(
    override val register: Register,
    val index: Int,
) : Instruction<E, A> {
    override val sources = intArrayOf()
    override val type = RegisterType.Vector

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        LoadArgumentInstruction(
            reg(mapping, register),
            index,
        )
}

class LoadScalarInstruction<E : Number, A : Any>(
    override val register: Register,
    val value: E,
) : Instruction<E, A> {
    override val sources = intArrayOf()
    override val type = RegisterType.Vector

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        LoadScalarInstruction(
            reg(mapping, register),
            value,
        )
}

class LoadArrayInstruction<E : Number, A : Any>(
    override val register: Register,
    val value: A,
) : Instruction<E, A> {
    override val sources = intArrayOf()
    override val type = RegisterType.Vector

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        LoadArrayInstruction(
            reg(mapping, register),
            value,
        )
}

class UnaryInstruction<E : Number, A : Any>(
    override val register: Register,
    val operator: Unary,
    val fst: Register,
) : Instruction<E, A> {
    override val sources = intArrayOf(fst.id)
    override val type = RegisterType.Vector

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        UnaryInstruction(
            reg(mapping, register),
            operator,
            reg(mapping, fst),
        )
}

class BinaryInstruction<E : Number, A : Any>(
    override val register: Register,
    val operator: Binary,
    val fst: Register,
    val snd: Register,
) : Instruction<E, A> {
    override val sources = intArrayOf(fst.id, snd.id)
    override val type = RegisterType.Vector

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        BinaryInstruction(
            reg(mapping, register),
            operator,
            reg(mapping, fst),
            reg(mapping, snd),
        )
}

class AssociativeInstruction<E : Number, A : Any>(
    override val register: Register,
    val operator: Associative,
    val fst: Register,
    val snd: Register,
) : Instruction<E, A> {
    override val sources = intArrayOf(fst.id, snd.id)
    override val type = RegisterType.Vector

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        AssociativeInstruction(
            reg(mapping, register),
            operator,
            reg(mapping, fst),
            reg(mapping, snd),
        )
}

class TernaryInstruction<E : Number, A : Any>(
    override val register: Register,
    val operator: Ternary,
    val fst: Register,
    val snd: Register,
    val trd: Register,
) : Instruction<E, A> {
    override val sources = intArrayOf(fst.id, snd.id, trd.id)
    override val type = RegisterType.Vector

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        TernaryInstruction(
            reg(mapping, register),
            operator,
            reg(mapping, fst),
            reg(mapping, snd),
            reg(mapping, trd),
        )
}

class SelectInstruction<E : Number, A : Any>(
    override val register: Register,
    val mask: Register,
    val fst: Register,
    val snd: Register,
) : Instruction<E, A> {
    override val sources = intArrayOf(mask.id, fst.id, snd.id)
    override val type = RegisterType.Vector

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        SelectInstruction(
            reg(mapping, register),
            reg(mapping, mask),
            reg(mapping, fst),
            reg(mapping, snd),
        )
}

class TestInstruction<E : Number, A : Any>(
    override val register: Register,
    val operator: Test,
    val fst: Register,
) : Instruction<E, A> {
    override val sources = intArrayOf(fst.id)
    override val type = RegisterType.Mask

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        TestInstruction(
            reg(mapping, register),
            operator,
            reg(mapping, fst),
        )
}

class ComparisonInstruction<E : Number, A : Any>(
    override val register: Register,
    val operator: Comparison,
    val fst: Register,
    val snd: Register,
) : Instruction<E, A> {
    override val sources = intArrayOf(fst.id, snd.id)
    override val type = RegisterType.Mask

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        ComparisonInstruction(
            reg(mapping, register),
            operator,
            reg(mapping, fst),
            reg(mapping, snd),
        )
}

class AndInstruction<E : Number, A : Any>(
    override val register: Register,
    val fst: Register,
    val snd: Register,
) : Instruction<E, A> {
    override val sources = intArrayOf(fst.id, snd.id)
    override val type = RegisterType.Mask

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        AndInstruction(
            reg(mapping, register),
            reg(mapping, fst),
            reg(mapping, snd),
        )
}

class OrInstruction<E : Number, A : Any>(
    override val register: Register,
    val fst: Register,
    val snd: Register,
) : Instruction<E, A> {
    override val sources = intArrayOf(fst.id, snd.id)
    override val type = RegisterType.Mask

    override fun rewrite(mapping: IntArray): Instruction<E, A> =
        OrInstruction(
            reg(mapping, register),
            reg(mapping, fst),
            reg(mapping, snd),
        )
}
