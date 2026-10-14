cmake_minimum_required(VERSION 3.22.1)
project("booming_viper_dsp")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(viper_dsp SHARED
        native-bridge.cpp
)

find_library(log-lib log)

target_include_directories(viper_dsp PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(viper_dsp
        ${log-lib}
)