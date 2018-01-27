package com.github.mrdziuban.moultingyaml

import com.fortysevendeg.scalacheck.datetime.joda.ArbitraryJoda._
import com.github.mrdziuban.moultingyaml.shapeless._
import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._
import org.joda.time.DateTime
import org.scalacheck.{Arbitrary, Properties}
import org.scalacheck.Prop.forAll
import org.scalacheck.ScalacheckShapeless._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.{typeOf, TypeTag}

object ShapelessSpec extends Properties("shapeless") {
  case class Empty()
  property("serializes empty case class as empty YamlObject") = forAll((_: Empty).toYaml == YamlObject())
  property("deserializes empty YamlObject as empty case class") = forAll((_: Unit) => YamlObject().convertTo[Empty] == Empty())

  def mkInt(i: Int): YamlValue = YamlNumber(i)
  def mkLong(l: Long): YamlValue = YamlNumber(l)
  def mkFloat(f: Float): YamlValue = YamlNumber(f.toDouble)
  def mkDouble(d: Double): YamlValue = YamlNumber(d)
  def mkByte(b: Byte): YamlValue = YamlNumber(b.toInt)
  def mkShort(s: Short): YamlValue = YamlNumber(s.toInt)
  def mkBigDec(bd: BigDecimal): YamlValue = YamlNumber(bd)
  def mkBigInt(bi: BigInt): YamlValue = YamlNumber(bi)
  def mkUnit(u: Unit): YamlValue = YamlNumber(1)
  def mkBool(b: Boolean): YamlValue = YamlBoolean(b)
  def mkChar(c: Char): YamlValue = YamlString(String.valueOf(c))
  def mkStr(s: String): YamlValue = YamlString(s)
  def mkSym(s: Symbol): YamlValue = YamlString(s.name)
  def mkDt(dt: DateTime): YamlValue = YamlDate(dt)

  case class IntAndStr(i: Int, s: String)
  def mkExpectedIntAndStr(is: IntAndStr): YamlValue =
    YamlObject(YamlString("i") -> mkInt(is.i), YamlString("s") -> mkStr(is.s))

  case class Example[A: Arbitrary: ClassTag: TypeTag: YamlFormat](
    expectedClass: String,
    mkExpected: A => YamlValue,
    collections: Boolean = true,
    compareDeserialized: Option[(A, A) => Boolean] = None
  ) {
    val t = typeOf[A]
    case class Klass(x: A)
    case class KlassO(x: Option[A])
    case class KlassEither(x: Either[IntAndStr, A])

    def mkProps(): Unit = {
      property(s"serializes $t as $expectedClass") = forAll((x: Klass) =>
        x.toYaml == YamlObject(YamlString("x") -> mkExpected(x.x)))

      property(s"deserializes $expectedClass to $t") = forAll { (x: A) =>
        val actual = YamlObject(YamlString("x") -> mkExpected(x)).convertTo[Klass]
        val expected = Klass(x)
        compareDeserialized.map(_(actual.x, expected.x)).getOrElse(actual == expected)
      }

      property(s"serializes Option[$t] as $expectedClass or YamlNull") = forAll((x: KlassO) =>
        x.toYaml == YamlObject(YamlString("x") -> x.x.map(mkExpected(_)).getOrElse(YamlNull)))

      property(s"deserializes $expectedClass or YamlNull to Option[$t]") = forAll { (x: Option[A]) =>
        val actual = YamlObject(YamlString("x") -> x.map(mkExpected(_)).getOrElse(YamlNull)).convertTo[KlassO]
        val expected = KlassO(x)
        (for {
          a <- actual.x
          e <- expected.x
          c <- compareDeserialized
        } yield c(a, e)).getOrElse(actual == expected)
      }

      // Anything can be converted to Unit which causes failures with Either[_, Unit] tests
      if (!t.toString.contains("Unit") && !t.toString.contains("IntAndStr")) {
        property(s"serializes Either[IntAndStr, $t] as $expectedClass or empty object") = forAll((x: KlassEither) =>
          x.toYaml == YamlObject(YamlString("x") -> x.x.fold(mkExpectedIntAndStr(_), mkExpected(_))))

        property(s"deserializes $expectedClass or empty object to Either[IntAndStr, $t]") = forAll { (x: Either[IntAndStr, A]) =>
          val actual = YamlObject(YamlString("x") -> x.fold(mkExpectedIntAndStr(_), mkExpected(_))).convertTo[KlassEither]
          val expected = KlassEither(x)
          (for {
            a <- actual.x.right
            e <- expected.x.right
          } yield compareDeserialized.map(_(a, e)).getOrElse(a == a)).right.getOrElse(actual == expected)
        }
      }

      if (collections) {
        Example[List[A]](s"YamlArray of $expectedClass", x => YamlArray(x.map(mkExpected(_)):_*), false).mkProps

        Example[Seq[A]](s"YamlArray of $expectedClass", x => YamlArray(x.map(mkExpected(_)):_*), false).mkProps

        Example[Array[A]](s"YamlArray of $expectedClass", x => YamlArray(x.map(mkExpected(_)):_*), false,
          Some((y: Array[A], k: Array[A]) => y.deep == k.deep)).mkProps

        Example[Set[A]](s"YamlSet of $expectedClass", x => YamlSet(x.map(mkExpected(_))), false).mkProps

        Example[Map[String, A]](s"YamlObject of YamlString -> $expectedClass",
          x => YamlObject(x.map(t => (YamlString(t._1): YamlValue) -> mkExpected(t._2))),
          false).mkProps
      }

      ()
    }
  }

  List(
    Example[Int]("YamlNumber", mkInt(_)),
    Example[Long]("YamlNumber", mkLong(_)),
    Example[Float]("YamlNumber", mkFloat(_)),
    Example[Double]("YamlNumber", mkDouble(_)),
    Example[Byte]("YamlNumber", mkByte(_)),
    Example[Short]("YamlNumber", mkShort(_)),
    Example[BigDecimal]("YamlNumber", mkBigDec(_)),
    Example[BigInt]("YamlNumber", mkBigInt(_)),
    Example[Unit]("YamlNumber", mkUnit(_)),
    Example[Boolean]("YamlBoolean", mkBool(_)),
    Example[Char]("YamlString", mkChar(_)),
    Example[String]("YamlString", mkStr(_)),
    Example[Symbol]("YamlString", mkSym(_)),
    Example[DateTime]("YamlDate", mkDt(_)),
    Example[IntAndStr]("YamlObject", mkExpectedIntAndStr(_))).foreach(_.mkProps)

  case class CCExample[A: Arbitrary: ClassTag: TypeTag: YamlFormat](productFormatter: YamlFormat[A], num: Int) {
    def mkProps(): Unit = {
      property(s"serializes case class with $num fields the same") = forAll((a: A) =>
        a.toYaml == productFormatter.write(a))

      property(s"deserializes case class with $num fields the same") = forAll { (a: A) =>
        val serialized = a.toYaml
        serialized.convertTo[A] == productFormatter.read(serialized)
      }

      ()
    }
  }

  case class CC1(a1: Int)
  case class CC2(a1: Int, a2: Int)
  case class CC3(a1: Int, a2: Int, a3: Int)
  case class CC4(a1: Int, a2: Int, a3: Int, a4: Int)
  case class CC5(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int)
  case class CC6(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int)
  case class CC7(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int)
  case class CC8(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int)
  case class CC9(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int)
  case class CC10(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int)
  case class CC11(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int)
  case class CC12(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int)
  case class CC13(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int)
  case class CC14(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int)
  case class CC15(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int)
  case class CC16(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int)
  case class CC17(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int)
  case class CC18(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int)
  case class CC19(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int)
  case class CC20(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int)
  case class CC21(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int)
  case class CC22(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int)

  List(
    CCExample[Empty](yamlFormat0(Empty), 0),
    CCExample[CC1](yamlFormat1(CC1), 1),
    CCExample[CC2](yamlFormat2(CC2), 2),
    CCExample[CC3](yamlFormat3(CC3), 3),
    CCExample[CC4](yamlFormat4(CC4), 4),
    CCExample[CC5](yamlFormat5(CC5), 5),
    CCExample[CC6](yamlFormat6(CC6), 6),
    CCExample[CC7](yamlFormat7(CC7), 7),
    CCExample[CC8](yamlFormat8(CC8), 8),
    CCExample[CC9](yamlFormat9(CC9), 9),
    CCExample[CC10](yamlFormat10(CC10), 10),
    CCExample[CC11](yamlFormat11(CC11), 11),
    CCExample[CC12](yamlFormat12(CC12), 12),
    CCExample[CC13](yamlFormat13(CC13), 13),
    CCExample[CC14](yamlFormat14(CC14), 14),
    CCExample[CC15](yamlFormat15(CC15), 15),
    CCExample[CC16](yamlFormat16(CC16), 16),
    CCExample[CC17](yamlFormat17(CC17), 17),
    CCExample[CC18](yamlFormat18(CC18), 18),
    CCExample[CC19](yamlFormat19(CC19), 19),
    CCExample[CC20](yamlFormat20(CC20), 20),
    CCExample[CC21](yamlFormat21(CC21), 21),
    CCExample[CC22](yamlFormat22(CC22), 22)).foreach(_.mkProps)

  case class CC23(a1: Int, a2: Int, a3: Int, a4: Int, a5: Int, a6: Int, a7: Int, a8: Int, a9: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int, a23: Int)
  property("serializes case class with more than 22 fields") = forAll((a: CC23) => a.toYaml.isInstanceOf[YamlObject])
  property("deserializes case class with more than 22 fields") = forAll((a: CC23) => a.toYaml.convertTo[CC23] == a)

  case class Nested(c: CC1)
  property("serializes nested case class") = forAll((n: Nested) =>
    n.toYaml == YamlObject(YamlString("c") -> YamlObject(YamlString("a1") -> YamlNumber(n.c.a1))))
  property("deserializes nested case class") = forAll((n: Nested) => n.toYaml.convertTo[Nested] == n)

  case class DeeplyNested(n: Nested)
  property("serializes deeply nested case class") = forAll((d: DeeplyNested) =>
    d.toYaml == YamlObject(YamlString("n") -> YamlObject(YamlString("c") -> YamlObject(YamlString("a1") -> YamlNumber(d.n.c.a1)))))
  property("deserializes deeply nested case class") = forAll((d: DeeplyNested) => d.toYaml.convertTo[DeeplyNested] == d)
}
