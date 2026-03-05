package com.yunpicture.yunpicturebackend.service;

import com.yunpicture.yunpicturebackend.model.dto.space.analyze.*;
import com.yunpicture.yunpicturebackend.model.vo.space.analyze.*;
import com.yunpicture.yunpicturebackend.model.dto.space.analyze.*;
import com.yunpicture.yunpicturebackend.model.vo.space.analyze.*;

import com.yunpicture.yunpicturebackend.model.entity.Space;
import com.yunpicture.yunpicturebackend.model.entity.User;


import java.util.List;

public interface SpaceAnalyzeService {


    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(User loginUser, SpaceAnalyzeRequest spaceAnalyzeRequest);


    /**
     * 空间图片分类响应
     * @param loginUser
     * @param spaceCategoryAnalyzeRequest
     * @return
     */
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(User loginUser, SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest);

    /**
     * 空间图片标签响应
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 空间图片大小分析响应
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取用户上传空间行为分析
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 空间使用排行分析
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
