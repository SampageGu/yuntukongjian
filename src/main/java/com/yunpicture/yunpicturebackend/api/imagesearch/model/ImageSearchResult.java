package com.yunpicture.yunpicturebackend.api.imagesearch.model;

import lombok.Data;

@Data
public class ImageSearchResult {


    private String thumbUrl;


    /**
     * 来源地址
     */
    private String fromUrl;
}
