package TensorCore

import FPUv2.utils.{EmptyFPUCtrl, TestFPUCtrl}
import chisel3._
import chisel3.util._
import top.parameters._
import pipeline.{vExeData,WriteVecCtrl}

// D=A*B+D
// Matrix D comes from Register.
// Matrix A\B comes from SMEM.

class TC_MMAInput(tcCtrl: TCCtrl) extends Bundle{
  val data_in = new vExeData
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TC_MMAOutput(tcCtrl:TCCtrl) extends Bundle{
  //  val data_out = Vec(DimM*DimN,UInt(dataLen.W))
  // ??? d=a*b+d
  // write back to D
  //    val data_out = Vec(num_thread,UInt(xLen.W))//pipeline.WriteVecCtrl.wb_wvd_rd
  val data_out = Vec(num_thread,UInt(xLen.W)) //new WriteVecCtrl
  val ctrl = tcCtrl.cloneType
}