import glob
import os
import shutil

from build_dependencies.common import download_file, extract_tar_file, log, delete_if_exists, \
    delete_file_if_exists, get_lib_path, build_libpng
from build_dependencies.values import Arch, FILE_NAMES, LIB_EXTENSION, Lib, ARCH_NAMES, LIBPNG_URL, LIBPNG_BUILD, BUILD

BUILT_LIB_NAME = "libpng16.so"


def build_libpng_libs():
    log(f"Downloading libpng source code.")
    os.makedirs(LIBPNG_BUILD, exist_ok=True)
    os.chdir(LIBPNG_BUILD)
    LIBPNG_ROOT_PATH = os.getcwd()

    temp_file_name = "libpng.tar.gz"
    download_file(LIBPNG_URL, temp_file_name)
    extract_tar_file(temp_file_name)
    delete_file_if_exists(temp_file_name)

    # get file name (different for each version)
    files = glob.glob("./libpng-*")
    os.chdir(files[0])  # there should be only one file

    for arch in [Arch.x86_64, Arch.arm64, Arch.x86, Arch.armeabi]:
        log(f"Building libpng for {ARCH_NAMES[arch]}.")

        # create build folder
        delete_if_exists(BUILD)
        os.makedirs(BUILD, exist_ok=True)
        os.chdir(BUILD)

        build_libpng(arch, LIBPNG_ROOT_PATH)
        log(f"Finished building {ARCH_NAMES[arch]}")

        lib_filename = FILE_NAMES[Lib.libpng] + LIB_EXTENSION
        lib_path = get_lib_path(arch, lib_filename, levels_up=3)

        os.chdir("../")
        delete_file_if_exists(lib_path)
        log("Deleted any old lib.")

        downloaded_lib_path = f"{BUILD}/{BUILT_LIB_NAME}"
        shutil.move(downloaded_lib_path, lib_path)
        log("Moved the new lib to the target dir.")

        log(f"Finished libpng for {ARCH_NAMES[arch]}.")
        log(f"------------------------------------")

    os.chdir("../../")


if __name__ == "__main__":
    build_libpng_libs()
