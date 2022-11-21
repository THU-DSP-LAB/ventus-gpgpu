#ifndef INPUTFIFOADAPTER_H
#define INPUTFIFOADAPTER_H

#include <systemc.h>

// a wrapper/adapter for bridging a SystemC FIFO to a signal-level
// read/valid enqueue interface

template <class T>
class InputFIFOAdapter : public sc_module
{
    SC_HAS_PROCESS(InputFIFOAdapter);

public:
    sc_in_clk clk;
    sc_fifo_in<T> fifoInput;

    InputFIFOAdapter(sc_module_name nm) : sc_module(nm)
    {
        m_transferCount = 0;

        SC_THREAD(transferMonitor);
        sensitive << clk.pos();

        SC_THREAD(fifoInputAdapt);
        sensitive << clk.pos();

        m_reset = false;
    }

    void resetCounters()
    {
      m_transferCount = 0;
    }

    void resetContent()
    {
      // read all content from the attached FIFO IF
      while(fifoInput.num_available())
        fifoInput.read();


      m_reset = true;
      for(int i = 0; i < 10; i++)
        wait(clk.posedge_event());
      m_reset = false;
    }

    void bindSignalInterface(sc_in<bool> & valid, sc_out<bool> & ready, sc_in<T> & data)
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

    void fifoInputAdapt()
    {
        m_valid = false;
        m_data = 0xdeadbeef;

        while(1)
        {
            m_valid = false;

            // add delays here for testing the latency insensitivity of the
            // target components
            //wait(15);

            T data;
            if(fifoInput.nb_read(data))
            {
                m_valid = true;
                m_data = data;
                //cout << "**************************************************FIFO " << this->name() << " read value " << data << endl;

                do {
                    wait(1);
                    // include reset for enabling FIFO flush
                    if(m_reset)
                      break;
                } while(m_ready != true);

            } else
                wait(1);
        }
    }

public:
    sc_signal<bool> m_valid;
    sc_signal<bool> m_ready;
    sc_signal<T> m_data;

    bool m_reset;

    unsigned long int m_transferCount;

};

#endif // INPUTFIFOADAPTER_H
