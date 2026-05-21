FROM python:3.10-slim

WORKDIR /app

# 安装 SecretFlow/SPU 依赖的系统库
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# 使用清华镜像加速
RUN pip install --no-cache-dir pip --timeout 300 && \
    pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple && \
    pip config set global.trusted-host pypi.tuna.tsinghua.edu.cn

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt --timeout 300

COPY src /app/src

ENV PYTHONPATH=/app/src

EXPOSE 50051

ENTRYPOINT ["python", "-m", "msp_node.transport.grpc_server"]