with import <nixpkgs> {
  config = {
    packageOverrides = pkgs: with pkgs; {
      llvmPackages = llvmPackages_13;
      clang = clang_13;
      lld = lld_13;
      jdk = if stdenv.isDarwin then jdk11_headless else graalvm11-ce;
      protobuf = protobuf3_15; # required by firrtl
      python = python3Full;
    };
  };
};

let
  clang-multiple-target =
    pkgs.writeScriptBin "clang" ''
      #!${pkgs.bash}/bin/bash
      if [[ "$*" == *--target=riscv64* || "$*" == *-target\ riscv64* ]]; then
        # works partially, namely no ld
        ${pkgs.pkgsCross.riscv64.buildPackages.clang}/bin/riscv64-unknown-linux-gnu-clang \
          --target=riscv64 \
          $@
      else
        # works fully
        ${pkgs.clang}/bin/clang $@
      fi
    '';
  clangpp-multiple-target =
    pkgs.writeScriptBin "clang++" ''
      #!${pkgs.bash}/bin/bash
      if [[ "$*" == *--target=riscv64* || "$*" == *-target\ riscv64* ]]; then
        # works partially, namely no ld
        ${pkgs.pkgsCross.riscv64.buildPackages.clang}/bin/riscv64-unknown-linux-gnu-clang++ \
          --target=riscv64 \
          $@
      else
        # works fully
        ${pkgs.clang}/bin/clang++ $@
      fi
    '';
  cpp-multiple-target = pkgs.writeScriptBin "cpp" ''
    #!${pkgs.bash}/bin/bash
    ${pkgs.clang}/bin/cpp $@
  '';
  cc = if stdenv.isDarwin then [clang] else [
    clang-multiple-target
    clangpp-multiple-target
    cpp-multiple-target
  ];
in pkgs.callPackage (
  {
    mkShellNoCC,
    jdk,
    python,
    gnumake, git, mill, wget, parallel, dtc, protobuf, antlr4,
    llvmPackages, clang, lld, verilator, cmake, ninja, rcs,
    autoconf, automake
  }:

  mkShellNoCC {
    name = "ventus-gpgpu";
    depsBuildBuild = [
      jdk gnumake git mill wget parallel dtc protobuf antlr4
      verilator cmake ninja rcs autoconf automake
      llvmPackages.llvm lld
      python
      cc
    ];
  }
) {}
