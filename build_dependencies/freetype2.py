import glob
import os
import shutil

from build_dependencies.common import download_file, extract_tar_file, log, delete_if_exists, \
    delete_file_if_exists, get_lib_path, build_freetype
from build_dependencies.values import Arch, FILE_NAMES, LIB_EXTENSION, Lib, FREETYPE_BUILD, \
    ARCH_NAMES, LIBPNG_BUILD, FREETYPE_URL

BUILT_LIB_NAME = "libfreetype.so"
DOWNLOADED_LIB_PATH = f"{FREETYPE_BUILD}/{BUILT_LIB_NAME}"

def build_freetype_libs():
    LIBPNG_DEPENDENCY_PATH = os.path.join(os.getcwd(), LIBPNG_BUILD)

    log(f"Downloading FreeType source code.")
    os.makedirs(FREETYPE_BUILD, exist_ok=True)
    os.chdir(FREETYPE_BUILD)
    FREETYPE_ROOT_PATH = os.getcwd()

    temp_file_name = "freetype.tar.gz"
    download_file(FREETYPE_URL, temp_file_name)

    extract_tar_file(temp_file_name)
    delete_file_if_exists(temp_file_name)

    # get file name (different for each version)
    files = glob.glob("./freetype-*")
    os.chdir(files[0])                    # there should be only one file

    for arch in [Arch.x86_64, Arch.arm64, Arch.x86, Arch.armeabi]:
        log(f"Building FreeType for {ARCH_NAMES[arch]}.")

        # create build folder
        os.makedirs(FREETYPE_BUILD, exist_ok=True)
        os.chdir(FREETYPE_BUILD)

        build_freetype(arch, FREETYPE_ROOT_PATH, LIBPNG_DEPENDENCY_PATH)
        log(f"Finished building {ARCH_NAMES[arch]}")

        lib_filename = FILE_NAMES[Lib.freetype2] + LIB_EXTENSION
        lib_path = get_lib_path(arch, lib_filename, levels_up=3)     # two level inside the root dir

        os.chdir("../")
        delete_file_if_exists(lib_path)
        log("Deleted any old lib.")

        print(os.getcwd())
        shutil.move(DOWNLOADED_LIB_PATH, lib_path)
        log("Moved the new lib to the target dir.")

        delete_if_exists(FREETYPE_BUILD)
        log("Deleted temp build dir.")

        log(f"Finished libpng for {ARCH_NAMES[arch]}.")
        log(f"------------------------------------")


    os.chdir("../")
    os.chdir("../")


if __name__ == "__main__":
    build_freetype_libs()