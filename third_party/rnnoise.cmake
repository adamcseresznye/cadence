# RNNoise v0.1.1 (BSD-3-Clause, see third_party/NOTICE.md) as the static library target rnnoise.
set(RNNOISE_DIR "${CMAKE_CURRENT_LIST_DIR}/rnnoise")
file(GLOB RNNOISE_SOURCES CONFIGURE_DEPENDS "${RNNOISE_DIR}/src/*.c")
add_library(rnnoise STATIC ${RNNOISE_SOURCES})
set_target_properties(rnnoise PROPERTIES POSITION_INDEPENDENT_CODE ON)
# _USE_MATH_DEFINES: M_PI on Windows compilers (desktop tests).
target_compile_definitions(rnnoise PRIVATE RNNOISE_BUILD _USE_MATH_DEFINES)
target_include_directories(rnnoise
    PUBLIC "${RNNOISE_DIR}/include"
    PRIVATE "${RNNOISE_DIR}/src")
