name := "time-counter"

version := "0.2-SNAPSHOT"

libraryDependencies ++= Seq(
  cache
)     

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test",
  "couch-play-plugin" % "couch-play-plugin_2.10" % "0.2-SNAPSHOT"
  )

play.Project.playScalaSettings
