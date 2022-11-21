#ifndef OUTPUTFIFOADAPTER_H
#define OUTPUTFIFOADAPTER_H

#include <systemc.h>


// a wrapper/adapter for bridging a SystemC FIFO to a signal-level
// read/valid dequeue interface

template <class T>
class OutputFIFOAdapter : public sc_module
{
    SC_HAS_PROCESS(OutputFIFOAdapter);

public:
    sc_in_clk clk;
    sc_fifo_out<T> fifoOutput;

    OutputFIFOAdapter(sc_module_name nm) : sc_module(nm)
    {
        m_transferCount = 0;

        SC_THREAD(transferMonitor);
        sensitive << clk.pos();

        SC_CTHREAD(fifoOutputAdapt, clk.pos());
    }

    void resetCounters()
    {
      m_transferCount = 0;
    }

    void bindSignalInterface(sc_out<bool> & valid, sc_in<bool> & ready, sc_out<T> & data)
    {
        valid.bind(m_valid);
        ready.bind(m_ready);
        data.bind(m_data);
    }

    unsigned long int getTransferCount()
    {
        return m_transferCount;
    }

    void transferMonitor()
    {
        while(1)
        {
            if( m_valid && m_ready)
                m_transferCount++;
            wait(1);
        }
    }

    void fifoOutputAdapt()
    {
        while(1)
        {
            wait(1);

            m_ready = (fifoOutput.num_free() > 0);

            if(fifoOutput.num_free() && m_valid)
            {
                fifoOutput.write(m_data);
            }
        }
    }

protected:
    sc_signal<bool> m_valid;
    sc_signal<bool> m_ready;
    sc_signal<T> m_data;

    unsigned long int m_transferCount;

};

#endif // OUTPUTFIFOADAPTER_H
