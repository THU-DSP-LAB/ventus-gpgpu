#include "gvm.hpp"
#include <cstdint>
#include <vector>

#define XREG_INSNS \
  {0x0000007f, 0x00000037, "LUI"                 },\
  {0x0000007f, 0x00000017, "AUIPC"               },\
  {0x0000007f, 0x0000006f, "JAL"                 },\
  {0x0000707f, 0x00000067, "JALR"                },\
  {0x0000707f, 0x00000013, "ADDI"                },\
  {0x0000707f, 0x00002013, "SLTI"                },\
  {0x0000707f, 0x00003013, "SLTIU"               },\
  {0x0000707f, 0x00004013, "XORI"                },\
  {0xfe00707f, 0x00001013, "SLLI"                },\
  {0xfe00707f, 0x00005013, "SRLI"                },\
  {0xfe00707f, 0x40005013, "SRAI"                },\
  {0xfe00707f, 0x00000033, "ADD"                 },\
  {0xfe00707f, 0x40000033, "SUB"                 },\
  {0xfe00707f, 0x00001033, "SLL"                 },\
  {0xfe00707f, 0x00002033, "SLT"                 },\
  {0xfe00707f, 0x00003033, "SLTU"                },\
  {0xfe00707f, 0x00004033, "XOR"                 },\
  {0xfe00707f, 0x00005033, "SRL"                 },\
  {0xfe00707f, 0x40005033, "SRA"                 },\
  {0xfe00707f, 0x00006033, "OR"                  },\
  {0xfe00707f, 0x00007033, "AND"                 },\
  {0xfe00707f, 0x02000033, "MUL"                 },\
  {0x8000707f, 0x00007057, "VSETVLI"             },\
  {0x0000707f, 0x00002073, "CSRRS"               },\
  {0x0000707f, 0x00006073, "CSRRSI"              },\
  {0x0000707f, 0x00001073, "CSRRW"               },\
  {0x0000707f, 0x00005073, "CSRRWI"              },\
  {0x0000707f, 0x00003073, "CSRRC"               },\
  {0x0000707f, 0x00007073, "CSRRCI"              },\
  {0x0000707f, 0x00002003, "LW"                  },\
  {0x0000707f, 0x0000305b, "SETRPC"              },\

#define SCALAR_SINGLE_CMP_INSNS \
  {0x0000007f, 0x00000037, "LUI"                 },\
  {0x0000007f, 0x00000017, "AUIPC"               },\
  {0x0000007f, 0x0000006f, "JAL"                 },\
  {0x0000707f, 0x00000067, "JALR"                },\
  {0x0000707f, 0x00000013, "ADDI"                },\
  {0x0000707f, 0x00002013, "SLTI"                },\
  {0x0000707f, 0x00003013, "SLTIU"               },\
  {0x0000707f, 0x00004013, "XORI"                },\
  {0xfe00707f, 0x00001013, "SLLI"                },\
  {0xfe00707f, 0x00005013, "SRLI"                },\
  {0xfe00707f, 0x40005013, "SRAI"                },\
  {0xfe00707f, 0x00000033, "ADD"                 },\
  {0xfe00707f, 0x40000033, "SUB"                 },\
  {0xfe00707f, 0x00001033, "SLL"                 },\
  {0xfe00707f, 0x00002033, "SLT"                 },\
  {0xfe00707f, 0x00003033, "SLTU"                },\
  {0xfe00707f, 0x00004033, "XOR"                 },\
  {0xfe00707f, 0x00005033, "SRL"                 },\
  {0xfe00707f, 0x40005033, "SRA"                 },\
  {0xfe00707f, 0x00006033, "OR"                  },\
  {0xfe00707f, 0x00007033, "AND"                 },\
  {0xfe00707f, 0x02000033, "MUL"                 },\
  {0x8000707f, 0x00007057, "VSETVLI"             },\
  {0x0000707f, 0x00002073, "CSRRS"               },\
  {0x0000707f, 0x00006073, "CSRRSI"              },\
  {0x0000707f, 0x00001073, "CSRRW"               },\
  {0x0000707f, 0x00005073, "CSRRWI"              },\
  {0x0000707f, 0x00003073, "CSRRC"               },\
  {0x0000707f, 0x00007073, "CSRRCI"              },\
  {0x0000707f, 0x00002003, "LW"                  },\

#define VREG_INSNS \
  {0xfff0707f, 0x5e004057, "VMV_V_X_              "},\
  {0xfc00707f, 0x00004057, "VADD_VX_              "},\
  {0xfc00707f, 0x08001057, "VFSUB_VV_             "},\
  {0xfc00707f, 0x90001057, "VFMUL_VV_             "},\
  {0x8000707f, 0x0000202b, "VLW_V_                "},\
  {0xfff0707f, 0x80d02072, "CSRR_V_GL_ID_X_       "},\
  {0xfff0707f, 0x80e02072, "CSRR_V_GL_ID_Y_       "},\
  {0xfff0707f, 0x80f02072, "CSRR_V_GL_ID_Z_       "},\
  {0xfc0ff07f, 0x4c001057, "VFSQRT_V_             "},\
  {0xfc00707f, 0xa0001057, "VFMADD_VV_            "},\
  {0xfc00707f, 0x88006057, "VREMU_VX_             "},\
  {0xfc00707f, 0x94003057, "VSLL_VI_              "},\
  {0xfdfff07f, 0x5008a057, "VID_V_                "},\
  {0x0000707f, 0x0000207b, "VLW12_V_              "},\
  {0xfc00707f, 0x18001057, "VFMAX_VV_             "},\
  {0x0000707f, 0x0000100b, "VSUB12_VI_            "},\
  {0xfc00707f, 0x94006057, "VMUL_VX_              "},\
  {0xfc00707f, 0x00003057, "VADD_VI_              "},\
  {0xfc00707f, 0x00001057, "VFADD_VV_             "},\
  {0xfc00707f, 0x80001057, "VFDIV_VV_             "},\
  {0xfc0ff07f, 0x48019057, "VFCVT_F_X_V_          "},\
  {0xfc00707f, 0x00000057, "VADD_VV_              "},\
  {0x0000707f, 0x0000407b, "VLBU12_V_             "},\
  {0xfc00707f, 0xa4006057, "VMADD_VX_             "},\
  {0xfc00707f, 0x24000057, "VAND_VV_              "},\
  {0xfc00707f, 0x80006057, "VDIVU_VX_             "},\
  {0xfc00707f, 0x6c004057, "VMSLT_VX_             "},\
  {0xfc00707f, 0x6c001057, "VMFLT_VV_             "},\
  {0xfc00707f, 0x2c003057, "VXOR_VI_              "},\
  {0xfc00707f, 0x08000057, "VSUB_VV_              "},\
  {0xfc00707f, 0x68004057, "VMSLTU_VX_            "},\
  {0xfc00707f, 0xa4002057, "VMADD_VV_             "},\
  {0xfc00707f, 0xa4003057, "VSRA_VI_              "},\
  {0xfc00707f, 0x74003057, "VMSLE_VI_             "},\
  {0xfc00707f, 0x08004057, "VSUB_VX_              "},



#define WARP_BARRIER_INSNS \
  {0xfe00707f, 0x0400400b, "BARRIER"            }, \
  {0xfe00707f, 0x0600400b, "BARRIERSUB (warning: BARRIERSUB not supported in GVM yet)"}, \
  {0xfe00707f, 0x0000400b, "ENDPRG"             }, 

#define FP32_VREG_INSNS \
  {0xfc00707f, 0x08001057, "VFSUB_VV_             "},\
  {0xfc00707f, 0x90001057, "VFMUL_VV_             "},\
  {0xfc0ff07f, 0x4c001057, "VFSQRT_V_             "},\
  {0xfc00707f, 0xa0001057, "VFMADD_VV_            "},\
  {0xfc00707f, 0x18001057, "VFMAX_VV_             "},\
  {0xfc00707f, 0x00001057, "VFADD_VV_             "},\
  {0xfc00707f, 0x80001057, "VFDIV_VV_             "},\
  {0xfc0ff07f, 0x48019057, "VFCVT_F_X_V_          "},\
  
  

const std::vector<care_insn_t> gvm_t::retire_care_insns = {
  XREG_INSNS
  WARP_BARRIER_INSNS
};
const std::vector<care_insn_t> gvm_t::scalar_single_insn_cmp_care_insns = {
  SCALAR_SINGLE_CMP_INSNS
};
const std::vector<care_insn_t> gvm_t::single_insn_cmp_care_insns = {
  SCALAR_SINGLE_CMP_INSNS
  VREG_INSNS
};
const std::vector<care_insn_t> gvm_t::fp32_vreg_insns = {
  FP32_VREG_INSNS
};
const std::vector<care_insn_t> gvm_t::disasm_table = {
  XREG_INSNS
  VREG_INSNS
  WARP_BARRIER_INSNS
};
const std::vector<care_insn_t> gvm_t::barrier_insns = {
  WARP_BARRIER_INSNS
};
