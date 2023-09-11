cmake_minimum_required(VERSION 3.0.2)
project("TinyALSA" C)
set (HDRS
    "include/tinyalsa/attributes.h"
    "include/tinyalsa/version.h"
    "include/tinyalsa/asoundlib.h"
    "include/tinyalsa/pcm.h"
    "include/tinyalsa/mixer.h")

set (SRCS
    "src/pcm.c"
    "src/mixer.c")

add_library("tinyalsa" ${HDRS} ${SRCS})
target_compile_options("tinyalsa" PRIVATE -Wall -Wextra -Werror -Wfatal-errors -g)
target_include_directories("tinyalsa" PRIVATE "include")
