package havudong.baocao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudinaryUploadResult {
    private String url;
    private String publicId;
}
