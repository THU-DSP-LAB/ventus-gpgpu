circuit SimpleModule :
  module SimpleModule :
    input clock : Clock
    input reset : UInt<1>
    output io : { flip current_mask : UInt<1>[4], current_mask_index : UInt<2>[4]}

    io.current_mask_index[0] <= UInt<1>("h0") @[masktest.scala 18:35]
    io.current_mask_index[1] <= UInt<1>("h0") @[masktest.scala 18:35]
    io.current_mask_index[2] <= UInt<1>("h0") @[masktest.scala 18:35]
    io.current_mask_index[3] <= UInt<1>("h0") @[masktest.scala 18:35]
    when io.current_mask[0] : @[masktest.scala 24:30]
      io.current_mask_index[0] <= UInt<1>("h0") @[masktest.scala 25:39]
    when io.current_mask[1] : @[masktest.scala 24:30]
      io.current_mask_index[1] <= UInt<1>("h1") @[masktest.scala 25:39]
    when io.current_mask[2] : @[masktest.scala 24:30]
      io.current_mask_index[2] <= UInt<2>("h2") @[masktest.scala 25:39]
    when io.current_mask[3] : @[masktest.scala 24:30]
      io.current_mask_index[3] <= UInt<2>("h3") @[masktest.scala 25:39]

