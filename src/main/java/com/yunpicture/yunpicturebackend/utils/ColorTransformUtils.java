package com.yunpicture.yunpicturebackend.utils;

public class ColorTransformUtils {

    private ColorTransformUtils() {
    }

    /**
     * 将颜色转为标准的6位 例如 0xRRGGBB
     *
     * @param color 输入的颜色，可以是 #RRGGBB 格式的字符串
     * @return 返回标准颜色的字符串格式 0xRRGGBB
     */
    public static String getStandardColor(String color) {
        // 去掉颜色字符串前面的 #
        if (color.startsWith("#")) {
            color = color.substring(1);
        }

        // 如果输入是RGB格式（例如：rgb(0, 224, 255)）
        if (color.startsWith("rgb")) {
            // 提取 RGB 值
            String[] rgbValues = color.substring(color.indexOf('(') + 1, color.indexOf(')')).split(",");
            int red = Integer.parseInt(rgbValues[0].trim());
            int green = Integer.parseInt(rgbValues[1].trim());
            int blue = Integer.parseInt(rgbValues[2].trim());
            // 转换成 0xRRGGBB 格式
            return String.format("0x%02X%02X%02X", red, green, blue);
        }

        // 如果颜色是一个 6 位的十六进制颜色字符串（例如：00e0ff）
        if (color.length() == 6) {
            return "0x" + color.toUpperCase();
        }

        throw new IllegalArgumentException("Invalid color format");
    }

    public static void main(String[] args) {
        // 测试
        String hexColor = "#00e0ff";
        String rgbColor = "rgb(0, 224, 255)";

        System.out.println(getStandardColor(hexColor));  // 输出：0x00E0FF
        System.out.println(getStandardColor(rgbColor));  // 输出：0x00E0FF
    }
}
