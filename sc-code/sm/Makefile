LIB_DIR=-L /home/lan/utils/systemc-2.3.4/lib-linux64/
INC_DIR=-I /home/lan/utils/systemc-2.3.4/include/
LIB=-lsystemc -Wl,-rpath,/home/lan/utils/systemc-2.3.4/lib-linux64/

App=BASE

.PHONY:BASE_test GPGPU_test

BASE_test:
	g++ -o $(App) BASE_test.cpp BASE.cpp exec.cpp init_decodetable.cpp init_ins.cpp init_instable.cpp initialize.cpp $(LIB_DIR) $(INC_DIR) $(LIB) -std=c++20

GPGPU_test:
	g++ -o $(App) GPGPU_test.cpp BASE.cpp exec.cpp init_decodetable.cpp init_ins.cpp init_instable.cpp initialize.cpp CTA_Scheduler.cpp init_extmem.cpp ../parameters.cpp $(LIB_DIR) $(INC_DIR) $(LIB) -std=c++20

clean:                                                                                                           
	RM -RF $(App)
