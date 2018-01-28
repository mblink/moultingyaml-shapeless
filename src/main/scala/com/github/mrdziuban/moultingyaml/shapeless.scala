package com.github.mrdziuban.moultingyaml

import net.jcazevedo.moultingyaml._
import _root_.shapeless.{::, HList, HNil, LabelledProductTypeClassCompanion, LabelledProductTypeClass}

object shapeless extends LabelledProductTypeClassCompanion[YamlFormat] {
  object typeClass extends LabelledProductTypeClass[YamlFormat] {
    def emptyProduct: YamlFormat[HNil] = new YamlFormat[HNil] {
      def read(v: YamlValue): HNil = HNil
      def write(h: HNil): YamlValue = YamlObject()
    }

    def product[H, T <: HList](name: String, yh: YamlFormat[H], yt: YamlFormat[T]): YamlFormat[H :: T] =
      new YamlFormat[H :: T] {
        def read(v: YamlValue): H :: T =
          yh.read(v.asYamlObject.fields.get(YamlString(name)).getOrElse(YamlNull)) ::
            yt.read(YamlObject(v.asYamlObject.fields.filterKeys(_ != YamlString(name))))

        def write(ht: H :: T): YamlValue =
          YamlObject(Map(YamlString(name) -> yh.write(ht.head)) ++ yt.write(ht.tail).asYamlObject.fields)
      }

    def project[F, G](inst: => YamlFormat[G], to: F => G, from: G => F): YamlFormat[F] = new YamlFormat[F] {
      def read(v: YamlValue): F = from(inst.read(v))
      def write(f: F): YamlValue = inst.write(to(f))
    }
  }

  def deriveYamlFormat[A](implicit yf: YamlFormat[A]): YamlFormat[A] = yf
}
