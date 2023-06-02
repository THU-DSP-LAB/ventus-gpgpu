LIB_DIR=-L /home/lan/utils/systemc-2.3.4/lib-linux64/
INC_DIR=-I /home/lan/utils/systemc-2.3.4/include/
LIB=-lsystemc -Wl,-rpath,/home/lan/utils/systemc-2.3.4/lib-linux64/

App=main

.PHONY:all new

all:
	g++ -o $(App) main.cpp $(LIB_DIR) $(INC_DIR) $(LIB) -std=c++20

new:
	g++ -o $(App) new.cpp $(LIB_DIR) $(INC_DIR) $(LIB) -std=c++20


clean:                                                                                                           
	RM -RF $(App)
