package com.yupi.yupicturebackend.model.dto.picture;

import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {


    private Long pictureId;


    private CreateOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}
