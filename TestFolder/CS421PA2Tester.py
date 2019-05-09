import os

os.system("python3 test_retrieve.py | python3 test_write.py | python3 test_modify.py")
os.system("python3 test_revert.py")