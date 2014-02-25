name := "couch-play-plugin"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test",
  "org.scala-lang.modules" %% "scala-async" % "0.9.0-M4"
  )


play.Project.playScalaSettings

Keys.fork in Test := false
