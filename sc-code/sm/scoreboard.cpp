#include "sm.h"

void sm::scoreboard()
{
    I_TYPE tmpins;
    int s1, s2;
    std::set<SCORE_TYPE>::iterator it;
    REG_TYPE regtype;
    while (true)
    {
        wait(dispatch | writeback);
        /***update score***/
        if (dispatch.triggered())
        {
            tmpins = ibuf_ins; // this top_ins is the old data
            switch (tmpins.op)
            {
            case lw_ | add_:
                regtype = s;
                break;
            case vaddvv_ | vaddvx_ | vload_:
                regtype = v;
                break;
            case vfadd_:
                regtype = f;
                break;
            }
            score.insert(SCORE_TYPE(regtype, tmpins.d));
        }
        if (writeback.triggered())
        {
            tmpins = wb_ins; // 需要后续修改，注意时序，少SC_ZERO_TIME
            switch (tmpins.op)
            {
            case lw_ | add_:
                regtype = s;
                break;
            case vaddvv_ | vaddvx_:
                regtype = v;
                break;
            case vfadd_:
                regtype = f;
                break;
            }

            it = score.find(SCORE_TYPE(regtype, tmpins.d));
            if (it == score.end())
            {
                perror("writeback error: scoreboard can't find rd in score set.\n");
                break;
            }
            score.erase(it);
        }

        /***judge new ins at top of ibuffer***/
        wait(SC_ZERO_TIME); // ensure top_ins is the new data
        tmpins = ibuf_ins;
        switch (tmpins.op)
        {
        case add_ | beq_:
            if (score.find(SCORE_TYPE(s, tmpins.s1)) == score.end() &&
                score.find(SCORE_TYPE(s, tmpins.s2)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        case vaddvv_:
            if (score.find(SCORE_TYPE(v, tmpins.s1)) == score.end() &&
                score.find(SCORE_TYPE(v, tmpins.s2)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        case vaddvx_:
            if (score.find(SCORE_TYPE(v, tmpins.s1)) == score.end() &&
                score.find(SCORE_TYPE(s, tmpins.s2)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        case vfadd_:
            if (score.find(SCORE_TYPE(f, tmpins.s1)) == score.end() &&
                score.find(SCORE_TYPE(f, tmpins.s2)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        }
    }
}