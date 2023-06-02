#include "init_extmem.hpp"

// // 定义缓冲区数量和基地址数组
// const int num_buffer = 5;
// int buffer_base[num_buffer] = {0, 8, 16, 24, 32};

// // 定义缓冲区大小数组
// int buffer_size[num_buffer] = {8, 8, 8, 8, 8};

// // 定义存储缓冲区数据的向量
// std::vector<std::vector<uint8_t>> buffer_data(num_buffer);

// 读取外部文本文件并将数据存入缓冲区
void BASE::readTextFile(const std::string &filename, std::vector<std::vector<uint8_t>> &buffers, uint64_t *buffer_size)
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
uint32_t BASE::getBufferData(const std::vector<std::vector<uint8_t>> &buffers, int virtualAddress, int num_buffer, uint64_t *buffer_base, uint64_t *buffer_size)
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
        data |= static_cast<uint32_t>(byte) << (i * 8);
    }

    return data;
}

void BASE::writeBufferData(int writevalue, std::vector<std::vector<uint8_t>> &buffers, int virtualAddress, int num_buffer, uint64_t *buffer_base, uint64_t *buffer_size)
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
        return;
    }

    int offset = virtualAddress - buffer_base[bufferIndex];
    int startIndex = offset;

    for (int i = 0; i < 4; i++)
    {
        uint8_t byte = static_cast<uint8_t>(writevalue >> (i * 8));
        buffers[bufferIndex][startIndex + i] = byte;
    }
}

void BASE::INIT_EXTMEM()
{
    int num_buffer = mtd.num_buffer;
    uint64_t *buffer_base = mtd.buffer_base;
    uint64_t *buffer_size = mtd.buffer_size;
    buffer_data = new std::vector<std::vector<uint8_t>>(num_buffer);
    readTextFile(datafile, *buffer_data, buffer_size);
}
