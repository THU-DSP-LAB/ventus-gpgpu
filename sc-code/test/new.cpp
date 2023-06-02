#include <iostream>
#include <fstream>
#include <bitset>
#include <string>

int main() {
    // 读取txt文件
    std::ifstream file("input.txt");
    if (!file) {
        std::cout << "无法打开文件" << std::endl;
        return 1;
    }

    std::string hexString;
    std::getline(file, hexString);
    file.close();

    // 将16进制字符串转换为32位整数
    unsigned int hexValue = std::stoi(hexString, nullptr, 16);

    // 将32位整数转换为32位二进制字符串
    std::string binaryString = std::bitset<32>(hexValue).to_string();

    // 32位比特的字符串（0、1、?）
    std::string bitString = "????????????????????????????1000";

    // 逐位比较
    for (int i = 0; i < 32; i++) {
        if (bitString[i] == '?') {
            continue;  // 不需要判断?
        }

        if (bitString[i] == binaryString[i]) {
            std::cout << "位 " << i << " 相等" << std::endl;
        } else {
            std::cout << "位 " << i << " 不相等" << std::endl;
        }
    }

    return 0;
}
