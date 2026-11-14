cmake_minimum_required(VERSION 3.22.1)
project(cadence_native LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
find_package(oboe CONFIG REQUIRED)
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/fvad.cmake)
file(GLOB_RECURSE CADENCE_SOURCES CONFIGURE_DEPENDS
    ${CMAKE_CURRENT_SOURCE_DIR}/jni/*.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/engine/*.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/dsp/*.cpp)
add_library(cadence_native SHARED ${CADENCE_SOURCES})
target_include_directories(cadence_native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(cadence_native oboe::oboe android log fvad)
