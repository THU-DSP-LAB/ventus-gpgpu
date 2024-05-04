#include "MemBox.hpp"
#include <cassert>
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

const uint8_t* MemBox::read(uint32_t addr, int len) {
    bool within_single_page = (addr / PAGESIZE == (addr + len - 1) / PAGESIZE);
    MemBox::MemPage* page1  = page_find(addr);
    MemBox::MemPage* page2  = within_single_page ? nullptr : page_find(addr + len - 1);

    uint8_t* buffer = new uint8_t[len];
    if (within_single_page) {
        if (page1 == nullptr) {
            memset(buffer, 0, len);
        } else {
            memcpy(buffer, page1->ptr + (addr - page1->addr), len);
        }
    } else {
        int len1 = page1->addr + PAGESIZE - addr;
        if (page1 == nullptr) {
            memset(buffer, 0, len1);
        } else {
            memcpy(buffer, page1->ptr + (addr - page1->addr), len1);
        }
        if (page2 == nullptr) {
            memset(buffer + len1, 0, len - len1);
        } else {
            memcpy(buffer + len1, page2->ptr, len - len1);
        }
    }
    return buffer;
}

void MemBox::write(uint32_t addr, bool mask[], uint8_t data[], int len) {
    int cnt       = 0;
    MemPage* page = nullptr;
    while (cnt) {
        if (page == nullptr || page->addr + PAGESIZE <= addr + cnt) {
            page = page_find(addr + cnt);
            if(page == nullptr) {
                page = page_new(addr+cnt);
                assert(page);
            }
        }
        if(mask[cnt]) {
            page->ptr[addr + cnt - page->addr] = data[cnt];
        }
        cnt++;
    }
    /*
    bool within_single_page = (addr / PAGESIZE == (addr + len - 1) / PAGESIZE);

    MemPage* page1 = page_find(addr);
    MemPage* page2 = within_single_page ? nullptr : page_find(addr + len - 1);
    page1          = page1 ? page1 : page_new(addr);
    page2          = (within_single_page || page2) ? page2 : page_new(addr);
    assert(page1);
    assert(within_single_page || page2);

    uint8_t* ptr1 = page1->ptr + (addr - page1->addr);
    int len1      = within_single_page ? len : page1->addr + PAGESIZE - addr;
    for (int i = 0; i < len1; i++) {
        if (mask[i]) {
            ptr1[i] = data[i];
        }
    }
    if (!within_single_page) {
        for (int i = 0; i < len - len1; i++) {
            if (mask[i + len1]) {
                page2->ptr[i] = data[len1 + i];
            }
        }
    }
    */
}

void MemBox::write(uint32_t addr, uint8_t data[], int len) {
    bool* mask = new bool[len];
    memset(mask, true, len);
    write(addr, mask, data, len);
    delete[] mask;
}
