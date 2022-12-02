import re

data  = "class VALUexe VL_NOT_FINAL : public ::sc_core::sc_module, public VerilatedModel {"

# moduleName = re.findall(r'sc_module\((.*)\)', data, re.M)

moduleName = re.findall(r'class.*.VL_NOT_FINAL : public ::sc_core::sc_module', data, re.M)[0]


print(moduleName)
