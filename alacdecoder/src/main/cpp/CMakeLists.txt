cmake_minimum_required(VERSION 3.22.1)

project("alac_decoder")

add_library(${CMAKE_PROJECT_NAME} SHARED
    booming_alac.cpp
    libalac/ALACDecoder.cpp
    libalac/ALACBitUtilities.c
    libalac/ag_dec.c
    libalac/dp_dec.c
    libalac/matrix_dec.c
    libalac/EndianPortable.c)

target_link_libraries(${CMAKE_PROJECT_NAME}
    # List libraries link to the target library
    android
    log)
