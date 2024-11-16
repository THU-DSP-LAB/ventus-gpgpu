#include "common.h"
#include "kernel.hpp"
#include <iostream>
#include <cassert>
#include <cstdlib>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <functional>
#include <memory>
#include <string>
#include <vector>

int cmdarg_kernel(std::string arg, std::function<void(std::shared_ptr<Kernel>)> new_kernel);
int cmdarg_error(std::vector<std::string> args);
int cmdarg_help(int exit_id);

int parse_arg(std::vector<std::string> args, std::function<void(std::shared_ptr<Kernel>)> new_kernel) {
    // NOTE: make g_config writable
    extern global_config_t g_config_writable;
    global_config_t* g_config_rw = (global_config_t*)(&g_config_writable);

    for (int argid = 0; argid < args.size(); argid++) {
        if (args[argid].starts_with("+verilator+")) {
            continue;
        }
        if (args[argid] == "-f") {
            if (++argid >= args.size()) {
                cmdarg_error(std::vector<std::string>(args.begin() + argid - 1, args.end()));
            } else {
                std::filesystem::path filename, path_to_file, path_origin;
                try {
                    filename = std::filesystem::canonical(args[argid]);
                } catch (const std::filesystem::filesystem_error& e) {
                    std::cout << "Error: file not found: -f " << args[argid] << "\n" << e.what() << std::endl;
                    exit(1);
                }
                path_to_file = filename.parent_path();
                path_origin  = std::filesystem::current_path();
                std::filesystem::current_path(path_to_file);
                std::ifstream file(filename);
                if (!file.is_open()) {
                    std::cout << "Error: file cannot open: -f " << args[argid] << std::endl;
                    exit(1);
                }
                std::vector<std::string> arguments;
                std::string line;
                while (std::getline(file, line)) {
                    // Ignore characters following '#' on each line
                    size_t sharp_pos = line.find('#');
                    if (sharp_pos != std::string::npos) {
                        line.erase(sharp_pos);
                    }
                    // Split the line into arguments based on spaces or other delimiters
                    std::istringstream iss(line);
                    std::string arg;
                    while (iss >> arg) {
                        arguments.push_back(arg);
                    }
                }
                parse_arg(arguments, new_kernel);
                std::filesystem::current_path(path_origin);
            }
        } else if (args[argid] == "--task") {
            if (++argid >= args.size()) {
                cmdarg_error(std::vector<std::string>(args.begin() + argid - 1, args.end()));
            } else { // TODO
            }
        } else if (args[argid] == "--kernel") {
            if (++argid >= args.size()) {
                cmdarg_error(std::vector<std::string>(args.begin() + argid - 1, args.end()));
            } else if (cmdarg_kernel(args[argid], new_kernel)) {
                cmdarg_error(std::vector<std::string>(args.begin() + argid - 1, args.begin() + argid + 1));
            }
        } else if (args[argid] == "--help") {
            cmdarg_help(0);
        } else if (args[argid] == "--sim-time-max") {
            if (++argid >= args.size()) {
                cmdarg_error(std::vector<std::string>(args.begin() + argid - 1, args.end()));
            } else {
                uint64_t simtime = std::stoull(args[argid]);
                if (simtime <= 0) {
                    std::cout << "Error: --sim-time-max needs number > 0\n";
                    cmdarg_error(std::vector<std::string>(args.begin() + argid - 1, args.end()));
                }
                g_config_rw->sim_time_max = simtime;
            }
        } else if (args[argid] == "--snapshot") {
            if (++argid >= args.size()) {
                cmdarg_error(std::vector<std::string>(args.begin() + argid - 1, args.end()));
            } else {
                uint64_t snapshot_time = std::stoull(args[argid]);
                g_config_rw->snapshot.enable = (snapshot_time > 0);
                g_config_rw->snapshot.time_interval = snapshot_time;
            }
        } else {
            cmdarg_error(std::vector<std::string>(args.begin() + argid, args.begin() + argid + 1));
        }
    }
    return 0;
}

int cmdarg_kernel(std::string arg_raw, std::function<void(std::shared_ptr<Kernel>)> new_kernel) {

    int len   = arg_raw.size();
    char* arg = new char[len + 1];
    strcpy(arg, arg_raw.c_str());

    // int taskid     = -1;
    char* name     = nullptr;
    char* metafile = nullptr;
    char* datafile = nullptr;

    char* ptr1   = NULL;
    char* subarg = strtok_r(arg, ",", &ptr1);
    while (subarg) {
        if (strlen(subarg) > 0) {
            char* ptr2 = NULL;
            char* var  = strtok_r(subarg, "=", &ptr2);
            char* val  = strtok_r(NULL, "=", &ptr2);
            assert(var && val);

            // if (strcmp(var, "taskid") == 0) {
            //     int num = std::stoi(val);
            //     assert(num < host->get_num_task());
            //     assert(num >= 0);
            //     taskid = num;
            // } else
            if (strcmp(var, "name") == 0) {
                name = val;
            } else if (strcmp(var, "metafile") == 0) {
                metafile = val;
            } else if (strcmp(var, "datafile") == 0) {
                datafile = val;
            } else {
                goto RET_ERR;
            }
        }
        subarg = strtok_r(NULL, ",", &ptr1);
    }

    if (!(name && metafile && datafile)) {
        goto RET_ERR;
    }

    // if (taskid != -1) {
    //     // TODO
    // } else
    {
        std::shared_ptr<Kernel> kernel = nullptr;
        try {
            kernel = std::make_shared<Kernel>(
                name, std::filesystem::canonical(metafile), std::filesystem::canonical(datafile));
        } catch (const std::filesystem::filesystem_error& e) {
            std::cout << "Error: file not found: \n"
                      << "metafile = " << metafile << "\ndatafile = " << datafile << "\n"
                      << e.what() << std::endl;
            exit(1);
        }
        new_kernel(kernel);
    }

    return 0;
RET_ERR:
    delete[] arg;
    return -1;
}

int cmdarg_error(std::vector<std::string> args) {
    std::cout << "Incorrect argument: \n";
    for (int i = 0; i < args.size(); i++) {
        std::cout << "  " << args[i] << "\n";
    }
    cmdarg_help(1);
    exit(1);
}

int cmdarg_help(int exit_id) {
    std::cout << "ventus-sim [--arg subarg1=val1,subarg2=val2,...]\n"
              << "\n"
              << "Supported cmdline arguments: \n"
              << "-f         FILE     string  // load cmd args from file\n"
              << "                            // if no cmd args is given, -f ventus_cmdargs.txt is applied\n"
              << "\n"
              << "--task                      // create a new GPGPU task\n"
              << "  subarg:  name     string  // 任取\n"
              << "           id       uint    // 任取\n"
              << "\n"
              << "--kernel                    // create a new GPGPU kernel\n"
              << "  subarg:  name     string  // 任取\n"
              << "           metafile string  // kernel的.metadata文件路径\n"
              << "           datafile string  // kernel的.data文件路径\n"
              << "           taskid   uint    // 可选，若无则为不归属任何task的独立kernel。必须指向之前已经申明的task\n"
              << "\n"
              << "--snapshot INTERVAL uint    // 每隔多少仿真时间生成一个快照，若为0则关闭快照功能"
              << "--sim-time-max NUM  uint    // number of simulation cycles" << std::endl;
    exit(exit_id);
}
