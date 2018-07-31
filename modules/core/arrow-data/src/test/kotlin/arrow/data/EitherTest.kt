package arrow.data

import arrow.Kind
import arrow.Kind2
import arrow.core.*
import arrow.instances.*
import arrow.instances.eq
import arrow.test.UnitSpec
import arrow.test.laws.*
import arrow.typeclasses.Eq
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class EitherTest : UnitSpec() {
  val EQ: Eq<Kind<EitherPartialOf<ForId>, Int>> = Eq { a, b ->
    a.fix() == b.fix()
  }

  val EQ2: Eq<Kind2<ForEither, Int, Int>> = Eq { a, b ->
    a.fix() == b.fix()
  }

  init {

    ForEither<Throwable>() extensions {

      testLaws(
        BifunctorLaws.laws(Either.bifunctor(), { Right(it) }, EQ2),
        SemigroupLaws.laws(Either.semigroup(String.semigroup(), String.semigroup()), Either.right("1"), Either.right("2"), Either.right("3"), Either.eq(String.eq(), String.eq())),
        MonoidLaws.laws(Either.monoid(MOL=String.monoid(), MOR = Int.monoid()), Either.right(1), Either.eq(String.eq(), Int.eq())),
        EqLaws.laws(Either.eq(String.eq(), Int.eq()), { Right(it) }),
        ShowLaws.laws(Either.show(), Either.eq(String.eq(), Int.eq()), { Right(it) }),
        MonadErrorLaws.laws(this, Eq.any(), Eq.any()),
        TraverseLaws.laws(this, this, { Right(it) }, Eq.any()),
        SemigroupKLaws.laws(Either.semigroupK(), Either.applicative(), EQ)
      )

    }

    "empty should return a Right of the empty of the inner type" {
      forAll { a: String ->
        Right(String.monoid().run { empty() }) == Either.monoid(String.monoid(), String.monoid()).run { empty() }
      }
    }

    "combine two rights should return a right of the combine of the inners" {
      forAll { a: String, b: String ->
        String.monoid().run { Either.right(a.combine(b)) } == Either.right(a).combine(String.monoid(), String.monoid(), Either.right(b))
      }
    }

    "combine two lefts should return a left of the combine of the inners" {
      forAll { a: String, b: String ->
        String.monoid().run { Either.left(a.combine(b)) } == Either.left(a).combine(String.monoid(), String.monoid(), Either.left(b))
      }
    }

    "combine a right and a left should return left" {
      forAll { a: String, b: String ->
        Either.left(a) == Either.left(a).combine(String.monoid(), String.monoid(), Either.right(b))
        Either.left(a) == Either.right(b).combine(String.monoid(), String.monoid(), Either.left(a))
      }
    }

    "getOrElse should return value" {
      forAll { a: Int, b: Int ->
        Right(a).getOrElse { b } == a
          && Left(a).getOrElse { b } == b
      }

    }

    "orNull should return value" {
      forAll { a: Int ->
          Either.Right(a).orNull() == a
          && Either.Left(a).orNull() == null
      }

    }

    "getOrHandle should return value" {
      forAll { a: Int, b: Int ->
        Right(a).getOrHandle { b } == a
          && Left(a).getOrHandle { it + b } == a + b
      }

    }

    "filterOrElse should filters value" {
      forAll { a: Int, b: Int ->
        val left: Either<Int, Int> = Left(a)

        Right(a).filterOrElse({ it > a - 1 }, { b }) == Right(a)
          && Right(a).filterOrElse({ it > a + 1 }, { b }) == Left(b)
          && left.filterOrElse({ it > a - 1 }, { b }) == Left(a)
          && left.filterOrElse({ it > a + 1 }, { b }) == Left(a)
      }
    }

    "swap should interchange values" {
      forAll { a: Int ->
        Left(a).swap() == Right(a)
          && Right(a).swap() == Left(a)
      }
    }

    "toOption should convert" {
      forAll { a: Int ->
        Right(a).toOption() == Some(a)
          && Left(a).toOption() == None
      }
    }

    "contains should check value" {
      forAll { a: Int, b: Int ->
        Right(a).contains(a)
          && !Right(a).contains(b)
          && !Left(a).contains(a)
      }
    }

    "mapLeft should alter left instance only" {
      forAll { a: Int, b: Int ->
        val right: Either<Int, Int> = Right(a)
        val left: Either<Int, Int> = Left(b)
        right.mapLeft { it + 1 } == right && left.mapLeft { it + 1 } == Left(b + 1)
      }
    }

    "cond should create right instance only if test is true" {
      forAll { t: Boolean, i: Int, s: String ->
        val expected = if (t) Right(i) else Left(s)
        Either.cond(t, { i }, { s }) == expected
      }
    }

  }
}
