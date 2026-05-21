# 密算平台 Python节点

from setuptools import setup, find_packages

setup(
    name="msp-node",
    version="1.0.0",
    packages=find_packages(where="src"),
    package_dir={"": "src"},
    python_requires=">=3.10",
    install_requires=[
        "secretflow>=1.11.0",
        "grpcio>=1.60.0",
        "protobuf>=3.25.0",
        "pyyaml>=6.0",
    ],
)