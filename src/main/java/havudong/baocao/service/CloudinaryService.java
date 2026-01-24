package havudong.baocao.service;

import havudong.baocao.dto.CloudinaryUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    CloudinaryUploadResult upload(MultipartFile file);
}
