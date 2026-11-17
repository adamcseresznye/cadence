cmake_minimum_required(VERSION 3.20)
project(cadence_native_tests LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
set(gtest_force_shared_crt ON)
FetchContent_Declare(
    googletest
    URL https://github.com/google/googletest/archive/refs/tags/v1.15.2.zip
)
FetchContent_MakeAvailable(googletest)

enable_testing()
include(GoogleTest)

set(CADENCE_CPP "${CMAKE_CURRENT_SOURCE_DIR}/../app/src/main/cpp")
include(${CMAKE_CURRENT_SOURCE_DIR}/../third_party/fvad.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../third_party/rnnoise.cmake)

file(GLOB TEST_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/tests/*.cpp)
file(GLOB DSP_SOURCES CONFIGURE_DEPENDS ${CADENCE_CPP}/dsp/*.cpp)

add_executable(dsp_tests ${TEST_SOURCES} ${DSP_SOURCES})
target_include_directories(dsp_tests PRIVATE ${CADENCE_CPP})
target_link_libraries(dsp_tests PRIVATE GTest::gtest_main fvad rnnoise)

gtest_discover_tests(dsp_tests)
