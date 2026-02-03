package com.yupi.yupicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetImagePageUrlApi {

    public static String getSogouDataUrl(String imageUrl) {
        try {
            // 1. 模拟上传换取 query
            String uploadUrl = "https://pic.sogou.com/ris_upload";
            HttpResponse uploadResponse = HttpRequest.post(uploadUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .form("pic_url", imageUrl)
                    .execute();

            String uploadBody = uploadResponse.body();

            // 2. 使用正则安全提取 query 参数
            // 搜狗返回的通常是 window.location.replace("...query=xxx...")
            String query = ReUtil.get("query=([^\"&]+)", uploadBody, 1);

            if (query == null || query.isEmpty()) {
                log.error("提取 query 失败，搜狗返回内容: {}", uploadBody);
                return null;
            }

            // 3. 构造最终的数据接口 URL (即你截图中想要的那种)
            // 注意：risapi 接口通常对编码要求极高
            String finalApiUrl = String.format(
                    "https://ris.sogou.com/risapi/pc/sim?query=%s&start=0&plevel=-1",
                    URLUtil.encode(query)
            );

            return finalApiUrl;
        } catch (Exception e) {
            log.error("构造搜狗数据链接失败", e);
            return null;
        }
    }

    public static void main(String[] args) {
        String testUrl = "https://pic.nximg.cn/file/20210516/10621685_175815636120_2.jpg";
        String resultUrl = getSogouDataUrl(testUrl);
        if (resultUrl != null) {
            System.out.println("成功！生成的 API URL 为：\n" + resultUrl);
        } else {
            System.out.println("获取失败，请检查控制台日志");
        }
    }
}