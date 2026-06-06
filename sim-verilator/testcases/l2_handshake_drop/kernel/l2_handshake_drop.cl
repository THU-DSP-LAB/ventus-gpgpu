__kernel void l2_handshake_drop(__global uint *out) {
  uint gid = get_global_id(0);
  uint group = get_group_id(0);
  uint lid = get_local_id(0);
  uint stride_words = 32; // 128-byte cache-line stride for 32-bit words.
  uint group_words = 1u << 20;
  __global uint *base = out + group * group_words;

  if (group == 0) {
    uint value = 0x5a5a0000u ^ gid;
    for (uint i = 0; i < 512; ++i) {
      base[i * stride_words + lid] = value + i;
    }
    return;
  }

  uint value = 0x6b6b0000u ^ gid;
  for (uint i = 0; i < 8192; ++i) {
    base[i * stride_words + lid] = value + i;
    value += base[i * stride_words + lid];
  }
}
