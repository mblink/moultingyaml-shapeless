import sbt._
import sbt.Keys._

scalaVersion := "2.12.4"

scalacOptions := Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint:-unused,_",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-infer-any",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused:imports,privates,locals",
  "-Ywarn-value-discard",
  "-Xfuture"
)

scalacOptions in (Compile, console) := scalacOptions.value.filterNot(_.contains("-Ywarn-unused"))

initialCommands in console := """
import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._
import com.github.mrdziuban.moultingyaml.shapeless._
"""

lazy val project = Project("moultingyaml-shapeless", file("."))
  .settings(Seq(
    organization := "com.github.mrdziuban",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "net.jcazevedo" %% "moultingyaml" % "0.4.0",
      "com.chuusai" %% "shapeless" % "2.3.3"
    ),
    homepage := Some(url("https://github.com/mrdziuban/moultingyaml-shapeless")),
    scmInfo := Some(ScmInfo(url("https://github.com/mrdziuban/moultingyaml-shapeless"),
                           "git@github.com:mrdziuban/moultingyaml-shapeless.git")),
    developers := List(Developer("mrdziuban", "Matt Dziuban", "mrdziuban@gmail.com", url("https://github.com/mrdziuban"))),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    publishMavenStyle := true,
    publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)
  ))
