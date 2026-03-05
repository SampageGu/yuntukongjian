package com.yunpicture.yunpicturebackend.model.dto.picture;

import com.yunpicture.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {


    private Long pictureId;


    private CreateOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}
