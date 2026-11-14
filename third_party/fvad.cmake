# libfvad (BSD-3-Clause, see third_party/NOTICE.md) as the static library target fvad.
set(FVAD_DIR "${CMAKE_CURRENT_LIST_DIR}/libfvad")
file(GLOB_RECURSE FVAD_SOURCES CONFIGURE_DEPENDS "${FVAD_DIR}/src/*.c")
add_library(fvad STATIC ${FVAD_SOURCES})
set_target_properties(fvad PROPERTIES POSITION_INDEPENDENT_CODE ON)
target_include_directories(fvad
    PUBLIC "${FVAD_DIR}/include"
    PRIVATE "${FVAD_DIR}/src")
