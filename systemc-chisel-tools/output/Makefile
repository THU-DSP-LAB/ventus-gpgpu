TARGET  := testbench
SRCS	:= $(wildcard *.cpp)
OBJS	:= ${SRCS:.cpp=.o} 
DEPS	:= ${SRCS:.cpp=.dep} 
XDEPS   := $(wildcard ${DEPS}) 

CXXFLAGS = -std=c++11 
LDFLAGS = -L$(SYSTEMC_ROOT)/lib-linux64
LIBS	= -lsystemc -lpthread
INCLUDE = -I$(SYSTEMC_ROOT)/include -I$(VERILATOR_ROOT)/include -I$(VERILATOR_ROOT)/include/vltstd

.PHONY: all clean distclean 
all:: ${TARGET} 

ifneq (${XDEPS},) 
include ${XDEPS} 
endif 

${TARGET}: ${OBJS} 
	${CXX} ${LDFLAGS} -o $@ $^ ${LIBS} 

${OBJS}: %.o: %.cpp %.dep 
	${CXX} ${CXXFLAGS} ${INCLUDE} -o $@ -c $< 

${DEPS}: %.dep: %.cpp Makefile 
	${CXX} ${CXXFLAGS} ${INCLUDE} -MM $< > $@ 

clean:: 
	-rm -f *~ *.o *.dep ${TARGET} 

distclean:: clean
