val http4sVersion = "1.0.0-M46"
val log4catsVersion = "2.7.1"
lazy val root = (project in file("."))
  .settings(
    name := "collatex-reptilian",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "3.8.1",
    assemblyJarName := s"collatex-reptilian-0.1.0-SNAPSHOT.jar",
    fork := true, // Enable forking for all run tasks
    Compile / run / fork := true, // Ensure forking for run
    Compile / run / javaOptions += "-Dcats.effect.warnOnNonMainThreadDetected=false", // Suppress IOApp warning
    Global / excludeLintKeys += (Compile / runMain / fork), // Suppress unused key warning
    Compile / resourceDirectories -= baseDirectory.value / "src" / "main" / "outputs",
    Compile / resourceDirectories -= baseDirectory.value / "src" / "main" / "mockups",
    Compile / resourceDirectories -= baseDirectory.value / "src" / "main" / "docs" / "refs",
    assembly / mainClass := None, // Some("net.collatex.reptilian.manifest"),
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Wvalue-discard",
      "-Wunused:imports",
      "-Wunused:privates",
      "-Wunused:params",
      "-Wunused:patvars",
      "-Wunused:implicits",
      "-Wunused:locals" // can change to "all" to add unchecked, deprecation, and feature
    ),
    libraryDependencies ++= Seq(
      "org.relaxng" % "jing" % "20241231",
      "com.lihaoyi" %% "os-lib" % "0.11.6",
      "com.lihaoyi" %% "upickle" % "4.4.2",
      "com.github.haifengl" %% "smile-scala" % "5.1.0",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "com.lihaoyi" %% "scalatags" % "0.13.1",
      "de.sciss" %% "fingertree" % "1.5.5",
      "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
      "io.github.pityka" %% "pairwisealignment" % "2.2.7",
      "de.sciss" %% "linkernighantsp" % "0.1.3",
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.scala-lang.modules" %% "scala-collection-contrib" % "0.4.0",
      "net.sf.saxon" % "Saxon-HE" % "12.9",
      "com.lihaoyi" %% "requests" % "0.9.2",
      "com.networknt" % "json-schema-validator" % "3.0.0",
      "org.virtuslab" %% "scala-yaml" % "0.3.1",
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-server" % http4sVersion,
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      "ch.qos.logback" % "logback-classic" % "1.5.25"
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", _ @_*) => MergeStrategy.concat
      case PathList("META-INF", _ @_*)             => MergeStrategy.discard
      case _                                       => MergeStrategy.first
    }
  )
