LIB_DIR=-L /home/lan/utils/systemc-2.3.3/lib-linux64/
INC_DIR=-I /home/lan/utils/systemc-2.3.3/include/
LIB=-lsystemc -Wl,-rpath,/home/lan/utils/systemc-2.3.3/lib-linux64/
App=main
all:
	g++ -o $(App) $(App).cpp $(LIB_DIR) $(INC_DIR) $(LIB)                                                
clean:                                                                                                           
	RM -RF $(App)   
