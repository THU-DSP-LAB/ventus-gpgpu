import mill._
import mill.scalalib._

trait HasChisel
  extends ScalaModule {
  // Define these for building chisel from ivy
  def chiselIvy: Option[Dep]
  def chiselPluginIvy: Option[Dep]
  override def ivyDeps = super.ivyDeps() ++ Agg(chiselIvy.get) ++ Agg(chiselPluginIvy.get)
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(chiselPluginIvy.get)
  // override def scalacPluginIvyDeps: T[Agg[Dep]] = T(super.scalacPluginIvyDeps() ++ chiselPluginIvy.map(Agg(_)).getOrElse(Agg.empty[Dep]))
}

trait VentusModule extends HasChisel {
  def hardfloatModule : ScalaModule
  def fpuv2Module : ScalaModule
  def rocketchipModule : ScalaModule
  def inclusivecacheModule : ScalaModule
  override def moduleDeps = super.moduleDeps ++ Seq(
    hardfloatModule,
    fpuv2Module,
    rocketchipModule,
    inclusivecacheModule,
  )
}