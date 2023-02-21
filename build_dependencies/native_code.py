import os
from pathlib import Path

from build_dependencies.common import get_lib_path, run_cmd
from build_dependencies.values import LIB_FOLDER_PATH


def build_native_code():
    JNI_PATH = Path(f"../{LIB_FOLDER_PATH}").resolve().parents[0]
    os.chdir(JNI_PATH)
    run_cmd(["/opt/android-ndk/ndk-build"])


if __name__ == "__main__":
    build_native_code()