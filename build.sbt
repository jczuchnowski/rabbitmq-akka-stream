name := "rabbitmq-akka-stream"

version := "1.0"

organization := "io.scalac"

scalaVersion := "2.11.1"

resolvers ++= Seq(
  "snapshots"           at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"            at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion = "2.3.3"
  Seq(
    "com.typesafe.akka"          %%  "akka-stream-experimental" % "0.3",
    "com.rabbitmq"               %   "amqp-client"              % "3.3.1",
    "com.typesafe.scala-logging" %%  "scala-logging-slf4j"      % "2.1.2",
    "ch.qos.logback"             %   "logback-core"             % "1.1.2",
    "ch.qos.logback"             %   "logback-classic"          % "1.1.2",
    "com.typesafe.akka"          %%  "akka-actor"               % akkaVersion,
    "com.typesafe.akka"          %%  "akka-slf4j"               % akkaVersion,
    "com.typesafe.akka"          %%  "akka-testkit"             % akkaVersion % "test",
    "org.scalatest"              %%  "scalatest"                % "2.1.6" % "test"
  )
}
