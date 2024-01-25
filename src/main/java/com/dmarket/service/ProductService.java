package com.dmarket.service;

import com.dmarket.dto.response.*;
import com.dmarket.repository.product.CategoryRepository;
import com.dmarket.repository.product.ProductRepository;
import com.dmarket.domain.product.Category;
import com.dmarket.domain.product.Product;
import com.dmarket.dto.common.ProductDto;
import com.dmarket.dto.common.ProductOptionDto;
import com.dmarket.dto.common.ProductReviewDto;
import com.dmarket.repository.product.*;
import com.dmarket.repository.user.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    //조회가 아닌 메서드들은 꼭 @Transactional 넣어주세요 (CUD, 입력/수정/삭제)
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;
    private final ProductImgsRepository productImgsRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductReviewRepository productReviewRepository;

    private static final int PRODUCT_PAGE_POST_COUNT = 16;
    private static final int REVIEW_PAGE_POST_COUNT = 5;

    // 카테고리 전체 목록 depth별로 조회
    public List<CategoryListResDto> getCategories(Integer categoryDepthLevel){
        return categoryRepository.findByCategoryDepth(categoryDepthLevel);
    }

    // 카테고리별 상품 목록 필터링 조회
    public Page<ProductListResDto> getCategoryProducts(Pageable pageable, int pageNo, Long cateId,
                                                       String sorter, Integer minPrice, Integer maxPrice, Float star) {
        pageable = PageRequest.of(pageNo, PRODUCT_PAGE_POST_COUNT, Sort.by(Sort.Direction.DESC, sorter));
        Page<ProductListResDto> productList = productRepository.findByCateId(pageable, cateId, minPrice, maxPrice, star);

        return productList;
    }

    // 상품 목록 조건 검색
    public Page<ProductListResDto> getSearchProducts(Pageable pageable, int pageNo, String query,
                                                     String sorter, Integer minPrice, Integer maxPrice, Float star) {
        pageable = PageRequest.of(pageNo, PRODUCT_PAGE_POST_COUNT, Sort.by(Sort.Direction.DESC, sorter));
        Page<ProductListResDto> productList = productRepository.findByQuery(pageable, query, minPrice, maxPrice, star);

        return productList;
    }

    // 최신 상품 조회
    public List<NewProductResDto> findNewProducts() {
        return productRepository.findNewProducts();
    }

    // 최신 상품 조회 - 매핑
    public List<Object> mapToResponseFormat(List<NewProductResDto> latestProducts) {
        return latestProducts.stream()
                .limit(8).map(product -> new Object() {
                    public final Long productId = product.getProductId();
                    public final String productBrand = product.getProductBrand();
                    public final String productName = product.getProductName();
                    public final String productImg = product.getProductImg();
                    public final String productSalePrice = String.valueOf(product.getProductSalePrice());
                })
                .collect(Collectors.toList());
    }

    // 상품 상세 정보 조회
    public ProductInfoResDto getProductInfo(Long productId, Long userId){
        // 싱품 정보 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 상품입니다."));
        // 상품의 카테고리 depth 1, depth2 조회 후 합치기
        Category category = categoryRepository.findByCategoryId(product.getCategoryId());
        String productCategory = category.getParent().getCategoryName() + " / " + category.getCategoryName();
        // 상품의 리뷰 개수 조회
        Long reviewCnt = productReviewRepository.countByProductId(productId);
        // 사용자가 위시리스트에 등록한 상품인지 확인
        Boolean isWish = wishlistRepository.existsByUserIdAndProductId(userId, productId);
        // 상품 옵션 목록, 옵션별 재고 조회
        List<ProductOptionDto> opts = productOptionRepository.findOptionsByProductId(productId);
        // 상품 이미지 목록 조회
        List<String> imgs = productImgsRepository.findAllByProductId(productId);
        // DTO 생성 및 반환
        return new ProductInfoResDto(product, productCategory, reviewCnt, isWish, opts, imgs);
    }

    // 상품별 사용자 리뷰 조회
    public ProductReviewListResDto getReviewList(Long productId, Integer pageNo){
        Pageable pageable = PageRequest.of(pageNo, REVIEW_PAGE_POST_COUNT, Sort.by(Sort.Direction.DESC, "reviewCreatedDate"));
        // 상품 번호, 상품 별점, 리뷰 개수 조회, 존재하지 않는 상품 번호의 경우 예외 발생
        ProductDto product = productRepository.findProductByProductId(productId)
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 상품"));
        // 상품의 리뷰 목록 불러오기
        Page<ProductReviewDto> reviewList = productReviewRepository.findReviewByProductId(pageable, productId);
        return new ProductReviewListResDto(product, reviewList.getTotalPages(), reviewList.getContent());
    }

    // 추천 상품 조회
    public List<RecommendProductResDto> recommendProduct(Long productId) {
        // PageRequest의 pageSize 4로 지정 최신 4개만 조회
        return productRepository.findProduct(productId, PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "productCreatedDate")));
    }

    @Transactional
    public void deleteReviewByReviewId(Long productId, Long reviewId) {
        productReviewRepository.deleteByReviewId(reviewId);
    }
}
