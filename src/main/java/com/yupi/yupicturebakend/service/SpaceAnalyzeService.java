package com.yupi.yupicturebakend.service;

import com.yupi.yupicturebakend.model.dto.space.analyze.*;
import com.yupi.yupicturebakend.model.entity.Space;
import com.yupi.yupicturebakend.model.entity.User;
import com.yupi.yupicturebakend.model.vo.space.analyze.*;

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
