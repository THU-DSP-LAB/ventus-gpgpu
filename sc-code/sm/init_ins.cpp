#include "BASE.h"
#include <sstream>

void BASE::INIT_INS()
{
    // std::ifstream inputFile("ireg_ins.txt"); // 指令文本文件的路径

    // if (!inputFile)
    // {
    //     std::cout << "无法打开ireg文件。\n";
    // }

    // std::string line;
    // int instructionCount = 0;

    // while (std::getline(inputFile, line))
    // {
    //     // 解析每行指令
    //     std::string opString, dString, s1String, s2String;

    //     std::istringstream iss(line);
    //     if (!(std::getline(iss, opString, ',') &&
    //           std::getline(iss, dString, ',') &&
    //           std::getline(iss, s1String, ',') &&
    //           std::getline(iss, s2String)))
    //     {
    //         std::cout << "指令格式错误。\n";
    //         continue;
    //     }

    //     // 去除字段中可能存在的空格
    //     opString.erase(remove_if(opString.begin(), opString.end(), ::isspace), opString.end());
    //     dString.erase(remove_if(dString.begin(), dString.end(), ::isspace), dString.end());
    //     s1String.erase(remove_if(s1String.begin(), s1String.end(), ::isspace), s1String.end());
    //     s2String.erase(remove_if(s2String.begin(), s2String.end(), ::isspace), s2String.end());

    //     // 将字段转换为适当的类型
    //     int d, s1, s2;
    //     try
    //     {
    //         d = std::stoi(dString);
    //         s1 = std::stoi(s1String);
    //         s2 = std::stoi(s2String);
    //     }
    //     catch (...)
    //     {
    //         std::cout << "指令参数解析错误。\n";
    //         continue;
    //     }

    //     // 根据指令字符串确定枚举值
    //     auto op = magic_enum::enum_cast<OP_TYPE>(opString);
    //     if(!op.has_value()){
    //         std::cout << "指令op参数解析错误。\n";
    //         continue;
    //     }

    //     // 初始化指令对象并存储到数组中
    //     I_TYPE instruction(op.value(), d, s1, s2);
    //     ireg[instructionCount] = instruction;
    //     instructionCount++;
    // }
}
