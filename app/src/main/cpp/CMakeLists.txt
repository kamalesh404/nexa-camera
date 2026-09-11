cmake_minimum_required(VERSION 3.22.1)
project(nexa_native)
add_library(nexa_native SHARED native_bridge.cpp)
target_compile_features(nexa_native PRIVATE cxx_std_17)
