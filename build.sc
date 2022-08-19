// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import scalalib._
// support BSP
import mill.bsp._
// input build.sc from each repositories.
import $file.dependencies.chisel3.build
import $file.dependencies.firrtl.build
import $file.dependencies.treadle.build
import $file.dependencies.`chisel-testers2`.build
import $file.dependencies.cde.build
import $file.dependencies.`berkeley-hardfloat`.build
import $file.dependencies.`rocket-chip`.common

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
}

object helper {
  val isMac = System.getProperty("os.name").toLowerCase.startsWith("mac")
}

// For modules not support mill yet, need to have a ScalaModule depend on our own repositories.
trait CommonModule extends ScalaModule {
  override def scalaVersion = ivys.sv

  override def scalacPluginClasspath = T { super.scalacPluginClasspath() ++ Agg(
    mychisel3.plugin.jar()
  ) }

  override def scalacOptions = T {
    super.scalacOptions() ++ Agg(s"-Xplugin:${mychisel3.plugin.jar().path}", "-P:chiselplugin:genBundleElements")
  }

  override def moduleDeps: Seq[ScalaModule] = Seq(mychisel3)

  override def compileIvyDeps = Agg(ivys.macroParadise)

  override def scalacPluginIvyDeps = Agg(ivys.macroParadise)
}


// Chips Alliance

object myfirrtl extends dependencies.firrtl.build.firrtlCrossModule(ivys.sv) {
  override def millSourcePath = os.pwd / "dependencies" / "firrtl"
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivys.pprint
  )
  override val checkSystemAntlr4Version = false
  override val checkSystemProtocVersion = false
  override val protocVersion = os.proc("protoc", "--version").call().out.text.dropRight(1).split(' ').last
  override val antlr4Version = os.proc("antlr4").call().out.text.split('\n').head.split(' ').last
}

object mychisel3 extends dependencies.chisel3.build.chisel3CrossModule(ivys.sv) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel3"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)

  def treadleModule: Option[PublishModule] = Some(mytreadle)

  def chiseltestModule: Option[PublishModule] = Some(mychiseltest)
}

object mytreadle extends dependencies.treadle.build.treadleCrossModule(ivys.sv) {
  override def millSourcePath = os.pwd /  "dependencies" / "treadle"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
}

object mycde extends dependencies.cde.build.cde(ivys.sv) with PublishModule {
  override def millSourcePath = os.pwd /  "dependencies" / "cde" / "cde"
}

object myrocketchip extends dependencies.`rocket-chip`.common.CommonRocketChip {
  // TODO: FIX
  override def scalacOptions = T {
    Seq("-Xsource:2.11", s"-Xplugin:${mychisel3.plugin.jar().path}", "-P:chiselplugin:genBundleElements")
  }

  override def scalacPluginClasspath = T { super.scalacPluginClasspath() ++ Agg(
    mychisel3.plugin.jar()
  ) }

  override def millSourcePath = os.pwd /  "dependencies" / "rocket-chip"

  override def scalaVersion = ivys.sv

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

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

// UCB

object mychiseltest extends dependencies.`chisel-testers2`.build.chiseltestCrossModule(ivys.sv) {
  override def millSourcePath = os.pwd /  "dependencies" / "chisel-testers2"

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  def treadleModule: Option[PublishModule] = Some(mytreadle)
}

object myhardfloat extends dependencies.`berkeley-hardfloat`.build.hardfloat {
  override def millSourcePath = os.pwd /  "dependencies" / "berkeley-hardfloat"

  override def scalaVersion = ivys.sv

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  override def scalacPluginClasspath = T { super.scalacPluginClasspath() ++ Agg(
    mychisel3.plugin.jar()
  ) }

  override def scalacOptions = T {
    Seq("-Xsource:2.11", s"-Xplugin:${mychisel3.plugin.jar().path}", "-P:chiselplugin:genBundleElements")
  }
}

object ventus extends CommonModule {
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, inclusivecache, mychiseltest)

  // add some scala ivy module you like here.
  override def ivyDeps = Agg(
    ivys.oslib,
    ivys.pprint
  )

  // use scalatest as your test framework
  object tests extends Tests with TestModule.ScalaTest {
    override def ivyDeps = Agg(
      ivys.scalatest
    )
    override def moduleDeps = super.moduleDeps ++ Seq(mychiseltest)
  }
}
