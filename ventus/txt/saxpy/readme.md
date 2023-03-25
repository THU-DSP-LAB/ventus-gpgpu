# 测试用例: 向量乘加 / SAXPY

## 简介

现有$n$维向量$\bold{x}, \bold{y}$和标量$\Alpha$, 进行如下计算, 并将计算结果更新至$\bold{y}$:
$$
\Alpha \bold{x}+\bold{y}
$$

## 文件及输入格式

| 文件          | 说明                                         |
| ------------- | -------------------------------------------- |
| `saxpy.s`     | 汇编源代码                                   |
| `saxpy.txt`   | 汇编器生成的反汇编文本文档                   |
| `saxpy.vmem`  | 汇编器输出的指令段文件, 但并不能直接用于仿真 |
| `saxpy_.vmem` | 用于仿真的指令段文件, 已经经过格式调整       |
| `saxpy.data`  | 用于仿真的数据段文件, 具体格式见下           |

### saxpy.data数据文件格式

* 每行为一个32bit数据, 以十六进制文本形式保存.
* 第一行为向量长度$n$; 第二行为标量$\Alpha$, float32格式.
* 此后30行补0.
* 33行开始为向量$\bold{x}$的所有元素, float32格式, 共$32\times ceil(n/32)$个元素, 超出的部分补零即可.
* 此后为向量$\bold{y}$的所有元素, float32格式, 同样共$32\times ceil(n/32)$个元素, 超出部分补零.

### saxpy.data样例数据

$$
\begin{align*}
& n = 64, \Alpha = 2.0 \\
& \bold{x} = [\underbrace{0, 0, \cdots, 0}_{32},\underbrace{32, 32, \cdots, 32}_{32} ] \\
& \bold{y} = [0, 2, 4, \cdots, 62, 0, 2, 4 \cdots, 62] \\
\text{output} \ \ & \bold{y} = [0, 2, 4, \cdots, 62, 64, 66, 68, \cdots , 126]
\end{align*}
$$
