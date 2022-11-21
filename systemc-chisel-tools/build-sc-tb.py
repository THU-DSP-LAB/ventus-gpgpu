#!/usr/bin/python3

# A simple Python script for extracting input and output ports from a SystemC
# module definition.
# The intended use case for the script is to be part of an automated testbench
# skeleton generator for testing Chisel components in SystemC.
# Since there currently is no Chisel SystemC backend, Verilog is generated via
# the Chisel Verilog backend, which is then passed through Verilator to 
# generate SystemC code. The module header file (VSomething.h) is the target
# for this particular script.

# There is also some preliminary support for extracting Decoupled interfaces,
# e.g those that consist of ready-valid-bits signals. These can be connected
# to SystemC FIFO types (sc_fifo<x>) through simple adapters.

# Contact: Yaman Umuroglu <yamanu@idi.ntnu.no>

import re, sys

def getSignals(templateType, headerData):
    signals = []
    signalMatches = re.findall( templateType + r'<.*>.*;', headerData, re.M)
    for s in signalMatches:
        match = re.match( templateType + r'<(.*)>\s(.*);', s)
        signals += [(match.group(2), match.group(1))]
    signals = dict(signals)
    return signals
    
def getInputSignals(headerData):
    return getSignals(r'sc_in', headerData)

def getOutputSignals(headerData):
    return getSignals(r'sc_out', headerData)

def getInputFIFONames(headerData):
    inputValidMatches = re.findall( r'sc_in<bool>.*valid;', headerData, re.M)
    inputFIFONames = []
    inputSignals=getInputSignals(headerData)
    outputSignals=getOutputSignals(headerData)
    for s in inputValidMatches:
        match = re.match( r'sc_in<bool>\s(.*)_valid;', s)
        fifoName = match.group(1)
        if fifoName+"_bits" in inputSignals and fifoName+"_ready" in outputSignals:
            inputFIFONames += [(fifoName, inputSignals[fifoName+"_bits"])]
        else:
            print >> sys.stderr, "some FIFO signals for " + fifoName + " not found!"
    return dict(inputFIFONames)

def getOutputFIFONames(headerData):
    outputValidMatches = re.findall( r'sc_out<bool>.*valid;', headerData, re.M)
    outputFIFONames = []
    inputSignals=getInputSignals(headerData)
    outputSignals=getOutputSignals(headerData)
    for s in outputValidMatches:
        match = re.match( r'sc_out<bool>\s(.*)_valid;', s)
        fifoName = match.group(1)
        if fifoName+"_bits" in outputSignals and fifoName+"_ready" in inputSignals:
            outputFIFONames += [(fifoName, outputSignals[fifoName+"_bits"])]
        else:
            print >> sys.stderr, "some FIFO signals for " + fifoName + " not found!"    
    return dict(outputFIFONames)
    

if len(sys.argv) == 2:
    headerFileName = str(sys.argv[1])
else:
    print >> sys.stderr, "Please specify input header file name."
    sys.exit()

with open(headerFileName, "r") as myfile:
    data=myfile.read()

moduleName = re.findall(r'SC_MODULE\((.*)\)', data, re.M)[0]
inputSignals = getInputSignals(data)
outputSignals = getOutputSignals(data)
inputFIFOs = getInputFIFONames(data)
outputFIFOs = getOutputFIFONames(data)

inputSignalDecls = ""
inputSignalConns = ""
inputSignalInits = ""   # initializers for the input drivers

outputSignalDecls = ""
outputSignalConns = ""

resetCode = ""

inputFIFODecls = ""
outputFIFODecls = ""
inputFIFOAdps = ""
outputFIFOAdps = ""
inputFIFOInit = ""
outputFIFOInit = ""
inputFIFOSetup = ""
outputFIFOSetup = ""

# - remove FIFO signals from regular I/O since we handle them separately
# - build sc_fifo<> declarations
# - build commands for initialization
for f in inputFIFOs:
    del inputSignals[f+"_valid"]
    del inputSignals[f+"_bits"]
    del outputSignals[f+"_ready"]
    fifoDataType = inputFIFOs[f]
    inputFIFODecls += "  sc_fifo<" + fifoDataType + "> fifo_"+f+";\n"
    inputFIFOAdps += "  InputFIFOAdapter<" + fifoDataType + "> adp_"+f+";\n"
    inputFIFOInit += ", adp_" + f + "(\"adp_" + f  + "\")"
    inputFIFOSetup += "    adp_" + f + ".clk(clk);\n"
    inputFIFOSetup += "    adp_" + f + ".fifoInput(fifo_" + f + ");\n"
    inputFIFOSetup += "    adp_" + f + ".bindSignalInterface(uut."+f+"_valid, uut." +f+"_ready, uut."+f+"_bits);\n"
    
for f in outputFIFOs:
    del outputSignals[f+"_valid"]
    del outputSignals[f+"_bits"]
    del inputSignals[f+"_ready"]
    fifoDataType = outputFIFOs[f]
    outputFIFODecls += "  sc_fifo<" + fifoDataType + "> fifo_"+f+";\n"
    outputFIFOAdps += "  OutputFIFOAdapter<" + fifoDataType + "> adp_"+f+";\n"
    outputFIFOInit += ", adp_" + f + "(\"adp_" + f  + "\")"
    outputFIFOSetup += "    adp_" + f + ".clk(clk);\n"
    outputFIFOSetup += "    adp_" + f + ".fifoOutput(fifo_" + f + ");\n"
    outputFIFOSetup += "    adp_" + f + ".bindSignalInterface(uut."+f+"_valid, uut." +f+"_ready, uut."+f+"_bits);\n"

# Handle clock driver manually if there is a clk input
if "clk" in inputSignals:
    del inputSignals["clk"]
    inputSignalConns += "    uut.clk(clk);\n"
    resetCode += "    sig_reset = true;\n"
    resetCode += "    wait(10*CLOCK_CYCLE);\n"
    resetCode += "    sig_reset = false;\n"

# Build signal declarations and conns for input and outputs
for sigName in inputSignals:
    sigType = inputSignals[sigName]
    inputSignalDecls += "  sc_signal<" + sigType + "> sig_"+sigName+";\n"
    inputSignalConns += "    uut." + sigName + "(sig_" + sigName + ");\n"
    inputSignalInits += "    sig_" + sigName + " = 0;\n"

for sigName in outputSignals:
    sigType = outputSignals[sigName]
    outputSignalDecls += "  sc_signal<" + sigType + "> sig_"+sigName+";\n"
    outputSignalConns += "    uut." + sigName + "(sig_" + sigName + ");\n"


# Load the template
with open("testbench-template/testbench.cpptmpl", "r") as templateFile:
    templateData = str(templateFile.read())

# Add module header #include
templateData=templateData.replace("${MODULE_HEADER}", moduleName+".h")

# Module name
templateData=templateData.replace("${MODULE_NAME}", moduleName)

# Testbench name
templateData=templateData.replace("${TESTBENCH_NAME}", "Test"+moduleName)

# Signal declarations
templateData=templateData.replace("${INPUT_DRIVERS}", inputSignalDecls)
templateData=templateData.replace("${OUTPUT_MONITORS}", outputSignalDecls)

# Signal connections
templateData=templateData.replace("${CONNECT_INPUT_DRIVERS}", inputSignalConns)
templateData=templateData.replace("${CONNECT_OUTPUT_MONITORS}", outputSignalConns)

# Init input drivers
templateData=templateData.replace("${INIT_INPUT_DRIVERS}", inputSignalInits)

# Reset code
templateData=templateData.replace("${RESET_CODE}", resetCode)

# FIFO declarations and adapter declarations
templateData=templateData.replace("${INPUT_FIFO_ADAPTERS}", inputFIFOAdps)
templateData=templateData.replace("${INPUT_FIFOS}", inputFIFODecls)
templateData=templateData.replace("${OUTPUT_FIFO_ADAPTERS}", outputFIFOAdps)
templateData=templateData.replace("${OUTPUT_FIFOS}", outputFIFODecls)

# FIFO setup in constructor
templateData=templateData.replace("${INPUT_FIFO_INIT}", inputFIFOInit)
templateData=templateData.replace("${OUTPUT_FIFO_INIT}", outputFIFOInit)
templateData=templateData.replace("${INPUT_FIFO_SETUP}", inputFIFOSetup)
templateData=templateData.replace("${OUTPUT_FIFO_SETUP}", outputFIFOSetup)

print(templateData)

# TODO fill in SystemC testbench code that instantiates sc_fifo's and
# appropriate adapters for connecting to a ready/valid/bits interface
