#include "BASE.h"
#include <sstream>

// void BASE::INIT_INS(int warp_id)
// {
//     WARPS[warp_id].ireg[0] = I_TYPE(vaddvv_, warp_id + 3, 1, 2);
//     WARPS[warp_id].ireg[1] = I_TYPE(add_, 1, 3, 6);
//     WARPS[warp_id].ireg[2] = I_TYPE(lw_, 4, 1, 9);
//     WARPS[warp_id].ireg[3] = I_TYPE(vle32v_, 3, 9, 4);
//     WARPS[warp_id].ireg[4] = I_TYPE(vaddvx_, 7, 4, 5);
//     WARPS[warp_id].ireg[5] = I_TYPE(vaddvx_, 2, 1, 4 + warp_id);
//     WARPS[warp_id].ireg[6] = I_TYPE(vfaddvv_, 3, 4, 2);
//     WARPS[warp_id].ireg[7] = I_TYPE(beq_, 6, 7, -5);
//     WARPS[warp_id].ireg[8] = I_TYPE(vaddvv_, 0, 1, 2);
//     WARPS[warp_id].ireg[9] = I_TYPE(add_, 1, 3, 3);
//     WARPS[warp_id].ireg[10] = I_TYPE(lw_, 0, 3, 9);
//     WARPS[warp_id].ireg[11] = I_TYPE(vle32v_, 3, 8, 4);
//     WARPS[warp_id].ireg[12] = I_TYPE(vle32v_, 3, 8, 5);
//     WARPS[warp_id].ireg[13] = I_TYPE(vle32v_, 3, 8, 6);
//     WARPS[warp_id].ireg[14] = I_TYPE(vle32v_, 3, 8, 7);
//     WARPS[warp_id].ireg[15] = I_TYPE(vle32v_, 3, 8, 8);
//     WARPS[warp_id].ireg[16] = I_TYPE(vle32v_, 3, 8, 9);
//     WARPS[warp_id].ireg[17] = I_TYPE(vle32v_, 3, 8, 10);
//     WARPS[warp_id].ireg[18] = I_TYPE(vle32v_, 3, 8, 11);
// }

void BASE::INIT_REG(int warp_id)
{
    // WARPS[warp_id].s_regfile[0] = 44;
    // WARPS[warp_id].s_regfile[1] = -10;
    // WARPS[warp_id].s_regfile[2] = 666;
    // WARPS[warp_id].s_regfile[3] = 32;
    // WARPS[warp_id].s_regfile[4] = 4;
    // WARPS[warp_id].s_regfile[5] = 888;
    // WARPS[warp_id].s_regfile[6] = 6;
    // WARPS[warp_id].s_regfile[7] = 22;
    // WARPS[warp_id].v_regfile[0].fill(1);
    // WARPS[warp_id].v_regfile[1].fill(3);
    // WARPS[warp_id].v_regfile[2].fill(-10);
    // WARPS[warp_id].v_regfile[3].fill(7);
    // WARPS[warp_id].v_regfile[4].fill(-1);
    // WARPS[warp_id].v_regfile[5].fill(10);
    // WARPS[warp_id].v_regfile[6].fill(8);
    // WARPS[warp_id].v_regfile[7].fill(1);
    // for (int i = 0; i < 32; i++)
    // {
    //     WARPS[warp_id].s_regfile[i] = 0;
    //     WARPS[warp_id].v_regfile[i].fill(0);
    // }
    for (int i = 0; i < num_thread; i++)
        WARPS[warp_id].v_regfile[0][i] = 30 + i;

    for (int i = 0; i < num_thread; i++)
        if (i == 0 || i == 1 || i == 2 || i == 7)
            WARPS[warp_id].v_regfile[1][i] = 30 + i;

    WARPS[warp_id].s_regfile[20] = 88;
    WARPS[warp_id].s_regfile[21] = 88;
}

// void BASE::INIT_EXTMEM()
// {
//     const int MAX_LINES = 8192; // 定义最大行数

//     std::ifstream infile("../../testcase/data_hex.txt"); // 打开文件

//     std::string line; // 定义一个字符串来存储每一行数据

//     int index = 0;      // 定义一个索引来追踪数组中的元素位置
//     int line_count = 0; // 定义一个计数器来追踪文件的行数

//     while (std::getline(infile, line)) // 循环读取每一行数据
//     {
//         ++line_count; // 计数器加一

//         if (line_count > MAX_LINES) // 判断行数是否超过最大值
//         {
//             std::cout << "Warning: the file contains more than " << MAX_LINES << " lines. Stop reading.\n";
//             break; // 超过最大行数则停止读取
//         }

//         std::stringstream ss(line); // 将字符串转换为 stringstream 对象

//         unsigned int hex_value; // 定义一个变量来存储十六进制数

//         ss >> std::hex >> hex_value; // 从 stringstream 中读取十六进制数

//         external_mem[index++] = hex_value; // 将读取到的十六进制数存储到数组中
//     }
// }

void BASE::INIT_INSMEM()
{
    const int MAX_LINES = mtd.buffer_size[0] / 4; // 定义最大行数
    cout << "INIT_INSMEM: MAX_LINES of ins=" << MAX_LINES << "\n";

    std::ifstream infile(datafile); // 打开文件

    std::string line; // 定义一个字符串来存储每一行数据

    int index = 0;      // 定义一个索引来追踪数组中的元素位置
    int line_count = 0; // 定义一个计数器来追踪文件的行数

    while (std::getline(infile, line)) // 循环读取每一行数据
    {
        ++line_count; // 计数器加一

        if (line_count > MAX_LINES) // 判断行数是否超过最大值
        {
            std::cout << "INIT_INSMEM: finish reading ins from datafile\n";
            break; // 超过最大行数则停止读取
        }

        std::stringstream ss(line); // 将字符串转换为 stringstream 对象

        unsigned int hex_value; // 定义一个变量来存储十六进制数

        ss >> std::hex >> hex_value; // 从 stringstream 中读取十六进制数

        ins_mem[index] = hex_value; // 将读取到的十六进制数存储到数组中
        // cout << "INIT_INSMEM: read ins_mem[" << std::dec << index << "]=" << std::hex << ins_mem[index] << "\n";

        index++;
    }
}