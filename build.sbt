lazy val project = Project("moultingyaml-shapeless", file("."))
  .settings(Seq(
    organization := "bondlink",
    version := "1.0.0",

    scalaVersion := "2.12.4",
    crossScalaVersions := Seq("2.12.4", "2.11.11"),

    scalacOptions := Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture",
      "-P:splain:all") ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, major)) if major == 11 => Seq("-Xlint", "-Ywarn-unused", "-Ywarn-unused-import")
        case _ => Seq("-Xlint:-unused,_", "-Ywarn-unused:implicits,imports,locals,params,patvars,privates")
      }),
    scalacOptions in (Compile, console) :=
      scalacOptions.value.filterNot(o => o.contains("-Ywarn-unused") || o.contains("splain")),
    scalacOptions in Tut := (scalacOptions in (Compile, console)).value,

    initialCommands in console := """
    import bondlink.moultingyaml.shapeless._
    import net.jcazevedo.moultingyaml._
    import net.jcazevedo.moultingyaml.DefaultYamlProtocol._
    """,

    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= Seq(
      "net.jcazevedo" %% "moultingyaml" % "0.4.0",
      "com.chuusai" %% "shapeless" % "2.3.3",
      "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.6" % "test",
      "com.fortysevendeg" %% "scalacheck-toolbox-datetime" % "0.2.1" % "test"
    ),
    addCompilerPlugin("io.tryp" % "splain" % "0.2.7" cross CrossVersion.patch),

    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("https://github.com/mblink/moultingyaml-shapeless")),
    bintrayOrganization := Some("bondlink"),
    bintrayRepository := "moultingyaml-shapeless",
    bintrayReleaseOnPublish in ThisBuild := false,
    publish := {},
    publishLocal := {},

    tutTargetDirectory := baseDirectory.value
  ))
  .enablePlugins(TutPlugin)
