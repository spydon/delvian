name := "delvian"

scalaVersion := "2.11.6"

version := "0.1"

resolvers += "scalac repo" at "https://raw.githubusercontent.com/ScalaConsultants/mvn-repo/master/"

libraryDependencies ++= Seq("io.scalac" %% "slack-scala-bot-core" % "0.2.1")
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.0"