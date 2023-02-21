import os
import shutil
import subprocess
import tarfile

import requests

from build_dependencies.values import LIB_FOLDER_PATH, ARCH_NAMES, ANDROID_PLATFORM, TOOLCHAIN, BUILD_TYPE


# ------------------------------------------------------------
def run_cmd(cmd):
    command = " ".join(cmd)
    log("Run: " + command)
    result = subprocess.call(command, shell=True)
    if result != 0:
        error(command + "  result=" + str(result))


def error(msg):
    log("Error !!! " + msg)
    os.sys.exit(0)


def log(msg):
    print("* " + msg)


def delete_if_exists(path):
    if os.path.exists(path):
        shutil.rmtree(path)


def delete_file_if_exists(path):
    if os.path.exists(path):
        os.remove(path)


def download_file(url, filename=None, show_done_message=False):
    if not filename:
        filename = url.split('/')[-1] + ".tar.xz"

    print(f"* Downloading {filename}")
    with requests.get(url, stream=True) as request:
        request.raise_for_status()

        with open(filename, 'wb') as file:
            for chunk in request.iter_content(chunk_size=1024):
                file.write(chunk)

    if show_done_message:
        print(f"* Finished downloading {filename}")
    return filename


def extract_tar_file(filename, path=".", show_done_message=False):
    log(f"Extracting {filename}")
    with tarfile.open(filename) as file:
        file.extractall(path)

    if show_done_message:
        log(f"Finished extracting {filename}")


def move_file(filename, target):
    os.makedirs(target, exist_ok=True)
    shutil.move(filename, f"{target}/{filename}")


def get_lib_path(arch, lib_filename, levels_up=2):
    return f"{'../' * levels_up}{LIB_FOLDER_PATH}/{ARCH_NAMES[arch]}/{lib_filename}"


def build_libpng(arch, LIBPNG_DIR):
    INSTALL_PREFIX = os.path.abspath(os.path.join(LIBPNG_DIR, "lib", ARCH_NAMES[arch]))
    # cmake generator
    cmd = ["cmake"]
    cmd += ["-DCMAKE_TOOLCHAIN_FILE=" + TOOLCHAIN]
    cmd += ["-DANDROID_ABI=" + ARCH_NAMES[arch]]
    cmd += ["-DANDROID_PLATFORM=" + ANDROID_PLATFORM]
    cmd += ["-DCMAKE_INSTALL_PREFIX=" + INSTALL_PREFIX]
    cmd += ["-DPNG_TESTS=OFF -DHAVE_LD_VERSION_SCRIPT=OFF"]

    if BUILD_TYPE == "Debug":
        cmd += ["-DPNG_DEBUG=ON"]
    cmd += [".."]
    run_cmd(cmd)

    # cmake build
    cmd = ["cmake --build ."]
    cmd += ["--config " + BUILD_TYPE]
    run_cmd(cmd)

    # cmake install
    cmd = ["cmake --install ."]
    cmd += ["--config " + BUILD_TYPE]
    run_cmd(cmd)


def build_freetype(arch, FREETYPE_DIR, LIBPNG_DEPENDENCY_PATH):
    # libpng dependency paths
    LIBPNG_INCLUDE_PATH = os.path.abspath(os.path.join(os.getcwd(), LIBPNG_DEPENDENCY_PATH, "lib", ARCH_NAMES[arch], "include"))
    LIBPNG_LIBRARY_PATH = os.path.abspath(os.path.join(os.getcwd(), LIBPNG_DEPENDENCY_PATH, "lib", ARCH_NAMES[arch], "lib", "libpng16.a"))
    print(LIBPNG_INCLUDE_PATH)
    print(LIBPNG_LIBRARY_PATH)

    INSTALL_PREFIX = os.path.abspath(os.path.join(FREETYPE_DIR, "lib", ARCH_NAMES[arch]))
    # cmake generator
    cmd = ["cmake"]
    cmd += ["-DCMAKE_POSITION_INDEPENDENT_CODE=ON"]
    cmd += ["-DCMAKE_TOOLCHAIN_FILE=" + TOOLCHAIN]
    cmd += ["-DBUILD_SHARED_LIBS=true"]
    cmd += ["-DANDROID_ABI=" + ARCH_NAMES[arch]]
    cmd += ["-DANDROID_PLATFORM=" + ANDROID_PLATFORM]
    cmd += ["-DCMAKE_INSTALL_PREFIX=" + INSTALL_PREFIX]
    cmd += ["-DFT_WITH_ZLIB=ON -D FT_WITH_PNG=ON"]
    cmd += ["-DPNG_PNG_INCLUDE_DIR=" + LIBPNG_INCLUDE_PATH]
    cmd += ["-DPNG_LIBRARY=" + LIBPNG_LIBRARY_PATH]
    cmd += [".."]
    run_cmd(cmd)

    # cmake build
    cmd = ["cmake --build ."]
    cmd += ["--config " + BUILD_TYPE]
    run_cmd(cmd)

    # cmake install
    cmd = ["cmake --install ."]
    cmd += ["--config " + BUILD_TYPE]
    run_cmd(cmd)

