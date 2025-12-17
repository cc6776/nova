#!/usr/bin/env python3
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0

"""
Flex Tier 图片推理使用示例
- 使用方法：在调用 invoke_model 时添加 serviceTier="flex" 参数
"""

import boto3
import json
import base64
from pathlib import Path

# 创建 Bedrock Runtime 客户端
client = boto3.client("bedrock-runtime", region_name="us-west-2")

# 读取图片
image_path = Path("images/test1.png")
with open(image_path, "rb") as image_file:
    binary_data = image_file.read()
    base_64_encoded_data = base64.b64encode(binary_data)
    base64_string = base_64_encoded_data.decode("utf-8")

# 获取图片格式
image_format = image_path.suffix.lower().replace('.', '')
if image_format == 'jpg':
    image_format = 'jpeg'

# 准备图片推理请求
request_body = {
    "schemaVersion": "messages-v1",
    "messages": [
        {
            "role": "user",
            "content": [
                {
                    "image": {
                        "format": image_format,
                        "source": {"bytes": base64_string}
                    }
                },
                {
                    "text": "Describe this image in detail."
                }
            ]
        }
    ],
    "inferenceConfig": {
        "maxTokens": 512,
        "temperature": 0.7
    }
}

print("=" * 60)
print("使用 Flex Tier 进行图片推理")
print("=" * 60)
print(f"图片: {image_path.name}\n")

# 使用 Flex Tier - 只需添加 serviceTier="flex" 参数
response = client.invoke_model(
    modelId="us.amazon.nova-2-lite-v1:0",
    body=json.dumps(request_body),
    contentType="application/json",
    accept="application/json",
    serviceTier="flex"  # 关键参数：指定使用 flex tier
)

# 打印完整的返回报文
print("=" * 60)
print("完整返回报文")
print("=" * 60)

# 1. 打印响应元数据（包含所有 HTTP 响应头）
print("\n【ResponseMetadata】")
print(json.dumps(response["ResponseMetadata"], indent=2, ensure_ascii=False))

# 2. 解析并打印响应体
result = json.loads(response["body"].read())
print("\n【Response Body】")
print(json.dumps(result, indent=2, ensure_ascii=False))

# 3. 提取关键信息
response_text = result['output']['message']['content'][0]['text']
headers = response["ResponseMetadata"]["HTTPHeaders"]
actual_tier = headers.get("x-amzn-bedrock-service-tier")

print("\n" + "=" * 60)
print("关键信息提取")
print("=" * 60)
print(f"\n响应内容:")
print(f"{response_text}\n")

print("验证实际使用的 Service Tier:")
print(f"  请求的 Tier: flex")
print(f"  实际使用的 Tier: {actual_tier}")

if actual_tier == "flex":
    print("  ✅ 确认：成功使用 Flex Tier")
else:
    print(f"  ⚠️  注意：实际使用的是 {actual_tier} tier，而不是 flex tier")

print("\n" + "=" * 60)
print("Flex Tier 图片推理使用说明")
print("=" * 60)
print("""
📝 使用方法：
# 1. 读取并编码图片
with open(image_path, "rb") as f:
    base64_string = base64.b64encode(f.read()).decode("utf-8")

# 2. 构建包含图片的请求
request_body = {
    "schemaVersion": "messages-v1",
    "messages": [{
        "role": "user",
        "content": [
            {"image": {"format": "png", "source": {"bytes": base64_string}}},
            {"text": "Describe this image."}
        ]
    }],
    "inferenceConfig": {"maxTokens": 512}
}

# 3. 调用时添加 serviceTier="flex"
response = client.invoke_model(
    modelId="us.amazon.nova-2-lite-v1:0",
    body=json.dumps(request_body),
    serviceTier="flex"  # 添加此参数
)

🔍 如何验证实际使用的 Tier：
从响应头中读取 X-Amzn-Bedrock-Service-Tier：

headers = response["ResponseMetadata"]["HTTPHeaders"]
actual_tier = headers.get("x-amzn-bedrock-service-tier")
print(f"实际使用的 Tier: {actual_tier}")

如果返回 "flex"，说明成功使用了 Flex Tier
""")
