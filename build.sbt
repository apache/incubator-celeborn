// scalastyle:off line.size.limit

import java.nio.file.Files
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Locale

import sbtprotoc.ProtocPlugin.autoImport._

import sbt.Keys.streams

// Scala versions
val scala212 = "2.12.15"
val scala213 = "2.13.5"
val default_scala_version = scala212
val all_scala_versions = Seq(scala212, scala213)

// Dependent library versions
val sparkVersion = "3.4.0"
val flinkVersion = "1.16.1"
val hadoopVersion = "3.3.1"
val scalaTestVersion = "3.2.15"
val scalaTestVersionForConnectors = "3.0.8"
val parquet4sVersion = "1.9.4"

// Versions for Hive 3
val hadoopVersionForHive3 = "3.1.0"
val hiveVersion = "3.1.2"
val tezVersion = "0.9.2"

// Versions for Hive 2
val hadoopVersionForHive2 = "2.7.2"
val hive2Version = "2.3.3"
val tezVersionForHive2 = "0.8.4"

// Versions for proto
val protocVersion = "3.19.2"
val protoVersion = "3.19.2"

scalaVersion := default_scala_version

// crossScalaVersions must be set to Nil on the root project
crossScalaVersions := Nil

lazy val commonSettings = Seq(
  organization := "org.apache.celeborn",
  scalaVersion := default_scala_version,
  crossScalaVersions := all_scala_versions,
  fork := true,
  scalacOptions ++= Seq("-target:jvm-1.8"),
  javacOptions ++= Seq("-encoding", UTF_8.name(), "-source", "1.8"),

  // -target cannot be passed as a parameter to javadoc. See https://github.com/sbt/sbt/issues/355
  Compile / compile / javacOptions ++= Seq("-target", "1.8"),

  // Make sure any tests in any project that uses Spark is configured for running well locally
  Test / javaOptions ++= Seq(
    "-Xmx2048m"
  ),

  testOptions += Tests.Argument("-oF"),
)

lazy val protoSettings = Seq(
  // Setting version for the protobuf compiler
  PB.protocVersion := protocVersion,
  // set proto sources path
  Compile / PB.protoSources := Seq(sourceDirectory.value / "main" / "proto"),
  Compile / PB.targets := Seq(PB.gens.java -> (Compile / sourceManaged).value)
)

lazy val common = (project in file("common"))
  .settings (
    name := "celeborn-common",
    commonSettings,
    protoSettings,
    libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-library" % "2.12.15",
        "com.google.protobuf" % "protobuf-java" % "3.19.2" % "protobuf",
        "org.apache.ratis" % "ratis-common" % "2.5.1",
        "org.apache.ratis" % "ratis-client" % "2.5.1",
        "io.dropwizard.metrics" % "metrics-core" % "3.2.6",
        "io.dropwizard.metrics" % "metrics-graphite" % "3.2.6",
        "io.dropwizard.metrics" % "metrics-jvm" % "3.2.6",
        "org.yaml" % "snakeyaml" % "1.33",
        "org.slf4j" % "slf4j-api" % "1.7.36",
        "org.slf4j" % "jul-to-slf4j" % "1.7.36",
        "org.slf4j" % "jcl-over-slf4j" % "1.7.36",
        "commons-io" % "commons-io" % "2.13.0",
        "org.apache.commons" % "commons-crypto" % "1.0.0",
        "org.apache.commons" % "commons-lang3" % "3.12.0",
        "io.netty" % "netty-all" % "4.1.93.Final",
        "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
        "com.google.code.findbugs" % "jsr305" % "1.3.9",
        "com.google.guava" % "guava" % "14.0.1",
        "org.scala-lang" % "scala-reflect" % "2.12.15",
        "org.apache.hadoop" % "hadoop-client-api" % "3.2.4",
        "org.apache.hadoop" % "hadoop-client-runtime" % "3.2.4",
        "org.roaringbitmap" % "RoaringBitmap" % "0.9.32",
        "org.mockito" % "mockito-core" % "4.11.0" % "test",
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.2" % "test",
        "org.apache.logging.log4j" % "log4j-1.2-api" % "2.17.2" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.scalatest" %% "scalatest" % "3.2.16" % "test",


      // Compiler plugins
      // -- Bump up the genjavadoc version explicitly to 0.18 to work with Scala 2.12
      compilerPlugin(
        "com.typesafe.genjavadoc" %% "genjavadoc-plugin" % "0.18" cross CrossVersion.full)
    ),

    Test / testOptions += Tests.Argument("-oDF"),
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),

    // Don't execute in parallel since we can't have multiple Sparks in the same JVM
    Test / parallelExecution := false,

    scalacOptions ++= Seq(
      "-P:genjavadoc:strictVisibility=true" // hide package private types and methods in javadoc
    ),

    javaOptions += "-Xmx2048m",

    // Configurations to speed up tests and reduce memory footprint
    Test / javaOptions ++= Seq(
      "-Dspark.ui.enabled=false",
      "-Xmx2048m"
    ),

    Test / envVars += ("IS_TESTING", "1"),

    // Generate the package object to provide the version information in runtime.
    Compile / sourceGenerators += Def.task {
      val file = (Compile / sourceManaged).value / "org" / "apache" / "celeborn" / "package.scala"
      streams.value.log.info("geneate version information file %s".format(file.toPath))
      IO.write(file,
        s"""package org.apache
           |
           |package object celeborn {
           |  val VERSION = "${version.value}"
           |}
           |""".stripMargin)
      Seq(file)
      // generate version task depends on PB generate to avoid concurrency generate source files
      // otherwise we may encounter the error:
      // ```
      //   [error] IO error while decoding ./celeborn/common/target/scala-2.12/src_managed/main/org/apache/celeborn/package.scala with UTF-8: ./celeborn/common/target/scala-2.12/src_managed/main/org/apache/celeborn/package.scala (No such file or directory)
      // ```
    }.dependsOn(Compile / PB.generate)
  )

lazy val client = (project in file("client"))
  .dependsOn(common)
  .settings (
    name := "client",
    commonSettings,
    libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-library" % "2.12.18",
        "io.netty" % "netty-all" % "4.1.93.Final",
        "com.google.guava" % "guava" % "14.0.1",
        "org.lz4" % "lz4-java" % "1.8.0",
        "com.github.luben" % "zstd-jni" % "1.5.2-1",
        "org.apache.commons" % "commons-lang3" % "3.12.0",
        "org.mockito" % "mockito-core" % "4.11.0" % "test",
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.2" % "test",
        "org.apache.logging.log4j" % "log4j-1.2-api" % "2.17.2" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.scalatest" %% "scalatest" % "3.2.16" % "test",

      // Compiler plugins
      // -- Bump up the genjavadoc version explicitly to 0.18 to work with Scala 2.12
      compilerPlugin(
        "com.typesafe.genjavadoc" %% "genjavadoc-plugin" % "0.18" cross CrossVersion.full)
    ),

    Test / testOptions += Tests.Argument("-oDF"),
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),

    // Don't execute in parallel since we can't have multiple Sparks in the same JVM
    Test / parallelExecution := false,

    scalacOptions ++= Seq(
      "-P:genjavadoc:strictVisibility=true" // hide package private types and methods in javadoc
    ),

    javaOptions += "-Xmx2048m",

    // Configurations to speed up tests and reduce memory footprint
    Test / javaOptions ++= Seq(
      "-Xmx2048m"
    ),

    Test / envVars += ("IS_TESTING", "1"),

  )

lazy val service = (project in file("service"))
  .dependsOn(common)
  .settings (
    name := "service",
    commonSettings,
    libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-library" % "2.12.18",
        "org.slf4j" % "slf4j-api" % "1.7.36",
        "io.netty" % "netty-all" % "4.1.93.Final",
        "javax.servlet" % "javax.servlet-api" % "3.1.0",
        "commons-io" % "commons-io" % "2.13.0",
        "org.apache.commons" % "commons-crypto" % "1.0.0",
        "com.google.code.findbugs" % "jsr305" % "1.3.9",
        "org.mockito" % "mockito-core" % "4.11.0" % "test",
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.2" % "test",
        "org.apache.logging.log4j" % "log4j-1.2-api" % "2.17.2" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.scalatest" %% "scalatest" % "3.2.16" % "test",

      // Compiler plugins
      // -- Bump up the genjavadoc version explicitly to 0.18 to work with Scala 2.12
      compilerPlugin(
        "com.typesafe.genjavadoc" %% "genjavadoc-plugin" % "0.18" cross CrossVersion.full)
    ),

    Test / testOptions += Tests.Argument("-oDF"),
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),

    // Don't execute in parallel since we can't have multiple Sparks in the same JVM
    Test / parallelExecution := false,

    scalacOptions ++= Seq(
      "-P:genjavadoc:strictVisibility=true" // hide package private types and methods in javadoc
    ),

    javaOptions += "-Xmx2048m",

    // Configurations to speed up tests and reduce memory footprint
    Test / javaOptions ++= Seq(
      "-Xmx2048m"
    ),

    Test / envVars += ("IS_TESTING", "1"),

  )

lazy val master = (project in file("master"))
  .dependsOn(common, service)
  .settings (
    name := "master",
    commonSettings,
    protoSettings,
    libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-library" % "2.12.18",
        "com.google.protobuf" % "protobuf-java" % "3.19.2",
        "io.netty" % "netty-all" % "4.1.93.Final",
        "com.google.guava" % "guava" % "14.0.1",
        "org.apache.ratis" % "ratis-common" % "2.5.1",
        "org.apache.ratis" % "ratis-client" % "2.5.1",
        "org.apache.ratis" % "ratis-server" % "2.5.1",
        "org.apache.ratis" % "ratis-netty" % "2.5.1",
        "org.apache.ratis" % "ratis-grpc" % "2.5.1",
        "org.apache.ratis" % "ratis-shell" % "2.5.1",
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.2",
        "org.apache.logging.log4j" % "log4j-1.2-api" % "2.17.2",
        "org.mockito" % "mockito-core" % "4.11.0" % "test",
        "org.apache.hadoop" % "hadoop-client-api" % "3.2.4",
        "junit" % "junit" % "4.12" % "test",
        "org.scalatest" %% "scalatest" % "3.2.16" % "test",

      // Compiler plugins
      // -- Bump up the genjavadoc version explicitly to 0.18 to work with Scala 2.12
      compilerPlugin(
        "com.typesafe.genjavadoc" %% "genjavadoc-plugin" % "0.18" cross CrossVersion.full)
    ),

    Test / testOptions += Tests.Argument("-oDF"),
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),

    // Don't execute in parallel since we can't have multiple Sparks in the same JVM
    Test / parallelExecution := false,

    scalacOptions ++= Seq(
      "-P:genjavadoc:strictVisibility=true" // hide package private types and methods in javadoc
    ),

    javaOptions += "-Xmx2048m",

    // Configurations to speed up tests and reduce memory footprint
    Test / javaOptions ++= Seq(
      "-Xmx2048m"
    ),

    Test / envVars += ("IS_TESTING", "1"),

  )

lazy val sparkCommon = (project in file("client-spark/common"))
  .dependsOn(common, client)
  .settings (
    name := "spark-common",
    commonSettings,
    libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-library" % "2.12.18",
        "org.apache.spark" %% "spark-core" % "3.3.2" % "provided",
        "org.apache.spark" %% "spark-sql" % "3.3.2" % "provided",
        "org.mockito" % "mockito-core" % "4.11.0" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.scalatest" %% "scalatest" % "3.2.16" % "test",

      // Compiler plugins
      // -- Bump up the genjavadoc version explicitly to 0.18 to work with Scala 2.12
      compilerPlugin(
        "com.typesafe.genjavadoc" %% "genjavadoc-plugin" % "0.18" cross CrossVersion.full)
    ),

    Test / testOptions += Tests.Argument("-oDF"),
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),

    // Don't execute in parallel since we can't have multiple Sparks in the same JVM
    Test / parallelExecution := false,

    scalacOptions ++= Seq(
      "-P:genjavadoc:strictVisibility=true" // hide package private types and methods in javadoc
    ),

    javaOptions += "-Xmx2048m",

    // Configurations to speed up tests and reduce memory footprint
    Test / javaOptions ++= Seq(
      "-Xmx2048m"
    ),

    Test / envVars += ("IS_TESTING", "1"),
  )

lazy val spark3 = (project in file("client-spark/spark-3"))
  .dependsOn(common, client, sparkCommon)
  .settings (
    name := "celeborn-client-spark-3",
    commonSettings,
    libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-library" % "2.12.18",
        "org.apache.spark" %% "spark-core" % "3.3.2" % "provided",
        "org.apache.spark" %% "spark-sql" % "3.3.2" % "provided",
        "org.mockito" % "mockito-core" % "4.11.0" % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.scalatest" %% "scalatest" % "3.2.16" % "test",

      // Compiler plugins
      // -- Bump up the genjavadoc version explicitly to 0.18 to work with Scala 2.12
      compilerPlugin(
        "com.typesafe.genjavadoc" %% "genjavadoc-plugin" % "0.18" cross CrossVersion.full)
    ),

    Test / testOptions += Tests.Argument("-oDF"),
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),

    // Don't execute in parallel since we can't have multiple Sparks in the same JVM
    Test / parallelExecution := false,

    scalacOptions ++= Seq(
      "-P:genjavadoc:strictVisibility=true" // hide package private types and methods in javadoc
    ),

    javaOptions += "-Xmx2048m",

    // Configurations to speed up tests and reduce memory footprint
    Test / javaOptions ++= Seq(
      "-Xmx2048m"
    ),

    Test / envVars += ("IS_TESTING", "1"),
  )


lazy val spark3Shaded = (project in file("client-spark/spark-3-shade"))
  .dependsOn(spark3)
  .settings (
    name := "celeborn-client-spark-3",
    commonSettings,

    (assembly / test) := { },

    (assembly / logLevel) := Level.Info,

    // Exclude `scala-library` from assembly.
    (assembly / assemblyPackageScala / assembleArtifact) := false,

    // Exclude `pmml-model-*.jar`, `scala-collection-compat_*.jar`,`jsr305-*.jar` and
    // `netty-*.jar` and `unused-1.0.0.jar` from assembly.
    (assembly / assemblyExcludedJars) := {
      val cp = (assembly / fullClasspath).value
      cp filter { v =>
        val name = v.data.getName
        // name.startsWith("pmml-model-") || name.startsWith("scala-collection-compat_") ||
        //  name.startsWith("jsr305-") || name.startsWith("netty-") || name == "unused-1.0.0.jar"
        !(name.startsWith("celeborn-") || name.startsWith("protobuf-java-") ||
          name.startsWith("guava-") || name.startsWith("netty-") || name.startsWith("commons-lang3-"))
      }
    },

    (assembly / assemblyShadeRules) := Seq(
      ShadeRule.rename("com.google.protobuf.**" -> "org.apache.celeborn.shaded.com.google.protobuf.@1").inAll,
      ShadeRule.rename("com.google.common.**" -> "org.apache.celeborn.shaded.com.google.common.@1").inAll,
      ShadeRule.rename("io.netty.**" -> "org.apache.celeborn.shaded.io.netty.@1").inAll,
      ShadeRule.rename("org.apache.commons.**" -> "org.apache.celeborn.shaded.org.apache.commons.@1").inAll
    ),

    (assembly / assemblyMergeStrategy) := {
      case m if m.toLowerCase(Locale.ROOT).endsWith("manifest.mf") => MergeStrategy.discard
      // Drop all proto files that are not needed as artifacts of the build.
      case m if m.toLowerCase(Locale.ROOT).endsWith(".proto") => MergeStrategy.discard
      case m if m.toLowerCase(Locale.ROOT).startsWith("meta-inf/native-image") => MergeStrategy.discard
      // Drop netty jnilib
      case m if m.toLowerCase(Locale.ROOT).endsWith(".jnilib") => MergeStrategy.discard
      // rename netty native lib
      case "META-INF/native/libnetty_transport_native_epoll_x86_64.so" => CustomMergeStrategy.rename( _ => "META-INF/native/liborg_apache_celeborn_shaded_netty_transport_native_epoll_x86_64.so" )
      case "META-INF/native/libnetty_transport_native_epoll_aarch_64.so" => CustomMergeStrategy.rename( _ => "META-INF/native/liborg_apache_celeborn_shaded_netty_transport_native_epoll_aarch_64.so" )
      case _ => MergeStrategy.first
    }
  )

ThisBuild / parallelExecution := false

ThisBuild / version := "0.4.0-SNAPSHOT"
