package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findBySlug(String slug);
    
    List<Category> findByParentIsNullAndIsActiveTrueOrderByDisplayOrder();
    
    List<Category> findByParentIdAndIsActiveTrueOrderByDisplayOrder(Long parentId);
    
    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.displayOrder")
    List<Category> findAllActiveOrdered();
    
    @Query(value = "WITH RECURSIVE category_tree AS (" +
            "SELECT id FROM categories " +
            "WHERE id = :categoryId AND deleted_at IS NULL " +
            "UNION ALL " +
            "SELECT c.id FROM categories c " +
            "INNER JOIN category_tree ct ON c.parent_id = ct.id " +
            "WHERE c.deleted_at IS NULL" +
            ") SELECT * FROM categories WHERE id IN (SELECT id FROM category_tree)",
            nativeQuery = true)
    List<Category> findCategoryWithChildren(@Param("categoryId") Long categoryId);

    @Query("""
        SELECT c.id, COUNT(p.id)
        FROM Category c
        LEFT JOIN Product p ON p.category.id = c.id AND p.isActive = true
        WHERE c.isActive = true
        GROUP BY c.id
        """)
    List<Object[]> countProductsByCategory();
    
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * True when the category has any (non-deleted) child. Used by the admin
     * delete guard — {@code @Where} on {@link Category} excludes soft-deleted
     * children automatically.
     */
    boolean existsByParentId(Long parentId);
}
