# Instructions on installing PANDA

This is a list of setup instructions to assist with installing PANDA
on Debian 8.

## Installation:
  - Install capstone 3.0 from source (github)
    - https://github.com/aquynh/capstone
    - Make sure you just run `./make.sh` and not `./make.sh nix32`
    - After running `sudo ./make install`, make sure to add /usr/lib64/
      to LD_LIBRARY_PATH so PANDA knows where to look.
      `$ LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib64/`

    - Install LLVM from source as mentioned in PANDA's instructions
      (https://github.com/panda-re/panda/blob/master/panda/docs/manual.md#llvm)

    - Install protobuf 2.6.1
```
      $ wget https://github.com/google/protobuf/releases/download/v2.6.1/protobuf-2.6.1.tar.gz
      $ tar xzf protobuf-2.6.1.tar.gz
      $ cd protobuf-2.6.1
      $ sudo apt-get update
      $ sudo apt-get install build-essential
      $ sudo ./configure
      $ sudo make
      $ sudo make check
      $ sudo make install
      $ sudo ldconfig
      $ protoc --version
```

    - Install dependencies from panda/scripts/install_ubuntu.sh
      - Do NOT install protobuf, llvm, or capstone dependencies
      - This is because Debian Jessie only comes with capstone2, protobuf
        3.2.0, and llvm 3.5

## Compilation:
  - Compile PANDA
    - Make sure the "want_tools" option is set to "yes" in "configure" file
    - This is so you can run `qemu-img` to create a new image
    - You probably also want to modify "build.sh" to "disable-xen" as well
    - `$ mkdir build; cd build`
    - `$ ../build.sh`
