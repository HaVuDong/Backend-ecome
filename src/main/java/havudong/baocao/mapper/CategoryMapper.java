package havudong.baocao.mapper;

import havudong.baocao.dto.CategoryRequest;
import havudong.baocao.dto.CategoryResponse;
import havudong.baocao.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    
    public Category toEntity(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setDescription(request.getDescription());
        return category;
    }
    
    public void updateEntity(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setDescription(request.getDescription());
    }
    
    public CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
