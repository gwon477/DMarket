package com.dmarket.repository.user;

import com.dmarket.domain.user.Wishlist;
import com.dmarket.dto.common.WishlistItemDto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    @Query(value = "select new com.dmarket.dto.common.WishlistItemDto(" +
            "   p.productId, w.wishlistId, p.productName, p.productBrand, pi.imgAddress, p.productSalePrice)" +
            " from Wishlist w" +
            " join Product p on w.productId = p.productId " +
            " join ProductImgs pi on p.productId = pi.productId " +
            " where w.userId = :userId")
    List<WishlistItemDto> findWishlistItemsByUserId(@Param("userId") Long userId);

    void deleteById(@Param("wishlistId") Long wishlistId);
}
