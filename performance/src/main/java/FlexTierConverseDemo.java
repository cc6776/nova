// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Flex Tier 图片推理示例 - 使用 Converse API
 * 使用方法：在调用 converse 时添加 performanceConfig 参数
 */
public class FlexTierConverseDemo {

    public static void main(String[] args) {
        BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        try {
            System.out.println("============================================================");
            System.out.println("使用 Flex Tier + Converse API 进行图片推理");
            System.out.println("============================================================");

            // 读取图片
            String imagePath = "../images/test1.png";
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            System.out.println("图片: " + imagePath + "\n");

            // 构建图片内容
            ImageBlock imageBlock = ImageBlock.builder()
                    .format(ImageFormat.PNG)
                    .source(ImageSource.builder()
                            .bytes(SdkBytes.fromByteArray(imageBytes))
                            .build())
                    .build();

            // 构建消息
            Message userMessage = Message.builder()
                    .role(ConversationRole.USER)
                    .content(
                            ContentBlock.fromImage(imageBlock),
                            ContentBlock.fromText("Describe this image in detail.")
                    )
                    .build();

            // 构建 Converse 请求，使用 serviceTier 指定 Flex Tier
            ConverseResponse response = client.converse(ConverseRequest.builder()
                    .modelId("global.amazon.nova-2-lite-v1:0")
                    .messages(userMessage)
                    .inferenceConfig(InferenceConfiguration.builder()
                            .maxTokens(512)
                            .temperature(0.7F)
                            .build())
                    .serviceTier(ServiceTier.builder()
                            .type(ServiceTierType.FLEX)  // 关键参数：指定使用 Flex Tier
                            .build())
                    .build());

            // 打印结果
            System.out.println("============================================================");
            System.out.println("响应结果");
            System.out.println("============================================================");

            String responseText = response.output().message().content().get(0).text();
            System.out.println("\n响应内容:");
            System.out.println(responseText);

            System.out.println("\n【Token 使用情况】");
            System.out.println("  输入 Token: " + response.usage().inputTokens());
            System.out.println("  输出 Token: " + response.usage().outputTokens());
            System.out.println("  总计 Token: " + response.usage().totalTokens());

            System.out.println("\n【停止原因】: " + response.stopReason());

            // 检查 Performance Config
            if (response.performanceConfig() != null) {
                System.out.println("\n【Performance Config】");
                System.out.println("  Latency: " + response.performanceConfig().latency());
            }

            // 使用说明
            System.out.println("\n============================================================");
            System.out.println("Converse API + Flex Tier 使用说明");
            System.out.println("============================================================");
            System.out.println("""

📝 使用方法：
// 1. 读取图片
byte[] imageBytes = Files.readAllBytes(Paths.get("image.png"));

// 2. 构建图片和消息
ImageBlock imageBlock = ImageBlock.builder()
    .format(ImageFormat.PNG)
    .source(ImageSource.builder()
        .bytes(SdkBytes.fromByteArray(imageBytes))
        .build())
    .build();

Message userMessage = Message.builder()
    .role(ConversationRole.USER)
    .content(
        ContentBlock.fromImage(imageBlock),
        ContentBlock.fromText("Describe this image.")
    )
    .build();

// 3. 调用 Converse API，添加 serviceTier
ConverseResponse response = client.converse(ConverseRequest.builder()
    .modelId("us.amazon.nova-2-lite-v1:0")
    .messages(userMessage)
    .serviceTier(ServiceTier.builder()
        .type(ServiceTierType.FLEX)  // Flex Tier
        .build())
    .build());

                    """);

        } catch (IOException e) {
            System.err.println("读取图片文件失败: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("调用 Bedrock 失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.close();
        }
    }
}
