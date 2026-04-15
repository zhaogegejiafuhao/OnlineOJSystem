# Base Image
FROM registry.cn-hangzhou.aliyuncs.com/jinmingyi/judgerserver:latest

# Set Working Directory
WORKDIR /app

# Install Python Dependencies (if any)
# RUN pip install flask requests

# Copy Source Code
COPY . /app

# Expose Port
EXPOSE 5001

# Start Command
CMD ["python3", "app.py"]
