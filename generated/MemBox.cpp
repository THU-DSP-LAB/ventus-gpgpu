#include "MemBox.hpp"
#include <cassert>
#include <cstdint>
#include <cstring>
#include <iostream>

MemBox::MemPage* MemBox::page_new(uint32_t addr) {
    addr -= addr % PAGESIZE;

    uint8_t* ptr = nullptr;
    bool success = false;
    while (!success) {
        try {
            ptr     = new uint8_t[this->PAGESIZE];
            success = true;
        } catch (std::bad_alloc) {
            std::cerr << "Memory request failed, currently used memory amount: " << this->m_page_cnt << " * "
                      << this->PAGESIZE << std::endl;
            std::cerr << "Press 'r' to retry, press 'q' to exit... ";
            while (true) {
                char c = getchar();
                if (c == 'q') {
                    exit(EXIT_FAILURE);
                    std::cerr << std::endl;
                } else if (c == 'r') {
                    std::cerr << "Retrying" << std::endl;
                    break;
                }
            }
        }
    }

    MemBox::MemPage page;
    page.ptr  = ptr;
    page.addr = addr;
    this->m_pages.push_back(page);
    this->m_page_cnt++;
    return &this->m_pages.back();
}

MemBox::MemPage* MemBox::page_find(uint32_t addr) {
    bool success = false;
    auto page    = this->m_pages.begin();
    for (; page != this->m_pages.end(); page++) {
        if (page->ptr != nullptr && addr >= page->addr && addr < page->addr + this->PAGESIZE) {
            success = true;
            break;
        }
    }
    if (!success)
        return nullptr;
    else
        return &*page;
}

bool MemBox::read(uint32_t addr, uint8_t buffer[], int len) {
    bool success = false;
    int cnt         = 0;
    MemPage* page   = nullptr;
    while (cnt < len) {
        int len_this_step = PAGESIZE - (addr + cnt) % PAGESIZE;
        len_this_step     = (len_this_step > len - cnt) ? len - cnt : len_this_step;
        page              = page_find(addr + cnt);
        if (page == nullptr) {
            memset(buffer + cnt, 0, len_this_step);
        } else {
            memcpy(buffer + cnt, page->ptr + (addr + cnt - page->addr), len_this_step);
            success = true;
        }
        cnt += len_this_step;
    }
    return success;
}

void MemBox::write(uint32_t addr, bool mask[], uint8_t data[], int len) {
    int cnt       = 0;
    MemPage* page = nullptr;
    while (cnt < len) {
        if (page == nullptr || page->addr + PAGESIZE <= addr + cnt) {
            page = page_find(addr + cnt);
            if (page == nullptr) {
                page = page_new(addr + cnt);
                assert(page);
            }
        }
        if (mask[cnt]) {
            page->ptr[addr + cnt - page->addr] = data[cnt];
        }
        cnt++;
    }
}

void MemBox::write(uint32_t addr, uint8_t data[], int len) {
    bool* mask = new bool[len];
    memset(mask, true, len);
    write(addr, mask, data, len);
    delete[] mask;
}
