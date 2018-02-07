package arrow.core

import arrow.higherkind

/**
 * Represents optional values. Instances of `Option`
 * are either an instance of $some or the object $none.
 */
@higherkind
sealed class Option<out A> : OptionOf<A> {

    companion object {

        fun <A> pure(a: A): Option<A> = Some(a)

        tailrec fun <A, B> tailRecM(a: A, f: (A) -> OptionOf<Either<A, B>>): Option<B> {
            val option = f(a).reify()
            return when (option) {
                is Some -> {
                    when (option.t) {
                        is Either.Left -> tailRecM(option.t.a, f)
                        is Either.Right -> Some(option.t.b)
                    }
                }
                is None -> None
            }
        }

        fun <A> fromNullable(a: A?): Option<A> = if (a != null) Some(a) else None

        operator fun <A> invoke(a: A): Option<A> = Some(a)

        fun <A> empty(): Option<A> = None

    }

    /**
     * Returns true if the option is [None], false otherwise.
     * @note Used only for performance instead of fold.
     */
    abstract fun isEmpty(): Boolean

    /**
     * alias for [isDefined]
     */
    fun nonEmpty(): Boolean = isDefined()

    /**
     * Returns true if the option is an instance of [Some], false otherwise.
     * @note Used only for performance instead of fold.
     */
    fun isDefined(): Boolean = !isEmpty()

    @Deprecated(DeprecatedUnsafeAccess, ReplaceWith("getOrElse { ifEmpty }"))
    abstract fun get(): A

    fun orNull(): A? = fold({ null }, { it })

    /**
     * Returns a [Some<$B>] containing the result of applying $f to this $option's
     * value if this $option is nonempty. Otherwise return $none.
     *
     * @note This is similar to `flatMap` except here,
     * $f does not need to wrap its result in an $option.
     *
     * @param f the function to apply
     * @see flatMap
     */
    inline fun <B> map(crossinline f: (A) -> B): Option<B> = fold({ None }, { a -> Some(f(a)) })

    inline fun <P1, R> map(p1: Option<P1>, crossinline f: (A, P1) -> R): Option<R> = if (isEmpty()) {
        None
    } else {
        p1.map { pp1 -> f(get(), pp1) }
    }

    inline fun <R> fold(ifEmpty: () -> R, some: (A) -> R): R = when (this) {
        is None -> ifEmpty()
        is Some<A> -> some(t)
    }

    /**
     * Returns the result of applying $f to this $option's value if
     * this $option is nonempty.
     * Returns $none if this $option is empty.
     * Slightly different from `map` in that $f is expected to
     * return an $option (which could be $none).
     *
     * @param f the function to apply
     * @see map
     */
    inline fun <B> flatMap(crossinline f: (A) -> OptionOf<B>): Option<B> = fold({ None }, { a -> f(a) }).reify()

    fun <B> ap(ff: OptionOf<(A) -> B>): Option<B> = ff.reify().flatMap { this.reify().map(it) }

    /**
     * Returns this $option if it is nonempty '''and''' applying the predicate $p to
     * this $option's value returns true. Otherwise, return $none.
     *
     *  @param predicate the predicate used for testing.
     */
    inline fun filter(crossinline predicate: Predicate<A>): Option<A> =
            fold({ None }, { a -> if (predicate(a)) Some(a) else None })

    /**
     * Returns this $option if it is nonempty '''and''' applying the predicate $p to
     * this $option's value returns false. Otherwise, return $none.
     *
     * @param predicate the predicate used for testing.
     */
    inline fun filterNot(crossinline predicate: Predicate<A>): Option<A> = fold({ None }, { a -> if (!predicate(a)) Some(a) else None })

    /**
     * Returns true if this option is nonempty '''and''' the predicate
     * $p returns true when applied to this $option's value.
     * Otherwise, returns false.
     *
     * @param predicate the predicate to test
     */
    inline fun exists(crossinline predicate: Predicate<A>): Boolean = fold({ false }, { a -> predicate(a) })

    /**
     * Returns true if this option is empty '''or''' the predicate
     * $p returns true when applied to this $option's value.
     *
     * @param p the predicate to test
     */
    inline fun forall(crossinline p: Predicate<A>): Boolean = fold({ true }, p)

    @Deprecated(DeprecatedUnsafeAccess, ReplaceWith("fold({ Unit }, f)"))
    inline fun forEach(f: (A) -> Unit) {
        if (nonEmpty()) f(get())
    }

    fun <B> foldLeft(b: B, f: (B, A) -> B): B =
            this.reify().let { option ->
                when (option) {
                    is Some -> f(b, option.t)
                    is None -> b
                }
            }

    fun <B> foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
            this.reify().let { option ->
                when (option) {
                    is Some -> f(option.t, lb)
                    is None -> lb
                }
            }

    fun toList(): List<A> = fold(::emptyList, { listOf(it) })

    infix fun <X> and(value: Option<X>): Option<X> = if (isEmpty()) {
        None
    } else {
        value
    }
}

object None : Option<Nothing>() {
    override fun get() = throw NoSuchElementException("None.get")

    override fun isEmpty() = true

    override fun toString(): String = "None"
}

data class Some<out T>(val t: T) : Option<T>() {
    override fun get() = t

    override fun isEmpty() = false

    override fun toString(): String = "Some($t)"
}

/**
 * Returns the option's value if the option is nonempty, otherwise
 * return the result of evaluating `default`.
 *
 * @param default the default expression.
 */
fun <T> Option<T>.getOrElse(default: () -> T): T = fold({ default() }, { it })

/**
 * Returns this option's if the option is nonempty, otherwise
 * returns another option provided lazily by `default`.
 *
 * @param alternative the default option if this is empty.
 */
fun <A, B : A> OptionOf<B>.orElse(alternative: () -> Option<B>): Option<B> = if (reify().isEmpty()) alternative() else reify()

infix fun <T> OptionOf<T>.or(value: Option<T>): Option<T> = if (reify().isEmpty()) {
    value
} else {
    reify()
}
