import mill._
import scalalib._

object ivys {
  val sv = "2.12.15"
  val scalatest = ivy"org.scalatest::scalatest:3.2.2"
  val scalatestplus = ivy"org.scalatestplus::scalacheck-1-14:3.1.1.1"
  val scalacheck = ivy"org.scalacheck::scalacheck:1.14.3"

  val chisel3 = ivy"edu.berkeley.cs::chisel3:3.5.0"
  val chisel3Plugin = ivy"edu.berkeley.cs:::chisel3-plugin:3.5.0"
  val chiseltest = ivy"edu.berkeley.cs::chiseltest:0.5.0"
  val chiselCirct = ivy"com.sifive::chisel-circt:0.4.0"
}

object cta extends ScalaModule {
  def scalaVersion = ivys.sv
  //override def millSourcePath = os.pwd

  override def forkArgs = Seq("-Xmx32G", "-Xss192m")

  override def scalacOptions = Seq(
    "-Xsource:2.11",
    "-language:reflectiveCalls",
    "-Xcheckinit"//,
  )
  override def scalacPluginIvyDeps = Agg(
    ivys.chisel3Plugin
  )

  override def ivyDeps = Agg(
    ivys.chisel3,
    ivys.chiseltest,
    ivys.chiselCirct
  )

  object tests extends Tests with TestModule.ScalaTest {
    override def ivyDeps = super.ivyDeps() ++ Agg(
      ivys.chiseltest,
      ivys.scalatest
    )
  }
}
