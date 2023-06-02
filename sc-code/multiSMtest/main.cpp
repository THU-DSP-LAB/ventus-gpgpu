#include <systemc.h>

// 定义一个CSR寄存器类
class CSRRegister {
public:
    void writeData(int data) {
        // 写入CSR寄存器的逻辑
        // ...
    }
};

// 定义在SM之外的功能类
class Functionality {
public:
    void sendDataToCSR(int data) {
        // 获取SM实例
        SM* sm_instance = dynamic_cast<SM*>(sc_core::sc_find_object("sm_module_name"));
        if (sm_instance) {
            // 调用SM模块的方法写入CSR寄存器
            sm_instance->writeToCSR(data);
        } else {
            // 找不到SM实例或转换失败时的处理逻辑
        }
    }
};

// 定义SM模块
SC_MODULE(SM) {
    CSRRegister csr_register;

    void writeToCSR(int data) {
        csr_register.writeData(data);
    }

    // SM模块的其他成员和行为
    // ...
};

int sc_main(int argc, char* argv[]) {
    // 创建SM模块实例
    SM sm_module("sm_module_name");

    // 创建功能类实例
    Functionality functionality;

    // 在仿真开始时调用发送数据写入CSR的功能
    functionality.sendDataToCSR(42);

    // 运行仿真
    sc_start();

    return 0;
}
