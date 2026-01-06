package havudong.baocao.service;

import havudong.baocao.entity.Category;
import havudong.baocao.exception.DuplicateResourceException;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    @Transactional
    public Category createCategory(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new DuplicateResourceException("Category", "name", category.getName());
        }
        return categoryRepository.save(category);
    }
    
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
    
    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    @Transactional
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        
        category.setName(categoryDetails.getName());
        category.setIcon(categoryDetails.getIcon());
        category.setDescription(categoryDetails.getDescription());
        
        return categoryRepository.save(category);
    }
    
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
