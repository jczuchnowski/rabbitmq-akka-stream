name := "rabbitmq-akka-stream"

version := "1.0"

organization := "io.scalac"

scalaVersion := "2.10.3"

resolvers ++= Seq(
  "snapshots"           at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"            at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

EclipseKeys.withSource := true

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion = "2.3.2"
  Seq(
    "com.typesafe.akka"       %%  "akka-stream-experimental" % "0.2",
    "com.rabbitmq"            %   "amqp-client"     % "3.3.1",
    "org.slf4j"               %   "slf4j-api"       % "1.7.6",
    "ch.qos.logback"          %   "logback-core"    % "1.1.1",
    "ch.qos.logback"          %   "logback-classic" % "1.1.1",
    "com.typesafe.akka"       %%  "akka-actor"      % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"      % akkaVersion,
    "com.typesafe.akka"       %%  "akka-testkit"    % akkaVersion % "test",
    "org.scalatest"           %%  "scalatest"       % "2.1.6" % "test"
  )
}
