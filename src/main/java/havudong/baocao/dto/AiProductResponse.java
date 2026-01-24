package havudong.baocao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiProductResponse {
    private Long id;
    private String name;
    private Long price;
    private String mainImage;
    private Long categoryId;
    private String categoryName;
}
