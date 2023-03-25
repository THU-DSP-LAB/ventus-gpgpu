# 测试用例: Gaussian / 高斯消元
## 简介

本例来源于[GPU-Rodinia](https://github.com/yuhc/gpu-rodinia)项目的`Gaussian`测试.

现有n元一次方程组:
$$
Ax=B
$$
$A$为$n\times n$矩阵, $B$为n维向量. 对方程组进行消元, 获得新方程组:
$$
A'x=B'
$$
此时$A'$为上三角阵.

## 文件及输入格式

| 文件             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| `gaussian.s`     | 汇编源代码                                                   |
| `gaussian.txt`   | 汇编器输出的反汇编文本文档                                   |
| `gaussian.vmem`  | 汇编器输出的指令段文件, 但并不能直接用于仿真                 |
| `gaussian_.vmem` | 用于仿真的指令段文件, 相比`gaussian.vmem`, 去除了形如`@xxxxxxxx`的前导, 且每一行只有一个32bit数据, 对应于一条指令. |
| `gaussian8.data` | 用于仿真的数据段文件, 具体格式见下                           |

### gaussian8.data数据文件格式

* 每行为一个32bit数据, 以十六进制文本形式保存.
* 矩阵尺寸$n=2^k, k\ge3$.
* 第一行为$k$, 后续31行均为0.
* 33行开始为矩阵$A$的所有元素, 以行主序方式存储, 数据均为float32格式, 共$n^2$个元素.
* 矩阵$A$之后为零矩阵$M$, 同样为$n^2$个元素.
* 矩阵$M$之后为向量$B$, 数据均为float32格式, 共$n$个元素.

## 运行

在`src/main/scala/pipeline/Top.scala`中找到类`TopForTest_SingleSM`, 修改其中的两个文件路径,分别指向指令vmem文件和数据文件.

```scala
class TopForTest_SingleSM extends Module {
  val io = IO(new Bundle{})

  val param=(new MyConfig).toInstance
  val CPU = Module(new CPUtest_SingleSM)
  val inst_filepath="./txt/gaussian/gaussian_.vmem"
  val data_filepath="./txt/gaussian/gaussian8.data"
  val NUMBER_CU=1
  val sm=VecInit(Seq.fill(NUMBER_CU)(Module(new SM_wrapper).io))
  val mem = Module(new L2ModelWithName(inst_filepath,data_filepath,5)(param))
  val sm2l2model = Module(new SM2L2ModelArbiter()(param))
  // ... ... ... ...
}
```

而后在`src/test/scala/pipeline/hello_test.scala`中运行相应测试, `c.clock.step()`中需要填入一个足以运行完成的周期数, 如:

```scala
class hello_test extends FreeSpec with ChiselScalatestTester{
  "first_test" in {
    val SINGLE_INST=true
    test(new TopForTest_SingleSM()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.clock.setTimeout(0)
      c.clock.step(10000)
    }
  }
}
```

`.withAnnotations(Seq(WriteVcdAnnotation))`会将运行结果输出为波形文件, 此外, 也可以在scala源码中插入`printf()`以进行简单的运行状态监控.

## 注意事项

* 由于时间有限, 虽然汇编源码没有指定矩阵规模以及硬件规模, 但本测试用例目前仅验证了在每warp 8线程的单元上对8*8矩阵进行消元的情况;
* 其他矩阵规模的测试数据可以使用`gpu-rodinia/data/gaussian`下的python脚本生成, 但需自行转换为前文的格式;
* 由于浮点精度问题, 消元后的上三角阵0元素并非真正的0, 而是一个很小的非0数值.