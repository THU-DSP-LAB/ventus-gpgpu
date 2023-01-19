LIB_DIR=-L /home/lan/utils/systemc-2.3.4/lib-linux64/
INC_DIR=-I /home/lan/utils/systemc-2.3.4/include/
LIB=-lsystemc -Wl,-rpath,/home/lan/utils/systemc-2.3.4/lib-linux64/
App=sm
all:
	g++ -o $(App) *.cpp $(LIB_DIR) $(INC_DIR) $(LIB)
fetch_test:
	g++ -o fetch_test fetch_test.cpp fetch.cpp fetch.h $(LIB_DIR) $(INC_DIR) $(LIB)
clean:                                                                                                           
	RM -RF $(App)   
