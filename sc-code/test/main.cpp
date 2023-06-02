#include "../sm/BASE.h"
#include <iostream>
#include <fstream>
#include <vector>
#include <cstdint>

// 定义缓冲区数量和基地址数组
const int num_buffer = 5;
int buffer_base[num_buffer] = {0, 8, 16, 24, 32};

// 定义缓冲区大小数组
int buffer_size[num_buffer] = {8, 8, 8, 8, 8};

// 定义存储缓冲区数据的向量
std::vector<std::vector<uint8_t>> buffer_data(num_buffer);

// 读取外部文本文件并将数据存入缓冲区
void readTextFile(const std::string &filename, std::vector<std::vector<uint8_t>> &buffers)
{
    std::ifstream file(filename);
    if (!file.is_open())
    {
        std::cerr << "Failed to open file: " << filename << std::endl;
        return;
    }

    std::string line;
    int bufferIndex = 0;
    std::vector<uint8_t> buffer;
    while (std::getline(file, line))
    {
        for (int i = 0; i < line.length(); i += 2)
        {
            std::string hexChars = line.substr(i, 2);
            uint8_t byte = std::stoi(hexChars, nullptr, 16);
            buffer.push_back(byte);
        }

        if (buffer.size() == buffer_size[bufferIndex])
        {
            buffers.push_back(buffer);
            buffer.clear();
            bufferIndex++;
        }
    }

    file.close();
}

// 通过虚拟地址获取对应缓冲区的数据并转换为整数
uint32_t getBufferData(const std::vector<std::vector<uint8_t>> &buffers, int virtualAddress)
{
    int bufferIndex = -1;
    for (int i = 0; i < num_buffer; i++)
    {
        if (virtualAddress >= buffer_base[i] && virtualAddress < (buffer_base[i] + buffer_size[i]))
        {
            bufferIndex = i;
            break;
        }
    }

    if (bufferIndex == -1)
    {
        std::cerr << "No buffer found for the given virtual address." << std::endl;
        return 0;
    }

    int offset = virtualAddress - buffer_base[bufferIndex];
    int startIndex = offset;

    uint32_t data = 0;
    for (int i = 0; i < 4; i++)
    {
        uint8_t byte = buffers[bufferIndex][startIndex + i];
        data |= static_cast<uint32_t>(byte) << ((3 - i) * 8);
    }

    return data;
}

int sc_main(int argc, char *argv[])
{
    sc_bv<10> aaa = "1010011101";

    auto b1 = aaa.range(9, 9);
    auto b2 = aaa.range(3, 0);

    int c1 = (aaa.range(9, 9), aaa.range(3, 0)).to_uint();
    std::bitset<32> c1_1 = (std::bitset<32>)c1;

    cout << "b1=" << b1 << ", b2=" << b2 << ", c1=" << c1 << ", c1_1=" << c1_1 << endl;

    return 0;
}
