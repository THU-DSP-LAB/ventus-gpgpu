# chisel future

# 硬件连接符号

chisel3.6中提出了不同于之前使用`:=`​、`<>`​与DataView的连接方式，使用了一套基于aligned/flipped计算方式的[新连接符](https://www.chisel-lang.org/docs/explanations/connectable)

aligned/flipped是一套用于表示数据驱动**相对**方向的计算方式：

1. 需要有一个基准base（通常以Bundle顶层作为基准，计算其内部各元素的aligned/flipped性质）
2. 元素总与自身对齐，例如我们说base与base是aligned
3. 相对于基准偶数次`Flipped()`​等价于aligned，相对于基准奇数次`Flipped()`​等价于flipped
4. Bundle内的元素默认与Bundle对齐，除非使用了`Flipped()`​
5. 嵌套Bundle会逐层按照上述第3,4条展开计算，得到所有最底层成员相对于最顶层的对齐情况

chisel3.6提供的新连接符（英文原文）：

* ​`c := p`​ (mono-direction): connects all `p`​ members to `c`​; requires `c`​ and `p`​ to not have any flipped members
* ​`c :#= p`​ (coercing mono-direction): connects all `p`​ members to `c`​; regardless of alignment
* ​`c :<= p`​ (aligned-direction): connects all aligned (non-flipped) `c`​ members from `p`​
* ​`c :>= p`​ (flipped-direction): connects all flipped `p`​ members from `c`​
* ​`c :<>= p`​ (bi-direction operator): connects all aligned `c`​ members from `p`​; all flipped `p`​ members from `c`​

chisel3.6提供的新连接符（中文总结），注意所有连接符的左侧称为consumer，右侧称为producer

* ​`c := p`​单向连接，要求c与p的内部是纯粹对齐的，c的所有信号（全部为aligned）被p的对应信号驱动
* ​`c :<= p`​正向连接，c的所有aligned信号被p的对应信号驱动（只关心左侧aligned信号）
* ​`c :>= p`​反压连接，p的所有flipped信号被c的对应信号驱动（只关心右侧flipped信号）
* ​`c :<>= p`​双向连接，等价于`c :<= p`​且`c :>= p`​
* ​`c :#= p`​强制单向连接，强制c中的aligned与flipped信号全部各自拉取p中的同名信号驱动自己，等价于`c :<= p`​且`p :>= c`​。例如可以用于监视一个已经连接好的DecoupledIO

上述5种连接符中只有`:<=`​与`:>=`​是原语，其他3种可以被这两种等价出来

连接符内部的运算过程可分为两步：在被驱动侧计算aligned/flipped并找出待驱动信号、从另一端拉取同名信号提供驱动

* 对于`c :<= p`​正向连接，consumer中的aligned信号是待驱动信号，这些信号会拉取p中的同名信号驱动自己，但并不关心被拉取信号相对于自己的aligned/flipped性质
* 对于`c :>= p`​反压连接，producer中的flipped信号是待驱动信号，这些信号会拉取c中的同名信号驱动自己，但并不关心被拉取信号相对于自己的aligned/flipped性质
* 如果希望驱动x中的aligned信号，将它作为`:<=`​的LHS也就是`x :<= ???`​
* 如果希望驱动x中的flipped信号，将它作为`:>=`​的RHS也就是`??? :>= x`​

连接符并不关心两侧各成员是Input、Output还是内部信号，这两者是无关的，但都是从aligned/flipped性质中衍生出来的

* 对于`val io = IO(base)`​，`IO()`​方法会将`base`​内部的aligned信号推导为Output，将`base`​内部的flipped信号推导为Input
* 如果两个IO信号直接用连接符连接，当然要求两信号互为flipped，因为Input信号不能被驱动
* 对于内部信号，既可以拉取Input驱动自己，又可以拉取Output驱动自己，这是连接符左右两侧的信号彼此之间的aligned/flipped关系不确定
* 连接符不关心Input/Output性质，但如果连接符给Input信号连接了驱动，IO会报错
