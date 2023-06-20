#ifndef _PARAMETERS_H
#define _PARAMETERS_H

#include "systemc.h"
// #include "tlm.h"
// #include "tlm_core/tlm_1/tlm_req_rsp/tlm_channels/tlm_fifo/tlm_fifo.h"
#include <set>
#include <queue>
#include <stack>
#include <map>
#include <bitset>
#include <math.h>
#include <iostream>
#include <bitset>
#include <unordered_map>
#include "magic_enum.hpp"

inline constexpr int num_warp = 4;
inline constexpr int depth_warp = 2;
inline constexpr int xLen = 32;
inline constexpr long unsigned int num_thread = 8;
inline constexpr int ireg_bitsize = 10;
inline constexpr int ireg_size = 1 << ireg_bitsize;
inline constexpr int INS_LENGTH = 32; // the length of per instruction
inline constexpr double PERIOD = 10;
inline constexpr int IFIFO_SIZE = 10;
inline constexpr int OPCFIFO_SIZE = 4;
inline constexpr int BANK_NUM = 4;
inline constexpr int NUM_SM = 2;

using reg_t = sc_int<32>;
using v_regfile_t = std::array<reg_t, num_thread>;
struct vector_t : std::array<reg_t, num_thread>
{
    friend std::ostream &operator<<(std::ostream &os, const v_regfile_t &arr)
    {
        os << "{" << std::hex;
        for (const auto &item : arr)
        {
            os << item << ",";
        }
        os << std::dec << "}";
        return os;
    }
    bool operator==(const std::array<reg_t, num_thread> &other) const
    {
        for (int i = 0; i < 8; ++i)
            if ((*this)[i] != other[i])
                return false;
        return true;
    }
    v_regfile_t &operator=(const std::array<reg_t, num_thread> &other)
    {
        for (int i = 0; i < 8; ++i)
        {
            (*this)[i] = other[i];
        }
        return *this;
    }
};

enum OP_TYPE
{
    INVALID_,
    ADD_,
    ADDI_,
    AMOADD_W_,
    AMOAND_W_,
    AMOMAX_W_,
    AMOMAXU_W_,
    AMOMIN_W_,
    AMOMINU_W_,
    AMOOR_W_,
    AMOSWAP_W_,
    AMOXOR_W_,
    AND_,
    ANDI_,
    AUIPC_,
    BARRIER_,
    BARRIERSUB_,
    BEQ_,
    BGE_,
    BGEU_,
    BLT_,
    BLTU_,
    BNE_,
    C_ADD_,
    C_ADDI_,
    C_ADDI16SP_,
    C_ADDI4SPN_,
    C_AND_,
    C_ANDI_,
    C_BEQZ_,
    C_BNEZ_,
    C_EBREAK_,
    C_J_,
    C_JALR_,
    C_JR_,
    C_LI_,
    C_LUI_,
    C_LW_,
    C_LWSP_,
    C_MV_,
    C_NOP_,
    C_OR_,
    C_SUB_,
    C_SW_,
    C_SWSP_,
    C_XOR_,
    CSRRC_,
    CSRRCI_,
    CSRRS_,
    CSRRSI_,
    CSRRW_,
    CSRRWI_,
    DIV_,
    DIVU_,
    EBREAK_,
    ECALL_,
    ENDPRG_,
    FADD_D_,
    FADD_S_,
    FCLASS_D_,
    FCLASS_S_,
    FCVT_D_S_,
    FCVT_D_W_,
    FCVT_D_WU_,
    FCVT_S_D_,
    FCVT_S_W_,
    FCVT_S_WU_,
    FCVT_W_D_,
    FCVT_W_S_,
    FCVT_WU_D_,
    FCVT_WU_S_,
    FDIV_D_,
    FDIV_S_,
    FENCE_,
    FEQ_D_,
    FEQ_S_,
    FLD_,
    FLE_D_,
    FLE_S_,
    FLT_D_,
    FLT_S_,
    FLW_,
    FMADD_D_,
    FMADD_S_,
    FMAX_D_,
    FMAX_S_,
    FMIN_D_,
    FMIN_S_,
    FMSUB_D_,
    FMSUB_S_,
    FMUL_D_,
    FMUL_S_,
    FMV_W_X_,
    FMV_X_W_,
    FNMADD_D_,
    FNMADD_S_,
    FNMSUB_D_,
    FNMSUB_S_,
    FSD_,
    FSGNJ_D_,
    FSGNJ_S_,
    FSGNJN_D_,
    FSGNJN_S_,
    FSGNJX_D_,
    FSGNJX_S_,
    FSQRT_D_,
    FSQRT_S_,
    FSUB_D_,
    FSUB_S_,
    FSW_,
    JAL_,
    JALR_,
    JOIN_,
    LB_,
    LBU_,
    LH_,
    LHU_,
    LR_W_,
    LUI_,
    LW_,
    MUL_,
    MULH_,
    MULHSU_,
    MULHU_,
    OR_,
    ORI_,
    REGEXT_,
    REGEXTI_,
    REM_,
    REMU_,
    SB_,
    SC_W_,
    SH_,
    SLL_,
    SLLI_,
    SLT_,
    SLTI_,
    SLTIU_,
    SLTU_,
    SRA_,
    SRAI_,
    SRL_,
    SRLI_,
    SUB_,
    SW_,
    VAADD_VV_,
    VAADD_VX_,
    VAADDU_VV_,
    VAADDU_VX_,
    VADC_VIM_,
    VADC_VVM_,
    VADC_VXM_,
    VADD12_VI_,
    VADD_VI_,
    VADD_VV_,
    VADD_VX_,
    VAMOADDEI16_V_,
    VAMOADDEI32_V_,
    VAMOADDEI64_V_,
    VAMOADDEI8_V_,
    VAMOANDEI16_V_,
    VAMOANDEI32_V_,
    VAMOANDEI64_V_,
    VAMOANDEI8_V_,
    VAMOMAXEI16_V_,
    VAMOMAXEI32_V_,
    VAMOMAXEI64_V_,
    VAMOMAXEI8_V_,
    VAMOMAXUEI16_V_,
    VAMOMAXUEI32_V_,
    VAMOMAXUEI64_V_,
    VAMOMAXUEI8_V_,
    VAMOMINEI16_V_,
    VAMOMINEI32_V_,
    VAMOMINEI64_V_,
    VAMOMINEI8_V_,
    VAMOMINUEI16_V_,
    VAMOMINUEI32_V_,
    VAMOMINUEI64_V_,
    VAMOMINUEI8_V_,
    VAMOOREI16_V_,
    VAMOOREI32_V_,
    VAMOOREI64_V_,
    VAMOOREI8_V_,
    VAMOSWAPEI16_V_,
    VAMOSWAPEI32_V_,
    VAMOSWAPEI64_V_,
    VAMOSWAPEI8_V_,
    VAMOXOREI16_V_,
    VAMOXOREI32_V_,
    VAMOXOREI64_V_,
    VAMOXOREI8_V_,
    VAND_VI_,
    VAND_VV_,
    VAND_VX_,
    VASUB_VV_,
    VASUB_VX_,
    VASUBU_VV_,
    VASUBU_VX_,
    VBEQ_,
    VBGE_,
    VBGEU_,
    VBLT_,
    VBLTU_,
    VBNE_,
    VCOMPRESS_VM_,
    VCPOP_M_,
    VDIV_VV_,
    VDIV_VX_,
    VDIVU_VV_,
    VDIVU_VX_,
    VFADD_VF_,
    VFADD_VV_,
    VFCLASS_V_,
    VFCVT_F_X_V_,
    VFCVT_F_XU_V_,
    VFCVT_RTZ_X_F_V_,
    VFCVT_RTZ_XU_F_V_,
    VFCVT_X_F_V_,
    VFCVT_XU_F_V_,
    VFDIV_VF_,
    VFDIV_VV_,
    VFEXP_V_,
    VFIRST_M_,
    VFMACC_VF_,
    VFMACC_VV_,
    VFMADD_VF_,
    VFMADD_VV_,
    VFMAX_VF_,
    VFMAX_VV_,
    VFMERGE_VFM_,
    VFMIN_VF_,
    VFMIN_VV_,
    VFMSAC_VF_,
    VFMSAC_VV_,
    VFMSUB_VF_,
    VFMSUB_VV_,
    VFMUL_VF_,
    VFMUL_VV_,
    VFMV_F_S_,
    VFMV_S_F_,
    VFMV_V_F_,
    VFNCVT_F_F_W_,
    VFNCVT_F_X_W_,
    VFNCVT_F_XU_W_,
    VFNCVT_ROD_F_F_W_,
    VFNCVT_RTZ_X_F_W_,
    VFNCVT_RTZ_XU_F_W_,
    VFNCVT_X_F_W_,
    VFNCVT_XU_F_W_,
    VFNMACC_VF_,
    VFNMACC_VV_,
    VFNMADD_VF_,
    VFNMADD_VV_,
    VFNMSAC_VF_,
    VFNMSAC_VV_,
    VFNMSUB_VF_,
    VFNMSUB_VV_,
    VFRDIV_VF_,
    VFREC7_V_,
    VFREDMAX_VS_,
    VFREDMIN_VS_,
    VFREDOSUM_VS_,
    VFREDUSUM_VS_,
    VFRSQRT7_V_,
    VFRSUB_VF_,
    VFSGNJ_VF_,
    VFSGNJ_VV_,
    VFSGNJN_VF_,
    VFSGNJN_VV_,
    VFSGNJX_VF_,
    VFSGNJX_VV_,
    VFSLIDE1DOWN_VF_,
    VFSLIDE1UP_VF_,
    VFSQRT_V_,
    VFSUB_VF_,
    VFSUB_VV_,
    VFTTA_VV_,
    VFWADD_VF_,
    VFWADD_VV_,
    VFWADD_WF_,
    VFWADD_WV_,
    VFWCVT_F_F_V_,
    VFWCVT_F_X_V_,
    VFWCVT_F_XU_V_,
    VFWCVT_RTZ_X_F_V_,
    VFWCVT_RTZ_XU_F_V_,
    VFWCVT_X_F_V_,
    VFWCVT_XU_F_V_,
    VFWMACC_VF_,
    VFWMACC_VV_,
    VFWMSAC_VF_,
    VFWMSAC_VV_,
    VFWMUL_VF_,
    VFWMUL_VV_,
    VFWNMACC_VF_,
    VFWNMACC_VV_,
    VFWNMSAC_VF_,
    VFWNMSAC_VV_,
    VFWREDOSUM_VS_,
    VFWREDUSUM_VS_,
    VFWSUB_VF_,
    VFWSUB_VV_,
    VFWSUB_WF_,
    VFWSUB_WV_,
    VID_V_,
    VIOTA_M_,
    VL1RE16_V_,
    VL1RE32_V_,
    VL1RE64_V_,
    VL1RE8_V_,
    VL2RE16_V_,
    VL2RE32_V_,
    VL2RE64_V_,
    VL2RE8_V_,
    VL4RE16_V_,
    VL4RE32_V_,
    VL4RE64_V_,
    VL4RE8_V_,
    VL8RE16_V_,
    VL8RE32_V_,
    VL8RE64_V_,
    VL8RE8_V_,
    VLB12_V_,
    VLB_V_,
    VLBU12_V_,
    VLBU_V_,
    VLE1024_V_,
    VLE1024FF_V_,
    VLE128_V_,
    VLE128FF_V_,
    VLE16_V_,
    VLE16FF_V_,
    VLE256_V_,
    VLE256FF_V_,
    VLE32_V_,
    VLE32FF_V_,
    VLE512_V_,
    VLE512FF_V_,
    VLE64_V_,
    VLE64FF_V_,
    VLE8_V_,
    VLE8FF_V_,
    VLH12_V_,
    VLH_V_,
    VLHU12_V_,
    VLHU_V_,
    VLM_V_,
    VLOXEI1024_V_,
    VLOXEI128_V_,
    VLOXEI16_V_,
    VLOXEI256_V_,
    VLOXEI32_V_,
    VLOXEI512_V_,
    VLOXEI64_V_,
    VLOXEI8_V_,
    VLSE1024_V_,
    VLSE128_V_,
    VLSE16_V_,
    VLSE256_V_,
    VLSE32_V_,
    VLSE512_V_,
    VLSE64_V_,
    VLSE8_V_,
    VLUXEI1024_V_,
    VLUXEI128_V_,
    VLUXEI16_V_,
    VLUXEI256_V_,
    VLUXEI32_V_,
    VLUXEI512_V_,
    VLUXEI64_V_,
    VLUXEI8_V_,
    VLW12_V_,
    VLW_V_,
    VMACC_VV_,
    VMACC_VX_,
    VMADC_VI_,
    VMADC_VIM_,
    VMADC_VV_,
    VMADC_VVM_,
    VMADC_VX_,
    VMADC_VXM_,
    VMADD_VV_,
    VMADD_VX_,
    VMAND_MM_,
    VMANDN_MM_,
    VMAX_VV_,
    VMAX_VX_,
    VMAXU_VV_,
    VMAXU_VX_,
    VMERGE_VIM_,
    VMERGE_VVM_,
    VMERGE_VXM_,
    VMFEQ_VF_,
    VMFEQ_VV_,
    VMFGE_VF_,
    VMFGT_VF_,
    VMFLE_VF_,
    VMFLE_VV_,
    VMFLT_VF_,
    VMFLT_VV_,
    VMFNE_VF_,
    VMFNE_VV_,
    VMIN_VV_,
    VMIN_VX_,
    VMINU_VV_,
    VMINU_VX_,
    VMNAND_MM_,
    VMNOR_MM_,
    VMOR_MM_,
    VMORN_MM_,
    VMSBC_VV_,
    VMSBC_VVM_,
    VMSBC_VX_,
    VMSBC_VXM_,
    VMSBF_M_,
    VMSEQ_VI_,
    VMSEQ_VV_,
    VMSEQ_VX_,
    VMSGT_VI_,
    VMSGT_VX_,
    VMSGTU_VI_,
    VMSGTU_VX_,
    VMSIF_M_,
    VMSLE_VI_,
    VMSLE_VV_,
    VMSLE_VX_,
    VMSLEU_VI_,
    VMSLEU_VV_,
    VMSLEU_VX_,
    VMSLT_VV_,
    VMSLT_VX_,
    VMSLTU_VV_,
    VMSLTU_VX_,
    VMSNE_VI_,
    VMSNE_VV_,
    VMSNE_VX_,
    VMSOF_M_,
    VMUL_VV_,
    VMUL_VX_,
    VMULH_VV_,
    VMULH_VX_,
    VMULHSU_VV_,
    VMULHSU_VX_,
    VMULHU_VV_,
    VMULHU_VX_,
    VMV1R_V_,
    VMV2R_V_,
    VMV4R_V_,
    VMV8R_V_,
    VMV_S_X_,
    VMV_V_I_,
    VMV_V_V_,
    VMV_V_X_,
    VMV_X_S_,
    VMXNOR_MM_,
    VMXOR_MM_,
    VNCLIP_WI_,
    VNCLIP_WV_,
    VNCLIP_WX_,
    VNCLIPU_WI_,
    VNCLIPU_WV_,
    VNCLIPU_WX_,
    VNMSAC_VV_,
    VNMSAC_VX_,
    VNMSUB_VV_,
    VNMSUB_VX_,
    VNSRA_WI_,
    VNSRA_WV_,
    VNSRA_WX_,
    VNSRL_WI_,
    VNSRL_WV_,
    VNSRL_WX_,
    VOR_VI_,
    VOR_VV_,
    VOR_VX_,
    VREDAND_VS_,
    VREDMAX_VS_,
    VREDMAXU_VS_,
    VREDMIN_VS_,
    VREDMINU_VS_,
    VREDOR_VS_,
    VREDSUM_VS_,
    VREDXOR_VS_,
    VREM_VV_,
    VREM_VX_,
    VREMU_VV_,
    VREMU_VX_,
    VRGATHER_VI_,
    VRGATHER_VV_,
    VRGATHER_VX_,
    VRGATHEREI16_VV_,
    VRSUB_VI_,
    VRSUB_VX_,
    VS1R_V_,
    VS2R_V_,
    VS4R_V_,
    VS8R_V_,
    VSADD_VI_,
    VSADD_VV_,
    VSADD_VX_,
    VSADDU_VI_,
    VSADDU_VV_,
    VSADDU_VX_,
    VSB12_V_,
    VSB_V_,
    VSBC_VVM_,
    VSBC_VXM_,
    VSE1024_V_,
    VSE128_V_,
    VSE16_V_,
    VSE256_V_,
    VSE32_V_,
    VSE512_V_,
    VSE64_V_,
    VSE8_V_,
    VSETIVLI_,
    VSETVL_,
    VSETVLI_,
    VSEXT_VF2_,
    VSEXT_VF4_,
    VSEXT_VF8_,
    VSH12_V_,
    VSH_V_,
    VSLIDE1DOWN_VX_,
    VSLIDE1UP_VX_,
    VSLIDEDOWN_VI_,
    VSLIDEDOWN_VX_,
    VSLIDEUP_VI_,
    VSLIDEUP_VX_,
    VSLL_VI_,
    VSLL_VV_,
    VSLL_VX_,
    VSM_V_,
    VSMUL_VV_,
    VSMUL_VX_,
    VSOXEI1024_V_,
    VSOXEI128_V_,
    VSOXEI16_V_,
    VSOXEI256_V_,
    VSOXEI32_V_,
    VSOXEI512_V_,
    VSOXEI64_V_,
    VSOXEI8_V_,
    VSRA_VI_,
    VSRA_VV_,
    VSRA_VX_,
    VSRL_VI_,
    VSRL_VV_,
    VSRL_VX_,
    VSSE1024_V_,
    VSSE128_V_,
    VSSE16_V_,
    VSSE256_V_,
    VSSE32_V_,
    VSSE512_V_,
    VSSE64_V_,
    VSSE8_V_,
    VSSRA_VI_,
    VSSRA_VV_,
    VSSRA_VX_,
    VSSRL_VI_,
    VSSRL_VV_,
    VSSRL_VX_,
    VSSUB_VV_,
    VSSUB_VX_,
    VSSUBU_VV_,
    VSSUBU_VX_,
    VSUB12_VI_,
    VSUB_VV_,
    VSUB_VX_,
    VSUXEI1024_V_,
    VSUXEI128_V_,
    VSUXEI16_V_,
    VSUXEI256_V_,
    VSUXEI32_V_,
    VSUXEI512_V_,
    VSUXEI64_V_,
    VSUXEI8_V_,
    VSW12_V_,
    VSW_V_,
    VWADD_VV_,
    VWADD_VX_,
    VWADD_WV_,
    VWADD_WX_,
    VWADDU_VV_,
    VWADDU_VX_,
    VWADDU_WV_,
    VWADDU_WX_,
    VWMACC_VV_,
    VWMACC_VX_,
    VWMACCSU_VV_,
    VWMACCSU_VX_,
    VWMACCU_VV_,
    VWMACCU_VX_,
    VWMACCUS_VX_,
    VWMUL_VV_,
    VWMUL_VX_,
    VWMULSU_VV_,
    VWMULSU_VX_,
    VWMULU_VV_,
    VWMULU_VX_,
    VWREDSUM_VS_,
    VWREDSUMU_VS_,
    VWSUB_VV_,
    VWSUB_VX_,
    VWSUB_WV_,
    VWSUB_WX_,
    VWSUBU_VV_,
    VWSUBU_VX_,
    VWSUBU_WV_,
    VWSUBU_WX_,
    VXOR_VI_,
    VXOR_VV_,
    VXOR_VX_,
    VZEXT_VF2_,
    VZEXT_VF4_,
    VZEXT_VF8_,
    XOR_,
    XORI_,
};
struct instable_t
{
    std::bitset<32> mask;
    std::unordered_map<std::bitset<32>, OP_TYPE> itable;
};

namespace DecodeParams
{
    enum branch_t
    {
        B_N,
        B_B,
        B_J,
        B_R,
    };
    enum csr_t
    {
        CSR_N,
        CSR_W,
        CSR_S,
        CSR_C,
    };
    enum sel_alu3_t
    {
        A3_X, // 不需要源操作数
        A3_FRS3,
        A3_VRS3,
        A3_SD,
        A3_PC,
    };
    enum sel_alu2_t
    {
        A2_X,
        A2_RS2,
        A2_IMM,
        A2_VRS2,
        A2_SIZE,
    };
    enum sel_alu1_t
    {
        A1_X,
        A1_RS1,
        A1_VRS1,
        A1_IMM,
        A1_PC,
    };
    enum sel_imm_t
    {
        IMM_B,
        IMM_J,
        IMM_I,
        IMM_U,
        IMM_X,
        IMM_Z,
        IMM_S,
        IMM_2,
        IMM_V,
        IMM_L11,
        IMM_S11,
    };
    enum mem_whb_t
    {
        MEM_X,
        MEM_W,
        MEM_B,
        MEM_H,
    };
    enum alu_fn_t
    {
        FN_X,
        FN_ADD,
        FN_SL,
        FN_SEQ,
        FN_SNE,
        FN_XOR,
        FN_SR,
        FN_OR,
        FN_AND,
        FN_SUB,
        FN_SRA,
        FN_SLT,
        FN_SGE,
        FN_SLTU,
        FN_SGEU,
        FN_MAX,
        FN_MIN,
        FN_MAXU,
        FN_MINU,
        FN_A1ZERO,
        FN_MUL,
        FN_MULH,
        FN_MULHU,
        FN_MULHSU,
        FN_MACC,
        FN_NMSAC,
        FN_MADD,
        FN_NMSUB,
        FN_VMNOR,
        FN_VMNAND,
        FN_VMXNOR,
        FN_VMORNOT,
        FN_VMANDNOT,
        FN_VID,
        FN_VMERGE,
        FN_FADD,
        FN_FSUB,
        FN_FMUL,
        FN_FMADD,
        FN_FMSUB,
        FN_FNMSUB,
        FN_FNMADD,
        FN_VFMADD,
        FN_VFMSUB,
        FN_VFNMSUB,
        FN_VFNMADD,
        FN_FMIN,
        FN_FMAX,
        FN_FLE,
        FN_FLT,
        FN_FEQ,
        FN_FNE,
        FN_FCLASS,
        FN_FSGNJ,
        FN_FSGNJN,
        FN_FSGNJX,
        FN_F2IU,
        FN_F2I,
        FN_IU2F,
        FN_I2F,
        FN_DIV,
        FN_REM,
        FN_DIVU,
        FN_REMU,
        FN_FDIV,
        FN_FSQRT,
        FN_EXP,
        FN_TTF,
        FN_TTH,
        FN_TTB,
    };
    enum mem_t
    {
        M_X,
        M_XRD,
        M_XWR,
    };

    // 自己添加的decode信号：
    enum sel_execunit_t
    {
        INVALID_EXECUNIT = 0,
        SALU,
        MUL,
        VALU,
        VFPU,
        LSU,
        SFU,
        CSR,
        SIMTSTK,
        TC, // tensor core
        WPSCHEDLER,
    };
}

class decodedat
{
public:
    bool isvec = 0; // float指令标量向量共用向量寄存器，标量指令的写回不能被向量指令的mask影响
    bool fp;
    bool barrier;
    DecodeParams::branch_t branch;
    bool simt_stack;
    bool simt_stack_op;
    DecodeParams::csr_t csr;
    bool reverse;
    DecodeParams::sel_alu3_t sel_alu3;
    DecodeParams::sel_alu2_t sel_alu2;
    DecodeParams::sel_alu1_t sel_alu1;
    DecodeParams::sel_imm_t sel_imm;
    DecodeParams::mem_whb_t mem_whb;
    DecodeParams::alu_fn_t alu_fn;
    bool mul;
    DecodeParams::mem_t mem_cmd;
    bool mem_unsigned;
    bool fence;
    bool sfu;
    bool wvd; // write vector register file
    bool readmask;
    bool writemask;
    bool wxd; // write scalar register file
    bool tc;
    bool disable_mask;
    bool undefined1;
    bool undefined2;

    // 不在初始化列表里的信号
    bool mem;

    // 自己加的decode信号
    DecodeParams::sel_execunit_t sel_execunit;

    decodedat &operator=(const decodedat &rhs) = default;
};
class I_TYPE // type of per instruction
{
public:
    uint32_t origin32bit; // 原始32位指令
    int op;               // sc_trace不支持enum，只能定义op为int型
    int d = -1;           // beq指令为imm
    int s1 = -1;          // load指令为寄存器addr
    int s2 = -1;          // load指令为offset, addi、auipc指令为imm
    int s3 = -1;          // fmadd等指令使用
    int imm = -1;
    decodedat ddd;
    // int jump_addr = -1; // 分支指令才有用
    int currentpc; // 每条指令当前pc，取指后赋予
    sc_bv<num_thread> mask;

    I_TYPE(){};
    I_TYPE(uint32_t origin) : origin32bit(origin){};
    I_TYPE(OP_TYPE _op, int _d, int _s1, int _s2) : op(_op), d(_d), s1(_s1), s2(_s2){};
    I_TYPE(I_TYPE _ins, int _currentpc) : origin32bit(_ins.origin32bit), op(_ins.op), d(_ins.d), s1(_ins.s1), s2(_ins.s2), s3(_ins.s3), currentpc(_currentpc){};
    bool operator==(const I_TYPE &rhs) const
    {
        return rhs.origin32bit == origin32bit && rhs.op == op && rhs.s1 == s1 && rhs.s2 == s2 && rhs.s3 == s3 && rhs.d == d;
    }
    I_TYPE &operator=(const I_TYPE &rhs)
    {
        origin32bit = rhs.origin32bit;
        op = rhs.op;
        d = rhs.d;
        s1 = rhs.s1;
        s2 = rhs.s2;
        s3 = rhs.s3;
        imm = rhs.imm;
        ddd = rhs.ddd;
        // jump_addr = rhs.jump_addr;
        currentpc = rhs.currentpc;
        mask = rhs.mask;
        return *this;
    }
    friend ostream &operator<<(ostream &os, I_TYPE const &v)
    {
        os << "(" << (magic_enum::enum_name((OP_TYPE)v.op)) << "," << v.d << "," << v.s1 << "," << v.s2 << "," << v.s3 << ")";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const I_TYPE &v, const std::string &NAME)
    {
        sc_trace(tf, v.origin32bit, NAME + ".ins_bit");
        sc_trace(tf, v.op, NAME + ".op");
        sc_trace(tf, v.s1, NAME + ".s1");
        sc_trace(tf, v.s2, NAME + ".s2");
        sc_trace(tf, v.s2, NAME + ".s3");
        sc_trace(tf, v.d, NAME + ".d");
        sc_trace(tf, v.imm, NAME + ".imm");
        // sc_trace(tf, v.jump_addr, NAME + ".jump_addr");
        sc_trace(tf, v.mask, NAME + ".mask");
        sc_trace(tf, v.currentpc, NAME + ".currentpc");
    }
};
// typename I_TYPE sc_uint<INS_LENGTH>;

class event_if : virtual public sc_interface // "if" means interface
{
public:
    virtual const sc_event &obtain_event() const = 0;
    virtual void notify() = 0;
    virtual void notify(const double time_) = 0;
};
class event : public sc_module, public event_if
{
public:
    event(sc_module_name _name) : sc_module(_name) {}
    const sc_event &obtain_event() const { return self_event; }
    void notify() { self_event.notify(); }
    void notify(const double time_) { self_event.notify(time_, SC_NS); }

private:
    sc_event self_event;
};

enum REG_TYPE
{
    s = 1,
    v,
    csr,
};
class SCORE_TYPE // every score in scoreboard
{
public:
    enum REG_TYPE regtype; // record to write scalar reg or vector reg
    int addr;
    bool operator<(const SCORE_TYPE &t_) const
    {
        if (regtype == t_.regtype)
            return addr < t_.addr;
        else
            return regtype < t_.regtype;
    }
    friend ostream &operator<<(ostream &os, SCORE_TYPE const &v)
    {
        os << "(regtype:" << v.regtype << ",addr:" << v.addr << ")";
        return os;
    }
    SCORE_TYPE(REG_TYPE regtype_, int addr_) : regtype(regtype_), addr(addr_){};
};
struct bank_t
{
    int bank_id;
    int addr;
    friend std::ostream &operator<<(std::ostream &os, const bank_t &arr)
    {
        os << "bank" << arr.bank_id << "-" << arr.addr;
        return os;
    }
};
struct warpaddr_t
{
    int warp_id;
    int addr;
};
struct opcfifo_t
{
    // 进入opcfifo时，若要取操作数，令valid=1，等待regfile返回ready=1
    // 若是立即数，不用取操作数，令valid=0且直接令ready=1
    // 只要ready=1，就可以发射
    I_TYPE ins;
    int warp_id;
    std::array<bool, 3> ready = {0};
    std::array<bool, 3> valid = {0};
    std::array<bank_t, 3> srcaddr;
    std::array<bool, 3> banktype = {0};
    // int mask;
    std::array<std::array<reg_t, num_thread>, 3> data;
    bool all_ready()
    {
        return ready[0] && ready[1] && ready[2];
    }
    opcfifo_t(){};
    opcfifo_t(I_TYPE ins_) : ins(ins_){};
    opcfifo_t(I_TYPE ins_, int warp_id_,
              const std::array<bool, 3> &ready_arr,
              const std::array<bool, 3> &valid_arr,
              const std::array<bank_t, 3> &srcaddr_arr,
              const std::array<bool, 3> &banktype_arr)
        : ins(ins_), warp_id(warp_id_), ready(ready_arr), valid(valid_arr),
          srcaddr(srcaddr_arr), banktype(banktype_arr){};
};

template <typename T, size_t N>
class StaticEntry
{ // for OPC entry
private:
    std::array<T, N> data_;
    std::array<bool, N> tag_; // 标志位置是否有效
    size_t size_;

public:
    StaticEntry() : size_(0)
    {
        tag_.fill(false);
    }
    void clear()
    {
        tag_.fill(false);
    }
    void push(const T &value)
    {
        if (size_ < N)
        {
            for (size_t i = 0; i < N; ++i)
            {
                if (tag_[i] == false)
                {
                    data_[i] = value;
                    tag_[i] = true;
                    ++size_;
                    break;
                }
            }
        }
        else
        {
            throw std::out_of_range("StaticEntry full but push data");
        }
    }

    void pop(size_t index)
    {
        if (index >= N || !tag_[index])
        {
            throw std::out_of_range("Invalid index");
            return;
        }

        tag_[index] = false;
        --size_;
    }

    T &operator[](size_t index)
    {
        return data_[index];
    }

    const T &operator[](size_t index) const
    {
        return data_[index];
    }
    bool tag_valid(size_t index) const
    {
        return tag_[index];
    }
    size_t get_size() const
    {
        return size_;
    }

    // 以下函数是为了使用范围-based for循环
    T *begin() { return data_.begin(); }
    T *end() { return data_.end(); }
    const T *begin() const { return data_.begin(); }
    const T *end() const { return data_.end(); }
};

template <typename T, std::size_t capacity>
class StaticQueue
{
private:
    std::array<T, capacity> data;
    std::size_t size;
    std::size_t front_index;

public:
    StaticQueue() : size(0), front_index(0) {}
    void push(const T &value)
    {
        if (size == capacity)
        {
            throw std::out_of_range("Queue is full");
        }
        data[(front_index + size) % capacity] = value;
        ++size;
    }
    void pop()
    {
        if (size == 0)
        {
            throw std::out_of_range("StaticQueue is empty");
        }
        front_index = (front_index + 1) % capacity;
        --size;
    }
    void clear()
    {
        while (!isempty())
            pop();
    }
    T get()
    { // return front and pop
        if (size == 0)
        {
            throw std::out_of_range("StaticQueue is empty");
        }
        T re = data[front_index];
        front_index = (front_index + 1) % capacity;
        --size;
        return re;
    }
    T &front()
    {
        if (size == 0)
        {
            throw std::out_of_range("StaticQueue is empty");
        }
        return data[front_index];
    }
    const T &front() const
    {
        if (size == 0)
        {
            throw std::out_of_range("StaticQueue is empty");
        }
        return data[front_index];
    }
    T &operator[](std::size_t index)
    { // front对应索引0
        return data[(front_index + index) % capacity];
    }
    const T &operator[](std::size_t index) const
    {
        return data[(front_index + index) % capacity];
    }
    T &at(size_t index) // 与[]不同，at()包含边界检查
    {
        if (index >= size)
        {
            throw std::out_of_range("Index out of range");
        }
        return data[(front_index + index) % capacity];
    }
    const T &at(size_t index) const
    {
        if (index >= size)
        {
            throw std::out_of_range("Index out of range");
        }
        return data[(front_index + index) % capacity];
    }
    bool isempty() const
    {
        return size == 0;
    }
    bool isfull() const
    {
        return size == capacity;
    }
    size_t used() const
    {
        return size;
    }
    size_t get_capacity() const
    {
        return capacity;
    }
};

struct salu_in_t
{
    I_TYPE ins;
    int warp_id;
    reg_t rss1_data;
    reg_t rss2_data;
    reg_t rss3_data;
};
struct salu_out_t
{
    I_TYPE ins;
    int warp_id;
    reg_t data; // 计算出的数据
    bool operator==(const salu_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.data == data;
    }
    salu_out_t &operator=(const salu_out_t &rhs)
    {
        ins = rhs.ins;
        data = rhs.data;
        warp_id = rhs.warp_id;
        return *this;
    }
    friend ostream &operator<<(ostream &os, salu_out_t const &v)
    {
        os << "(" << v.ins << "," << v.data << ")";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const salu_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.warp_id, NAME + ".warp_id");
        sc_trace(tf, v.data, NAME + ".data");
    }
};
struct valu_in_t
{
    I_TYPE ins;
    int warp_id;
    std::array<reg_t, num_thread> rsv1_data, rsv2_data;
    reg_t rss1_data;
};
struct valu_out_t
{
    I_TYPE ins;
    int warp_id;
    std::array<reg_t, num_thread> rdv1_data;
    bool operator==(const valu_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.rdv1_data == rdv1_data;
    }
    valu_out_t &operator=(const valu_out_t &rhs)
    {
        ins = rhs.ins;
        rdv1_data = rhs.rdv1_data;
        warp_id = rhs.warp_id;
        return *this;
    }
    friend ostream &operator<<(ostream &os, valu_out_t const &v)
    {
        os << "{" << v.ins << ";";
        auto it = v.rdv1_data.begin();
        while (it != v.rdv1_data.end())
        {
            os << *it << " ";
            it = std::next(it);
        }
        os << "}";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const valu_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.rdv1_data[0], NAME + ".rdv1_data(0)");
        sc_trace(tf, v.rdv1_data[1], NAME + ".rdv1_data(1)");
        sc_trace(tf, v.rdv1_data[2], NAME + ".rdv1_data(2)");
        sc_trace(tf, v.rdv1_data[3], NAME + ".rdv1_data(3)");
        sc_trace(tf, v.rdv1_data[4], NAME + ".rdv1_data(4)");
        sc_trace(tf, v.rdv1_data[5], NAME + ".rdv1_data(5)");
        sc_trace(tf, v.rdv1_data[6], NAME + ".rdv1_data(6)");
        sc_trace(tf, v.rdv1_data[7], NAME + ".rdv1_data(7)");
        sc_trace(tf, v.warp_id, NAME + ".warp_id");
    }
};
struct vfpu_in_t
{
    I_TYPE ins;
    int warp_id;
    std::array<int, num_thread> vfpuSdata1, vfpuSdata2, vfpuSdata3;
};
struct vfpu_out_t
{
    I_TYPE ins;
    int warp_id;
    std::array<int, num_thread> rdf1_data;
    reg_t rds1_data; // FCVT_W_S等指令使用
    bool operator==(const vfpu_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.rdf1_data == rdf1_data;
    }
    vfpu_out_t &operator=(const vfpu_out_t &rhs)
    {
        ins = rhs.ins;
        rdf1_data = rhs.rdf1_data;
        warp_id = rhs.warp_id;
        return *this;
    }
    friend ostream &operator<<(ostream &os, vfpu_out_t const &v)
    {
        os << "{" << v.ins << ";";
        auto it = v.rdf1_data.begin();
        while (it != v.rdf1_data.end())
        {
            os << *it << " ";
            it = std::next(it);
        }
        os << "}";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const vfpu_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.rdf1_data[0], NAME + ".rdf1_data(0)");
        sc_trace(tf, v.rdf1_data[1], NAME + ".rdf1_data(1)");
        sc_trace(tf, v.rdf1_data[2], NAME + ".rdf1_data(2)");
        sc_trace(tf, v.rdf1_data[3], NAME + ".rdf1_data(3)");
        sc_trace(tf, v.rdf1_data[4], NAME + ".rdf1_data(4)");
        sc_trace(tf, v.rdf1_data[5], NAME + ".rdf1_data(5)");
        sc_trace(tf, v.rdf1_data[6], NAME + ".rdf1_data(6)");
        sc_trace(tf, v.rdf1_data[7], NAME + ".rdf1_data(7)");
        sc_trace(tf, v.warp_id, NAME + ".warp_id");
    }
};
struct lsu_in_t
{
    I_TYPE ins;
    int warp_id;
    reg_t rss1_data;
    int rss2_data;
    int rss3_data;
    // below 3 data is to store
    reg_t rds1_data;
    std::array<reg_t, num_thread> rdv1_data;
    std::array<float, num_thread> rdf1_data;
};
struct lsu_out_t
{
    I_TYPE ins;
    int warp_id;
    reg_t rds1_data;
    std::array<reg_t, num_thread> rdv1_data;
    std::array<float, num_thread> rdf1_data;
    bool operator==(const lsu_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.rds1_data == rds1_data &&
               rhs.rdv1_data == rdv1_data && rhs.rdf1_data == rdf1_data;
    }
    lsu_out_t &operator=(const lsu_out_t &rhs)
    {
        ins = rhs.ins;
        rds1_data = rhs.rds1_data;
        rdv1_data = rhs.rdv1_data;
        rdf1_data = rhs.rdf1_data;
        warp_id = rhs.warp_id;
        return *this;
    }
    friend ostream &operator<<(ostream &os, lsu_out_t const &v)
    {
        os << "{" << v.ins << ";" << v.rds1_data << ";";
        auto it = v.rdv1_data.begin();
        while (it != v.rdv1_data.end())
        {
            os << *it << " ";
            it = std::next(it);
        }
        os << ";";
        auto itf = v.rdf1_data.begin();
        while (itf != v.rdf1_data.end())
        {
            os << *itf << " ";
            itf = std::next(itf);
        }
        os << "}";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const lsu_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.rds1_data, NAME + ".rds1_data");
        sc_trace(tf, v.rdv1_data[0], NAME + ".rdv1_data(0)");
        sc_trace(tf, v.rdv1_data[1], NAME + ".rdv1_data(1)");
        sc_trace(tf, v.rdv1_data[2], NAME + ".rdv1_data(2)");
        sc_trace(tf, v.rdv1_data[3], NAME + ".rdv1_data(3)");
        sc_trace(tf, v.rdv1_data[4], NAME + ".rdv1_data(4)");
        sc_trace(tf, v.rdv1_data[5], NAME + ".rdv1_data(5)");
        sc_trace(tf, v.rdv1_data[6], NAME + ".rdv1_data(6)");
        sc_trace(tf, v.rdv1_data[7], NAME + ".rdv1_data(7)");
        sc_trace(tf, v.rdf1_data[0], NAME + ".rdf1_data(0)");
        sc_trace(tf, v.rdf1_data[1], NAME + ".rdf1_data(1)");
        sc_trace(tf, v.rdf1_data[2], NAME + ".rdf1_data(2)");
        sc_trace(tf, v.rdf1_data[3], NAME + ".rdf1_data(3)");
        sc_trace(tf, v.rdf1_data[4], NAME + ".rdf1_data(4)");
        sc_trace(tf, v.rdf1_data[5], NAME + ".rdf1_data(5)");
        sc_trace(tf, v.rdf1_data[6], NAME + ".rdf1_data(6)");
        sc_trace(tf, v.rdf1_data[7], NAME + ".rdf1_data(7)");
        sc_trace(tf, v.warp_id, NAME + ".warp_id");
    }
};

class simtstack_t
{
    // 对于SIMT-stack来说，else分支（对于elsemask等数据）指的是分支指令判断跳转的path，
    // 无论是beq还是bne，也不用管编程模型定义的if和else。
public:
    int rpc;                 // 在ventus中没有用处，因为存入simt-stack时不知道rpc
    sc_bv<num_thread> rmask; // 汇合点mask
    int elsepc;
    sc_bv<num_thread> elsemask;
    bool is_part;
    bool pair;
    friend ostream &operator<<(ostream &os, simtstack_t const &v)
    {
        os << "(rmask" << v.rmask << ",elsemask" << v.elsemask << ",elsepc" << v.elsepc << ",ispart" << v.is_part << ",pair" << v.pair << ")";
        return os;
    }
};
struct csr_in_t
{
    I_TYPE ins;
    int warp_id;
    reg_t csrSdata1;
    reg_t csrSdata2;
};
struct csr_out_t
{
    I_TYPE ins;
    int warp_id;
    reg_t data; // 计算出的数据
    bool operator==(const csr_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.data == data;
    }
    csr_out_t &operator=(const csr_out_t &rhs)
    {
        ins = rhs.ins;
        data = rhs.data;
        warp_id = rhs.warp_id;
        return *this;
    }
    friend ostream &operator<<(ostream &os, csr_out_t const &v)
    {
        os << "(" << v.ins << "," << v.data << ")";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const csr_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.warp_id, NAME + ".warp_id");
        sc_trace(tf, v.data, NAME + ".data");
    }
};
struct mul_in_t
{
    I_TYPE ins;
    int warp_id;
    std::array<reg_t, num_thread> rsv1_data, rsv2_data;
    reg_t rss1_data;
};
struct mul_out_t
{
    I_TYPE ins;
    int warp_id;
    std::array<reg_t, num_thread> rdv1_data;
    bool operator==(const mul_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.rdv1_data == rdv1_data;
    }
    mul_out_t &operator=(const mul_out_t &rhs)
    {
        ins = rhs.ins;
        rdv1_data = rhs.rdv1_data;
        warp_id = rhs.warp_id;
        return *this;
    }
    friend ostream &operator<<(ostream &os, mul_out_t const &v)
    {
        os << "{" << v.ins << ";";
        auto it = v.rdv1_data.begin();
        while (it != v.rdv1_data.end())
        {
            os << *it << " ";
            it = std::next(it);
        }
        os << "}";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const mul_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.rdv1_data[0], NAME + ".rdv1_data(0)");
        sc_trace(tf, v.rdv1_data[1], NAME + ".rdv1_data(1)");
        sc_trace(tf, v.rdv1_data[2], NAME + ".rdv1_data(2)");
        sc_trace(tf, v.rdv1_data[3], NAME + ".rdv1_data(3)");
        sc_trace(tf, v.rdv1_data[4], NAME + ".rdv1_data(4)");
        sc_trace(tf, v.rdv1_data[5], NAME + ".rdv1_data(5)");
        sc_trace(tf, v.rdv1_data[6], NAME + ".rdv1_data(6)");
        sc_trace(tf, v.rdv1_data[7], NAME + ".rdv1_data(7)");
        sc_trace(tf, v.warp_id, NAME + ".warp_id");
    }
};

struct sfu_in_t
{
    I_TYPE ins;
    int warp_id;
    std::array<reg_t, num_thread> rsv1_data, rsv2_data;
    reg_t rss1_data;
};
struct sfu_out_t
{
    I_TYPE ins;
    int warp_id;
    std::array<reg_t, num_thread> rdv1_data;
    bool operator==(const sfu_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.rdv1_data == rdv1_data;
    }
    sfu_out_t &operator=(const sfu_out_t &rhs)
    {
        ins = rhs.ins;
        rdv1_data = rhs.rdv1_data;
        warp_id = rhs.warp_id;
        return *this;
    }
    friend ostream &operator<<(ostream &os, sfu_out_t const &v)
    {
        os << "{" << v.ins << ";";
        auto it = v.rdv1_data.begin();
        while (it != v.rdv1_data.end())
        {
            os << *it << " ";
            it = std::next(it);
        }
        os << "}";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const sfu_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.rdv1_data[0], NAME + ".rdv1_data(0)");
        sc_trace(tf, v.rdv1_data[1], NAME + ".rdv1_data(1)");
        sc_trace(tf, v.rdv1_data[2], NAME + ".rdv1_data(2)");
        sc_trace(tf, v.rdv1_data[3], NAME + ".rdv1_data(3)");
        sc_trace(tf, v.rdv1_data[4], NAME + ".rdv1_data(4)");
        sc_trace(tf, v.rdv1_data[5], NAME + ".rdv1_data(5)");
        sc_trace(tf, v.rdv1_data[6], NAME + ".rdv1_data(6)");
        sc_trace(tf, v.rdv1_data[7], NAME + ".rdv1_data(7)");
        sc_trace(tf, v.warp_id, NAME + ".warp_id");
    }
};

class WARP_BONE
{
public:
    WARP_BONE()
    {
        current_mask.write(~sc_bv<num_thread>());
    }

    bool is_warp_activated;

    // fetch
    sc_event ev_fetchpc, ev_decode;
    sc_signal<bool> ibuf_swallow; // 表示是否接收上一cycle fetch_valid，相当于ready
    sc_signal<bool> fetch_valid{"fetch_valid"}, fetch_valid2{"fetch_valid2"};   // 2是真正的valid，直接与ibuffer沟通
    sc_signal<bool, SC_MANY_WRITERS> jump{"jump"}, branch_sig{"branch_sig"}, vbran_sig{"vbran_sig"}; // 无论是否jump，只要发生了分支判断，将branch_sig置为1
    sc_signal<int> jump_addr{"jump_addr"}, pc{"pc"};
    I_TYPE fetch_ins;
    sc_signal<I_TYPE> decode_ins{"decode_ins"};
    // ibuffer
    sc_event ev_ibuf_updated;
    sc_signal<bool> ibuf_empty{"ibuf_empty"}, ibuf_full{"ibuf_full"};
    sc_signal<I_TYPE> ibuftop_ins{"ibuftop_ins"};
    StaticQueue<I_TYPE, IFIFO_SIZE> ififo;
    sc_signal<int> ififo_elem_num;
    // scoreboard
    sc_event ev_judge_dispatch;
    bool can_dispatch;
    sc_signal<bool> dispatch_warp_valid{"dispatch_warp_valid"};
    I_TYPE _scoretmpins;
    std::set<SCORE_TYPE> score; // record regfile addr that's to be written
    bool wait_bran;             // 应该使用C++类型；dispatch了分支指令，则要暂停dispatch等待分支指令被执行
    // issue
    sc_event ev_issue;
    // regfile
    std::array<reg_t, 32> s_regfile;
    std::array<v_regfile_t, 32> v_regfile;
    std::array<int, 12> CSR_reg;
    // simt-stack
    std::stack<simtstack_t> simt_stack;
    sc_signal<sc_bv<num_thread>> current_mask; // 在dispatch时随指令存入OPC
    sc_signal<int> simtstk_jumpaddr;           // out_pc
    sc_signal<bool> simtstk_jump;              // fetch跳转的控制信号
    sc_signal<bool> simtstk_flush;             // 流水线冲刷信号
};

// union FloatAndInt
// {
//     float f;
//     int i;
// };
// inline std::ostream &operator<<(std::ostream &os, const FloatAndInt &val)
// {
//     os << val.f;
//     return os;
// };
// inline bool operator==(const FloatAndInt &left, const FloatAndInt &right)
// {
//     return left.i == right.i;
// };

struct meta_data
{ // 这个metadata是供驱动使用的，而不是给硬件的
    uint64_t kernel_id;
    uint64_t kernel_size[3];   ///> 每个kernel的workgroup三维数目
    uint64_t wf_size;          ///> 每个warp的thread数目
    uint64_t wg_size;          ///> 每个workgroup的warp数目
    uint64_t metaDataBaseAddr; ///> CSR_KNL的值，
    uint64_t ldsSize;          ///> 每个workgroup使用的local memory的大小
    uint64_t pdsSize;          ///> 每个thread用到的private memory大小
    uint64_t sgprUsage;        ///> 每个workgroup使用的标量寄存器数目
    uint64_t vgprUsage;        ///> 每个thread使用的向量寄存器数目
    uint64_t pdsBaseAddr;      ///> private memory的基址，要转成每个workgroup的基地址， wf_size*wg_size*pdsSize
    uint64_t num_buffer;       ///> buffer的数目，包括pc
    uint64_t *buffer_base;     ///> 各buffer的基址。第一块buffer是给硬件用的metadata
    uint64_t *buffer_size;     ///> 各buffer的size，以Bytes为单位
};


uint32_t extractBits32(uint32_t number, int start, int end);

#endif
