// Import Mill dependencies
import mill._
import mill.scalalib._
import mill.scalalib.publish._
import coursier.maven.MavenRepository

// Import from each repository
import $file.dependencies.`rocket-chip`.common
import $file.dependencies.`rocket-chip`.dependencies.hardfloat.common
import $file.dependencies.`rocket-chip`.dependencies.diplomacy.common
import $file.dependencies.`rocket-chip`.dependencies.cde.common
import $file.dependencies.`berkeley-hardfloat`.common
import $file.dependencies.`fpuv2`.build
import $file.common

// Define Scala and Chisel versions
object v {
  val scalaVersions = Map(
    "6.4.0" -> "2.13.12",
    // "3.6.0" -> "2.13.10",
    // "3.5.0" -> "2.13.7",
  )
  val scalaReflect = Map(
    "6.4.0" -> ivy"org.scala-lang:scala-reflect:2.13.12",
    // "3.6.0" -> ivy"org.scala-lang:scala-reflect:2.13.10",
    // "3.5.0" -> ivy"org.scala-lang:scala-reflect:2.13.7",
  )
  val chiselCrossVersions = Map(
    "6.4.0" -> (ivy"org.chipsalliance::chisel:6.4.0", ivy"org.chipsalliance:::chisel-plugin:6.4.0", ivy"edu.berkeley.cs::chiseltest:6.0.0"),
    // "3.6.0" -> (ivy"edu.berkeley.cs::chisel3:3.6.0", ivy"edu.berkeley.cs:::chisel3-plugin:3.6.0", ivy"edu.berkeley.cs::chiseltest:0.6.2"),
    // "3.5.0" -> (ivy"edu.berkeley.cs::chisel3:3.5.0", ivy"edu.berkeley.cs:::chisel3-plugin:3.5.0", ivy"edu.berkeley.cs::chiseltest:0.5.0"),
  )
}

// Define berkeley-hardfloat module
object hardfloat extends Cross[Hardfloat](v.chiselCrossVersions.keys.toSeq)
trait Hardfloat
  extends millbuild.dependencies.`berkeley-hardfloat`.common.HardfloatModule
    with Cross.Module[String] {
  def chiselModule = None
  def chiselPluginJar = None
  def chiselVersion: String = crossValue
  def scalaVersion = v.scalaVersions(chiselVersion)
  def chiselIvy = Some(v.chiselCrossVersions(chiselVersion)._1)
  def chiselPluginIvy = Some(v.chiselCrossVersions(chiselVersion)._2)
  override def millSourcePath = os.pwd / "dependencies" / "berkeley-hardfloat" / "hardfloat"
}

// Define fpuv2 module
object fpuv2 extends Cross[FPUv2](v.chiselCrossVersions.keys.toSeq)
trait FPUv2 extends SbtModule
  with millbuild.dependencies.`fpuv2`.build.FPUv2Module {
  def chiselVersion: String = crossValue
  def scalaVersion = v.scalaVersions(chiselVersion)
  def chiselIvy = Some(v.chiselCrossVersions(chiselVersion)._1)
  def chiselPluginIvy = Some(v.chiselCrossVersions(chiselVersion)._2)
  override def millSourcePath = os.pwd / "dependencies" / "fpuv2"
  def fudianModule = fudian(crossValue)
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(chiselPluginIvy.get)

  object fudian extends Cross[FuDian](crossValue)
  trait FuDian extends SbtModule
    with millbuild.common.HasChisel with Cross.Module[String] {
    def chiselVersion: String = crossValue
    def scalaVersion = v.scalaVersions(chiselVersion)
    def chiselIvy = Some(v.chiselCrossVersions(chiselVersion)._1)
    def chiselPluginIvy = Some(v.chiselCrossVersions(chiselVersion)._2)
    override def millSourcePath = os.pwd / "dependencies" / "fpuv2" / "fudian"
  }
}



object rocketchip extends Cross[RocketChip](v.chiselCrossVersions.keys.toSeq)
trait RocketChip
  extends millbuild.dependencies.`rocket-chip`.common.RocketChipModule
    with SbtModule
    with Cross.Module[String] {
  def chiselVersion: String = crossValue
  def scalaVersion: T[String] = T(v.scalaVersions(chiselVersion))
  override def millSourcePath = os.pwd / "dependencies" / "rocket-chip"

  def chiselModule = None
  def chiselPluginJar = None
  def chiselIvy = Some(v.chiselCrossVersions(chiselVersion)._1)
  def chiselPluginIvy = Some(v.chiselCrossVersions(chiselVersion)._2)

  def macrosModule = macros(crossValue)
  def hardfloatModule = hardfloat(crossValue)
  def cdeModule = cde(crossValue)
  def diplomacyModule = diplomacy(crossValue)
  def mainargsIvy = ivy"com.lihaoyi::mainargs:0.5.0"
  def json4sJacksonIvy = ivy"org.json4s::json4s-jackson:4.0.5"

  object macros extends Cross[Macros](crossValue)
  trait Macros
    extends millbuild.dependencies.`rocket-chip`.common.MacrosModule
      with SbtModule with Cross.Module[String] {
    def scalaVersion: T[String] = T(v.scalaVersions(crossValue))
    def scalaReflectIvy = v.scalaReflect(crossValue)
  }

  object hardfloat extends Cross[Hardfloat](crossValue)
  trait Hardfloat
    extends millbuild.dependencies.`rocket-chip`.dependencies.hardfloat.common.HardfloatModule
      with Cross.Module[String] {
    def scalaVersion: T[String] = T(v.scalaVersions(crossValue))
    override def millSourcePath = os.pwd / "dependencies" / "rocket-chip" / "dependencies" / "hardfloat" / "hardfloat"
    
    def chiselModule = None
    def chiselPluginJar = None
    def chiselIvy = Some(v.chiselCrossVersions(chiselVersion)._1)
    def chiselPluginIvy = Some(v.chiselCrossVersions(chiselVersion)._2)
  }

  object cde extends Cross[CDE](crossValue)
  trait CDE
    extends millbuild.dependencies.`rocket-chip`.dependencies.cde.common.CDEModule
      with ScalaModule
      with Cross.Module[String] {
    def scalaVersion: T[String] = T(v.scalaVersions(crossValue))
    override def millSourcePath = os.pwd / "dependencies" / "rocket-chip" / "dependencies" / "cde" / "cde"
  }

  object diplomacy extends Cross[Diplomacy](crossValue)
  trait Diplomacy
    extends millbuild.dependencies.`rocket-chip`.dependencies.diplomacy.common.DiplomacyModule
      with Cross.Module[String] {
    override def scalaVersion: T[String] = T(v.scalaVersions(crossValue))
    override def millSourcePath = os.pwd / "dependencies" / "rocket-chip" / "dependencies" / "diplomacy" / "diplomacy"
    
    def chiselModule = None
    def chiselPluginJar = None
    def chiselIvy = Option.when(crossValue != "source")(v.chiselCrossVersions(crossValue)._1)
    def chiselPluginIvy = Option.when(crossValue != "source")(v.chiselCrossVersions(crossValue)._2)
    // use CDE from source until published to sonatype
    def cdeModule = cde(crossValue)
    def sourcecodeIvy = ivy"com.lihaoyi::sourcecode:0.3.1"
  }
}

object inclusivecache extends Cross[InclusiveCache](v.chiselCrossVersions.keys.toSeq)
trait InclusiveCache
  extends millbuild.common.HasChisel
    with Cross.Module[String] {
  def chiselVersion: String = crossValue
  def scalaVersion = v.scalaVersions(chiselVersion)
  override def millSourcePath = os.pwd / "dependencies" / "rocket-chip-inclusive-cache" / "design" / "craft" / "inclusivecache"
  
  def chiselIvy = Some(v.chiselCrossVersions(chiselVersion)._1)
  def chiselPluginIvy = Some(v.chiselCrossVersions(chiselVersion)._2)

  override def scalacOptions = T {
    super.scalacOptions() ++ Agg("-Xsource:2.13")
  }
  
  def rocketchipModule = rocketchip(crossValue)
  override def moduleDeps = super.moduleDeps ++ Seq(rocketchipModule)
}

// Define main ventus module
object ventus extends Cross[Ventus](v.chiselCrossVersions.keys.toSeq)
trait Ventus
  extends millbuild.common.VentusModule 
    with Cross.Module[String] {
  def chiselVersion: String = crossValue
  def scalaVersion = v.scalaVersions(chiselVersion)
  def chiselIvy = Some(v.chiselCrossVersions(chiselVersion)._1)
  def chiselPluginIvy = Some(v.chiselCrossVersions(chiselVersion)._2)

  override def millSourcePath = os.pwd / "ventus"

  def hardfloatModule = hardfloat(crossValue)
  def fpuv2Module = fpuv2(crossValue)
  def rocketchipModule = rocketchip(crossValue)
  def inclusivecacheModule = inclusivecache(crossValue)

  override def forkArgs = Seq("-Xmx32G", "-Xss192m")
  override def scalacOptions = super.scalacOptions() ++ Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit"
  )

  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(chiselPluginIvy.get)

  // Define tests module
  object tests extends ScalaTests with TestModule.ScalaTest {
    override def forkArgs = Seq("-Xmx32G", "-Xss192m")
    override def ivyDeps = super.ivyDeps() ++ Agg(
      v.chiselCrossVersions(chiselVersion)._2,
      v.chiselCrossVersions(chiselVersion)._3
    )
  }
}

// trait VentusGPGPUPublishModule extends PublishModule {
//   def publishVersion = "1.0.0"
//   def pomSettings = PomSettings(
//     description = "GPGPU processor supporting RISC-V vector extension, developed with Chisel HDL",
//     organization = "THU-DSP-LAB",
//     url = "https://github.com/THU-DSP-LAB/ventus-gpgpu",
//     licenses = Seq(License.`Apache-2.0`),   // Mulan not supported yet
//     versionControl = VersionControl.github("THU-DSP-LAB", "ventus-gpgpu"),
//   )
// }
