name := "HOTS-extra matchmaker"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

lazy val scalazversion = "7.1.5"

lazy val h4sversion = "0.11.2"

libraryDependencies ++= Seq(
  //"org.scalaz"        %% "scalaz-core"         % scalazversion,
  //"org.scalaz"        %% "scalaz-concurrent"   % scalazversion,
  //"org.scalaz.stream" %% "scalaz-stream"       % "0.8",
  "org.http4s"        %% "http4s-dsl"          % h4sversion,
  "org.http4s"        %% "http4s-blaze-server" % h4sversion,
  "org.http4s"        %% "http4s-twirl"        % h4sversion,
  "org.http4s"        %% "http4s-argonaut"     % h4sversion,
  //"io.argonaut"       %% "argonaut"            % "6.0.4",
  "org.clapper"       %% "grizzled-scala"      % "1.4.0",
  "com.chuusai" %% "shapeless" % "2.2.4"
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "1")

scalacOptions in Test ++= Seq("-Yrangepos")

scalacOptions ++= Seq(
  "-deprecation",           
  "-encoding", "UTF-8",       // yes, this is 2 args
  "-feature",                
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  //"-Xfatal-warnings",       //disabled for Twirl 
  "-Xlint",
  "-Yno-adapted-args",       
  "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole
  "-Ywarn-numeric-widen",   
  "-Ywarn-value-discard",
  "-Xfuture",
  //"-Xlog-implicits",
  "-Ywarn-unused-import"     // 2.11 only
)

scalariformSettings

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)