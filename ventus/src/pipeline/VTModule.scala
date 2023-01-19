package pipeline

import chisel3._
import chisel3.util._

import pipeline.parameters

trait VTCoreParameters{
  val num_warp = parameters.num_warp
  val num_thread = 16
  val num_fetch = 4
  val num_issue = 2
}

class VTModule extends Module with VTCoreParameters
