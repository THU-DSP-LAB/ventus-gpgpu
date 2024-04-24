// import Mill dependency
import os.Path
import mill._
import mill.define.Sources
import mill.modules.Util
import scalalib._
// support BSP
import mill.bsp._
import publish._
import coursier.maven.MavenRepository
// input build.sc from each repositories.
//import $file.dependencies.chisel3.build
//import $file.dependencies.firrtl.build
//import $file.dependencies.treadle.build
//import $file.dependencies.`chisel-testers2`.build
import $file.dependencies.cde.build
import $file.dependencies.`rocket-chip`.common
import $file.dependencies.`berkeley-hardfloat`.build
import $file.dependencies.`rocket-chip`.`api-config-chipsalliance`.`build-rules`.mill.build
import $file.dependencies.`fpuv2`.build

// Global Scala Version
object ivys {
  val sv = "2.12.15"
  val upickle = ivy"com.lihaoyi::upickle:1.3.15"
  val oslib = ivy"com.lihaoyi::os-lib:0.7.8"
  val pprint = ivy"com.lihaoyi::pprint:0.6.6"
  val utest = ivy"com.lihaoyi::utest:0.7.10"
  val macroParadise = ivy"org.scalamacros:::paradise:2.1.1"
  val jline = ivy"org.scala-lang.modules:scala-jline:2.12.1"
  val scalatest = ivy"org.scalatest::scalatest:3.2.2"
  val scalatestplus = ivy"org.scalatestplus::scalacheck-1-14:3.1.1.1"
  val scalacheck = ivy"org.scalacheck::scalacheck:1.14.3"
  val scopt = ivy"com.github.scopt::scopt:3.7.1"
  val playjson =ivy"com.typesafe.play::play-json:2.6.10"
  val spire = ivy"org.typelevel::spire:0.16.2"
  val breeze = ivy"org.scalanlp::breeze:1.1"

  val chisel3 = ivy"edu.berkeley.cs::chisel3:3.5.0"
  val chisel3Plugin = ivy"edu.berkeley.cs:::chisel3-plugin:3.5.0"
  val chiseltest = ivy"edu.berkeley.cs::chiseltest:0.5.0"
  val chiselCirct = ivy"com.sifive::chisel-circt:0.4.0"
}

object helper {
  val isMac = System.getProperty("os.name").toLowerCase.startsWith("mac")
}

// For modules not support mill yet, need to have a ScalaModule depend on our own repositories.
trait CommonModule extends ScalaModule {
  // def repositoriesTask = T.task { super.repositoriesTask() ++ Seq(
  //   MavenRepository("https://maven.aliyun.com/repository/central")
  // ) }

  override def scalaVersion = ivys.sv

  override def ivyDeps = Agg(
    ivys.chisel3,
    ivys.chiseltest,
    ivys.chiselCirct
  )

  override def compileIvyDeps = Agg(ivys.macroParadise)

  override def scalacPluginIvyDeps = Agg(
    ivys.macroParadise,
    ivys.chisel3Plugin
  )

  override def scalacOptions = Seq("-Xsource:2.11")
}

object mycde extends dependencies.cde.build.cde(ivys.sv) with PublishModule {
  // def repositoriesTask = T.task { super.repositoriesTask() ++ Seq(
  //   MavenRepository("https://maven.aliyun.com/repository/central")
  // ) }
  override def millSourcePath = os.pwd /  "dependencies" / "cde" / "cde"
}

object myrocketchip extends dependencies.`rocket-chip`.common.CommonRocketChip {
  // def repositoriesTask = T.task { super.repositoriesTask() ++ Seq(
  //   MavenRepository("https://maven.aliyun.com/repository/central")
  // ) }
  // TODO: FIX
  override def scalacOptions = T {
    Seq("-Xsource:2.11")
  }

  override def millSourcePath = os.pwd /  "dependencies" / "rocket-chip"

  override def scalaVersion = ivys.sv

  def hardfloatModule: PublishModule = myhardfloat

  def configModule: PublishModule = mycde
}

object inclusivecache extends CommonModule {
  // TODO: FIX
  override def scalacOptions = T {
    super.scalacOptions() ++ Agg("-Xsource:2.11")
  }

  override def millSourcePath = os.pwd / "dependencies" / "rocket-chip-inclusive-cache" / 'design / 'craft / "inclusivecache"

  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip)
}

object myhardfloat extends dependencies.`berkeley-hardfloat`.build.hardfloat {
  // def repositoriesTask = T.task { super.repositoriesTask() ++ Seq(
  //   MavenRepository("https://maven.aliyun.com/repository/central")
  // ) }

  override def millSourcePath = os.pwd /  "dependencies" / "berkeley-hardfloat"

  override def scalaVersion = ivys.sv

  def chisel3PluginIvyDeps = Agg(ivys.chisel3Plugin)
}

object ventus extends CommonModule {

  override def forkArgs = Seq("-Xmx32G", "-Xss192m")

  override def scalacOptions = Seq(
    "-Xsource:2.11",
    "-language:reflectiveCalls",
    //"-deprecation",
    //"-feature",
    "-Xcheckinit"//,
    // Enables autoclonetype2 in 3.4.x (on by default in 3.5)
    //"-P:chiselplugin:useBundlePlugin"
  )
  
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, inclusivecache, dependencies.`fpuv2`.build.fpuv2)

  // add some scala ivy module you like here.
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivys.oslib,
    ivys.pprint
  )

  // use scalatest as your test framework
  object tests extends Tests with TestModule.ScalaTest {
    override def forkArgs = Seq("-Xmx32G", "-Xss192m")
    override def moduleDeps = super.moduleDeps ++ Seq(dependencies.`fpuv2`.build.fpuv2.test)
    override def ivyDeps = super.ivyDeps() ++ Agg(
      ivys.chiseltest,
      ivys.scalatest
    )
  }
}
