import sbt._
import Keys._
import sbt.inc.Analysis
import util.Properties

object BuildSettings {
  val buildVersion = "3.0"
  val buildScalaVersion = "2.11.2"
  val buildScalaOrganization = "org.scala-lang"
  val buildParadiseVersion = "2.0.1"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    scalaOrganization := buildScalaOrganization,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.scalamacros" % "paradise" % buildParadiseVersion cross CrossVersion.full)
  )
}

object Dependencies {
  val scalatest =  "org.scalatest" %% "scalatest" % "2.1.5" //% "test"
  val reflect = BuildSettings.buildScalaOrganization % "scala-reflect" % BuildSettings.buildScalaVersion
}


object MyBuild extends Build {
  import BuildSettings._
  import Dependencies._

  lazy val main = Project(
    "playground", 
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in test,
      libraryDependencies += scalatest
    )) dependsOn(macros) aggregate(macros)

  lazy val macros = Project(
    "macros", 
    file("macros"),
    settings = buildSettings ++ Seq(
      name := "macros",
      libraryDependencies += reflect
    ))
}