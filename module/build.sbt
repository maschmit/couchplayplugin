name := "time-counter"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)     

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test"
  )

play.Project.playScalaSettings
