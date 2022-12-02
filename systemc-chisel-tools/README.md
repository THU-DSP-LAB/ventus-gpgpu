# 配置环境
配置verilator和systemc：https://zhuanlan.zhihu.com/p/473218029  
配置chisel，进行到【二、】即可：https://blog.csdn.net/qq_39507748/article/details/118003696  
我将环境都安装到个人文件夹，方便之后管理，当时的.bashrc中加了如下语句：  
```
export SYSTEMC_HOME="/home/lan/utils/systemc-2.3.3"
export LD_LIBRARY_PATH=$SYSTEMC_HOME/lib-linux64
export SYSTEMC_LIBDIR=$SYSTEMC_HOME/lib-linux64
export SYSTEMC_INCLUDE=$SYSTEMC_HOME/include
export VERILATOR_ROOT="/home/lan/utils/verilator"
export PATH=$VERILATOR_ROOT/bin:$PATH
export PATH="$PATH:/home/lan/utils/scala-2.12.12/bin"
```
# 使用systemc-chisel-tools
```
git clone https://github.com/maltanar/systemc-chisel-tools.git
```
将chisel文件全放进工程目录，根据sbt的要求，放在src/main/scala文件夹中  
这里放了与ALUexe有关的文件，我们将生成ALUexe的systemc代码。  
我们在execution.scala文件中加入如下代码：  
```
object ALUexeMain extends App {
  println("Generating the ALUexe hardware")
  (new chisel3.stage.ChiselStage).emitVerilog(new ALUexe(), Array("--target-dir", "generated"))
}
```
这个当时没有加，报错了，提示：`[error] java.lang.RuntimeException: No main class detected. [error] at scala.sys.package$.error(package.scala:30)`。感觉就像缺少main函数。  

工程中缺少build.sbt文件，新建它，并写入如下内容：  
```
scalaVersion := "2.12.13"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-language:reflectiveCalls",
)

// Chisel 3.5
addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.5.3" cross CrossVersion.full)
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.5.3"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.5.3"
```
这个文件配置sbt将chisel转为RTL。文件中写的scala版本和chisel版本要匹配，版本匹配信息可以在网上搜到。这样sbt会自动下载chisel，存放到一个临时目录中。（这部分说的不一定对，个人浅薄理解）  
还要修改一下chisel-to-systemc.sh文件，文件中的VERILATOR_DIR应该改成我们安装verilator的地方。有两处VERILATOR_DIR需要修改  

最后，修改chisel-to-systemc.sh第29行，将verilator运行的位置修改一下。估计是改用了最新版sbt导致的，修改如下图：  
![](pictures/Snipaste_2022-11-20_20-54-27.png)  

现在可以运行命令：
```
./chisel-to-systemc-tb.sh ./generated/ALUexe output
```
它首先调用sbt将chisel转化为RTL，存放在generated目录中，然后调用verilator将RTL转化为systemc，存放在指定的output文件夹中。

最终，我们成功在output文件夹生成了ALUexe的systemc代码，模块定义在VALUexe.h，模块具体功能在VALUexe___024root__DepSet_h70c47e95__0.cpp中实现。  
缺点是代码结构混乱，虽然功能完整，但是不易于阅读和修改。我想，这个可以作为生成不需要抽象硬件结构的模块时的辅助工具。

## 补充：关于生成模块化systemc代码的注释
尝试了如何使用verilator将RTL代码转为模块化的systemc代码。依然在本文件夹下运行命令，generated文件夹中包含生成的RTL，我们添加一个文件config.vlt，这是verilator的configuration file。hier_block声明要单独地生成哪些子模块（具体可参见这个文件）。运行
```
verilator --sc generated/ALUexe.v generated/config.vlt --hierarchical
```
即可在obj_dir目录下生成模块化的systemc代码。  

![]()  


# 以下为原文档 from https://github.com/maltanar/systemc-chisel-tools
# systemc-chisel-tools
A collection of tools for working with Chisel-generated hardware in SystemC.

chisel-to-systemc.sh
=====================
This script translates a Chisel hardware design into SystemC (via Verilator),
then generates a simple SystemC testbench skeleton that instantiates the module and 
binds signals to the inputs and outputs.
There is also some preliminary support for working with Decoupled interfaces in Chisel:
interfaces with _ready/_valid/_bits naming are extracted and treated separately,
generating simple adapters that support bridging the SystemC-style FIFO interfaces
(sc_fifo<x>) and the signal-level interface. This enables creating independent SystemC
threads that feed the FIFOs with data.

Writing complicated testbenches in SystemC is may sometimes be easier compared to Chisel, since it is
possible to spawn threads executing in parallel and the entire C++ language is also at your
disposal for modelling the signal driver/tester behavior.

Usage: chisel-to-systemc-tb.sh verilog_module target_dir [-t]

If -t is specified, a testbench skeleton will also be generated.


Requirements
=============

 - working Chisel setup (sbt etc. -- see chisel.eecs.berkeley.edu)
 - Verilator
 - SystemC (SYSTEMC_ROOT, SYSTEMC_INCLUDE and SYSTEMC_LIBDIR env vars must be set)
 - Python
 
