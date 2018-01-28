# moultingyaml-shapeless

Automatic derivation of [moultingyaml's `YamlFormat` typeclass](https://github.com/jcazevedo/moultingyaml/blob/master/src/main/scala/net/jcazevedo/moultingyaml/YamlFormat.scala) for case classes.

This project is available for Scala 2.11 and 2.12.

## Installation

Add to your library dependencies in `build.sbt`:

```scala
libraryDependencies += "bondlink" %% "moultingyaml-shapeless" % "1.0.0"
```

## Usage

```scala
import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._
import bondlink.moultingyaml.shapeless._

case class Example(i: Int, s: String)

// To convert to and from strings and YamlObjects
"""
i: 1
s: test
""".parseYaml.convertTo[Example]
// => Example(1,test)

Example(1, "test").toYaml
// => YamlObject(Map(YamlString(i) -> YamlNumber(1), YamlString(s) -> YamlString(test)))

// To get an instance of YamlFormat
implicitly[YamlFormat[Example]]
// or
deriveYamlFormat[Example]
```

Note that you need to import the `DefaultYamlProtocol` (or your own protocol) as the auto-derivation relies on
implicit `YamlFormat` instances in scope for all the fields of your case class. Without them, compilation will fail:

```scala
import net.jcazevedo.moultingyaml._
import bondlink.moultingyaml.shapeless._

case class Fail(i: Int)

"i: 1".parseYaml.convertTo[Fail]
/*
error: Cannot find YamlReader or YamlFormat type class for Fail
       "i: 1".parseYaml.convertTo[Fail]
                                 ^
*/
```

## Motivation

The motivation for this project was to provide an easy way to derive `YamlFormat` without needing to know the
number of parameters on the case class, which is currently required for
[moultingyaml's `yamlFormatN` functions](https://github.com/jcazevedo/moultingyaml/blob/master/src/main/scala/net/jcazevedo/moultingyaml/ProductFormats.scala). Additionally, using shapeless helps to avoid the runtime reflection that
moultingyaml uses in its `yamlFormatN` functions.

## License

Released under the Apache 2.0 license. See the [LICENSE file](LICENSE) for more details.
