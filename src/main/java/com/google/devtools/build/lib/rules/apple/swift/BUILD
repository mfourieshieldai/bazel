load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//src:__subpackages__"])

filegroup(
    name = "srcs",
    srcs = glob(["**"]),
)

# Description:
#   Support for building swift for Apple platform
java_library(
    name = "swift",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/devtools/build/lib:build-base",
        "//src/main/java/com/google/devtools/build/lib/concurrent",
        "//src/main/java/com/google/devtools/build/lib/skylarkbuildapi/apple",
        "//src/main/java/com/google/devtools/common/options",
        "//third_party:guava",
    ],
)
