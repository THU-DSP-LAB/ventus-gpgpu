# CU interface

CU interface的功能包括：

1. 从Allocator、Resource table、WGram2三处拼合线程块信息
2. 存储新线程块信息（`num_wf`​用于判定线程块执行结束，其他信息用于后续的dealloc与向host回报）

    * 用SyncReadMem记录线程块信息
3. 将WG分割为WF并逐个发送给CU（每个WF需步进`sgpr_base`​与`vgpr_base`​）
4. 接收CU发回的线程束信息，统计各WG已完成的WF数量

    * 用SyncReadMem计数WF，由于SyncReadMem无法单周期内完成读取，采用多周期状态机实现
    * 将计数值与2中记录的`num_wf`​比较以确定线程块是否执行完成
5. 某WG的WF全部完成后，向Resouce table发送dealloc请求
6. 某WG的WF全部完成后，向Host回报WG ID

‍
