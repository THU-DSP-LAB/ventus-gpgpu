package pipeline

import chisel3._
import chisel3.util._

import pipeline.parameters

trait VTCoreParameters{
  val num_warp = parameters.num_warp
  val num_thread = 16
  val num_fetch = 2
  val num_issue = 1
}

class VTModule extends Module with VTCoreParameters
