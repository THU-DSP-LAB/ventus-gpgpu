COMPILER = iverilog
SRC_DIR = ./
SRCS := $(wildcard $(SRC_DIR)/*.v)
SIM = vvp
TARGET_DIR = ./build
TARGET = masktest
WAVE_VIEWER = gtkwave

.PHONY: compile run wave clean

# 编译规则，这里不需要指定具体的数字，通过%模式匹配传入
compile:
	# $(COMPILER) -o $(TARGET_DIR)/$(TARGET).out $(SRCS) -g2005-sv
	$(COMPILER) -o $(TARGET_DIR)/$(TARGET).out $(SRCS)

# 运行规则
run: compile
	$(SIM) $(TARGET_DIR)/$(TARGET).out

# 波形查看规则
wave: run
	$(WAVE_VIEWER) $(TARGET_DIR)/$(TARGET).vcd

# 清理规则
clean:
	rm -f $(TARGET_DIR)/*

# 确保目标目录存在
$(shell mkdir -p $(TARGET_DIR))
