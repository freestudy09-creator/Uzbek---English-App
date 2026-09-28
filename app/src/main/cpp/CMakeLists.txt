cmake_minimum_required(VERSION 3.22.1)
project(tilmate_translation)

include(FetchContent)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(BUILD_CLI OFF CACHE BOOL "" FORCE)
set(WITH_MKL OFF CACHE BOOL "" FORCE)
set(WITH_DNNL OFF CACHE BOOL "" FORCE)
set(WITH_OPENBLAS OFF CACHE BOOL "" FORCE)
set(WITH_ACCELERATE OFF CACHE BOOL "" FORCE)
set(WITH_CUDA OFF CACHE BOOL "" FORCE)
set(WITH_CUDNN OFF CACHE BOOL "" FORCE)
set(WITH_RUY ON CACHE BOOL "" FORCE)
set(ENABLE_CPU_DISPATCH OFF CACHE BOOL "" FORCE)
set(OPENMP_RUNTIME NONE CACHE STRING "" FORCE)

set(SPM_ENABLE_SHARED OFF CACHE BOOL "" FORCE)
set(SPM_ENABLE_TCMALLOC OFF CACHE BOOL "" FORCE)
set(SPM_BUILD_TEST OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
  sentencepiece
  GIT_REPOSITORY https://github.com/google/sentencepiece.git
  GIT_TAG v0.2.1
  GIT_SHALLOW TRUE
)

FetchContent_Declare(
  ctranslate2
  GIT_REPOSITORY https://github.com/OpenNMT/CTranslate2.git
  GIT_TAG v4.8.2
  GIT_SHALLOW TRUE
  GIT_SUBMODULES_RECURSE TRUE
)

FetchContent_MakeAvailable(sentencepiece)

FetchContent_GetProperties(ctranslate2)
if(NOT ctranslate2_POPULATED)
  FetchContent_Populate(ctranslate2)

  # Android/Bionic does not expose pthread_setaffinity_np. CTranslate2 only
  # uses it for optional worker pinning, so disable that Linux-only branch.
  set(CT2_THREAD_POOL "${ctranslate2_SOURCE_DIR}/src/thread_pool.cc")
  file(READ "${CT2_THREAD_POOL}" CT2_THREAD_POOL_TEXT)
  string(REPLACE
    "#if !defined(__linux__) || defined(_OPENMP)"
    "#if !defined(__linux__) || defined(_OPENMP) || defined(__ANDROID__)"
    CT2_THREAD_POOL_TEXT
    "${CT2_THREAD_POOL_TEXT}")
  file(WRITE "${CT2_THREAD_POOL}" "${CT2_THREAD_POOL_TEXT}")

  add_subdirectory("${ctranslate2_SOURCE_DIR}" "${ctranslate2_BINARY_DIR}")
endif()

add_library(tilmate_translation SHARED native_translation.cpp)

target_compile_features(tilmate_translation PRIVATE cxx_std_17)

target_link_libraries(
  tilmate_translation
  ctranslate2
  sentencepiece-static
  log
  android
)
