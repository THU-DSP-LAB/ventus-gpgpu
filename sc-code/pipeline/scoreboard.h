#ifndef _SCOREBOARD_H
#define _SCOREBOARD_H

#include "../parameters.h"
#include <set>

SC_MODULE(scoreboard)
{
    sc_in<I_TYPE> top_ins{"top_ins"};       // from ibuffer
    sc_port<event_if> dispatch{"dispatch"};  // from issue
    sc_port<event_if> wb_event{"wb_event"}; // from writeback
    sc_in<I_TYPE> wb_ins{"wb_ins"};        // from writeback

    sc_out<bool> can_dispatch{"can_dispatch"}; // to issue

    void judge_dispatch();
    void update_score();

    SC_CTOR(scoreboard)
    {
        SC_THREAD(judge_dispatch);
        SC_THREAD(update_score);
    }

private:
    std::set<SCORE_TYPE> score; // record regfile that's to be written
};

#endif