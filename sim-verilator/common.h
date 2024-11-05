#pragma once
#include <cstdint>
#include <filesystem>

typedef struct {
    uint64_t sim_time_max;              // 最大仿真时间限制
    struct {                            // 波形输出功能，这里只设置正常仿真流程，对仿真快照回溯后的波形输出无影响
        bool enable;                    // 是否启用？仿真快照回溯后将自动启用
        uint64_t time_begin;            // 输出波形的起始时刻
        uint64_t time_end;              // 输出波形的结束时刻，end > begin才有波形输出
        std::filesystem::path filename; 
    } waveform;
    struct {                            // 仿真快照，当仿真出错时可回溯仿真进度到最旧快照，开启波形记录重新仿真
        bool enable;
        uint64_t time_interval;         // 快照时间间隔
        int num_max;                    // 最大快照数量，超限时新快照将顶替最旧快照
        std::filesystem::path filename; // 快照输出的FST波形文件名
    } snapshot;
} global_config_t;

extern const global_config_t g_config;
