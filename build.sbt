import Dependencies._
import FiloSettings._

// Build-level setting across all subprojects
ThisBuild / scalaVersion := "2.11.12"
ThisBuild / organization := "org.filodb"
ThisBuild / organizationName := "FiloDB"
ThisBuild / publishMavenStyle := true
ThisBuild / Test / publishArtifact := false
ThisBuild / IntegrationTest / publishArtifact := false
ThisBuild / licenses += ("Apache-2.0", url("http://choosealicense.com/licenses/apache/"))
ThisBuild / pomIncludeRepository := { x => false }

// root project settings
publishTo := Some(Resolver.file("Unused repo", file("target/unusedrepo")))
publish / skip := true
crossScalaVersions := Vector.empty

/**
  * FiloDB modules and dependencies
  */

lazy val memory = (project in file("memory"))
  .settings(
    commonSettings,
    assemblySettings,
    name := "filodb-memory",
    scalacOptions += "-language:postfixOps",
    libraryDependencies ++= memoryDeps
  )

lazy val core = (project in file("core"))
  .dependsOn(memory % "compile->compile; test->test")
  .settings(
    commonSettings,
    name := "filodb-core",
    scalacOptions += "-language:postfixOps",
    libraryDependencies ++= coreDeps
  )

lazy val coordinator = (project in file("coordinator"))
  .dependsOn(core % "compile->compile; test->test")
  .dependsOn(query % "compile->compile; test->test")
  .dependsOn(prometheus % "compile->compile; test->test")
  .configs(MultiJvm)
  .settings(
    commonSettings,
    multiJvmSettings,
    testMultiJvmToo,
    name := "filodb-coordinator",
    libraryDependencies ++= coordDeps,
    libraryDependencies +=
      "com.typesafe.akka" %% "akka-contrib" % akkaVersion exclude(
        "com.typesafe.akka", s"akka-persistence-experimental_${scalaBinaryVersion.value}")
  )

lazy val prometheus = (project in file("prometheus"))
  .dependsOn(core % "compile->compile; test->test")
  .dependsOn(query % "compile->compile; test->test")
  .settings(
    commonSettings,
    name := "filodb-prometheus",
    libraryDependencies ++= promDeps
  )

lazy val query = (project in file("query"))
  .dependsOn(core % "compile->compile; test->test")
  .settings(
    commonSettings,
    name := "filodb-query",
    libraryDependencies ++= queryDeps,
    scalacOptions += "-language:postfixOps"
  )

lazy val cassandra = (project in file("cassandra"))
  .dependsOn(core % "compile->compile; test->test", coordinator)
  .settings(
    commonSettings,
    name := "filodb-cassandra",
    libraryDependencies ++= cassDeps
  )

lazy val cli = (project in file("cli"))
  .dependsOn(core, coordinator % "test->test", cassandra)
  .dependsOn(prometheus % "compile->compile; test->test")
  .settings(
    commonSettings,
    name := "filodb-cli",
    libraryDependencies ++= cliDeps,
    cliAssemblySettings
  )

lazy val kafka = (project in file("kafka"))
  .dependsOn(
    core % "compile->compile; it->test",
    coordinator % "compile->compile; test->test")
  .configs(IntegrationTest, MultiJvm)
  .settings(
    name := "filodb-kafka",
    commonSettings,
    kafkaSettings,
    itSettings,
    assemblySettings,
    libraryDependencies ++= kafkaDeps
  )

lazy val sparkJobs = (project in file("spark-jobs"))
  .dependsOn(cassandra, core % "compile->compile; test->test")
  .settings(commonSettings: _*)
  .settings(name := "spark-jobs")
  .settings(scalacOptions += "-language:postfixOps")
  .settings(libraryDependencies ++= sparkJobsDeps)

lazy val bootstrapper = (project in file("akka-bootstrapper"))
  .configs(MultiJvm)
  .settings(commonSettings: _*)
  .settings(multiJvmMaybeSettings: _*)
  .settings(name := "akka-bootstrapper")
  .settings(libraryDependencies ++= bootstrapperDeps)

lazy val http = (project in file("http"))
  .dependsOn(core, coordinator % "compile->compile; test->test")
  .settings(commonSettings: _*)
  .settings(name := "http")
  .settings(libraryDependencies ++= httpDeps)

lazy val standalone = (project in file("standalone"))
  .dependsOn(core, prometheus % "test->test", coordinator % "compile->compile; test->test",
    cassandra, kafka, http, bootstrapper, sparkJobs, gateway % Test)
  .configs(MultiJvm)
  .settings(commonSettings: _*)
  .settings(multiJvmMaybeSettings: _*)
  .settings(assemblySettings: _*)
  .settings(libraryDependencies ++= standaloneDeps)

// standalone does not depend on spark-jobs, but the idea is to simplify packaging and versioning

// lazy val spark = (project in file("spark"))
//   .dependsOn(core % "compile->compile; test->test; it->test",
//      coordinator % "compile->compile; test->test",
//      cassandra % "compile->compile; test->test; it->test")
//   .configs(IntegrationTest)
//   .settings(
//      name := "filodb-spark",
//      commonSettings,
//      libraryDependencies ++= sparkDeps,
//      itSettings,
//      jvmPerTestSettings,
//      assemblyExcludeScala,
//      // Disable tests for now since lots of work remaining to enable Spark
//      test := {}
//    )

lazy val jmh = (project in file("jmh"))
  .enablePlugins(JmhPlugin)
  .dependsOn(core % "compile->compile; compile->test", gateway)
  .settings(
    commonSettings,
    name := "filodb-jmh",
    libraryDependencies ++= jmhDeps,
    publish / skip := true
  )

// lazy val stress = (project in file("stress"))
//   .dependsOn(spark)
//   .settings(
//     commonSettings,
//     name := "filodb-stress",
//     libraryDependencies ++= stressDeps,
//     assemblyExcludeScala
//   )

lazy val gateway = (project in file("gateway"))
  .dependsOn(coordinator % "compile->compile; test->test",
    prometheus, cassandra)
  .settings(
    commonSettings,
    name := "filodb-gateway",
    libraryDependencies ++= gatewayDeps,
    gatewayAssemblySettings
  )
