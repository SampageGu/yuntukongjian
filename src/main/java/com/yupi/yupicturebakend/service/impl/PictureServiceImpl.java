package com.yupi.yupicturebakend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebakend.model.entity.Picture;
import com.yupi.yupicturebakend.service.PictureService;
import com.yupi.yupicturebakend.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
 * @author 49879
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-01-22 10:41:16
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

}




