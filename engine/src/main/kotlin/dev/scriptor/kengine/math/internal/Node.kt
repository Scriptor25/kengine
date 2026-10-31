package dev.scriptor.kengine.math.internal

import jdk.incubator.vector.VectorOperators.*

sealed interface Node<E : Number, A : Any>

class ArgumentNode<E : Number, A : Any>(
    val index: Int,
) : Node<E, A>

class ScalarNode<E : Number, A : Any>(
    val value: E,
) : Node<E, A>

class ArrayNode<E : Number, A : Any>(
    val value: A,
) : Node<E, A>

class UnaryNode<E : Number, A : Any>(
    val operator: Unary,
    val fst: Node<E, A>,
) : Node<E, A>

class BinaryNode<E : Number, A : Any>(
    val operator: Binary,
    val fst: Node<E, A>,
    val snd: Node<E, A>,
) : Node<E, A>

class AssociativeNode<E : Number, A : Any>(
    val operator: Associative,
    val fst: Node<E, A>,
    val snd: Node<E, A>,
) : Node<E, A>

class TernaryNode<E : Number, A : Any>(
    val operator: Ternary,
    val fst: Node<E, A>,
    val snd: Node<E, A>,
    val trd: Node<E, A>,
) : Node<E, A>

class AndMask<E : Number, A : Any>(
    val fst: Mask<E, A>,
    val snd: Mask<E, A>,
) : Mask<E, A>

class OrMask<E : Number, A : Any>(
    val fst: Mask<E, A>,
    val snd: Mask<E, A>,
) : Mask<E, A>

class SelectNode<E : Number, A : Any>(
    val mask: Mask<E, A>,
    val fst: Node<E, A>,
    val snd: Node<E, A>,
) : Node<E, A>
