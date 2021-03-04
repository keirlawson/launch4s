val scala213 = "2.13.5"
val scala212 = "2.12.13"

ThisBuild / scalaVersion     := scala213
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.github.keirlawson"

lazy val root = (project in file("."))
  .settings(
    name := "Launch4s",
    libraryDependencies ++= Seq(
      "com.launchdarkly" % "launchdarkly-java-server-sdk" % "5.2.0",
      "org.typelevel" %% "cats-effect" % "2.3.0",
      "org.typelevel" %% "cats-core" % "2.3.0",
      "co.fs2" %% "fs2-core" % "2.4.6"
    ),
    crossScalaVersions := List(scala212, scala213),
    releaseCrossBuild := true
  )

publishTo := sonatypePublishToBundle.value
publishMavenStyle := true
sonatypeProfileName := "keirlawson"

homepage := Some(url("https://github.com/keirlawson/launch4s"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/keirlawson/launch4s"),
    "scm:git@github.com:keirlawson/launch4s.git"
  )
)
developers := List(
  Developer(id="keirlawson", name="Keir Lawson", email="keirlawson@gmail.com", url=url("https://github.com/keirlawson/"))
)

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
) 
