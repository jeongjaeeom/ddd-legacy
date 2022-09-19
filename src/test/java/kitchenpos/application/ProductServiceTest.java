package kitchenpos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.MenuRepository;
import kitchenpos.domain.Product;
import kitchenpos.domain.ProductRepository;
import kitchenpos.domain.ProfanityClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  private ProductService productService;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private MenuRepository menuRepository;

  @Mock
  private ProfanityClient profanityClient;

  @BeforeEach
  void setUp() {
    this.productService = new ProductService(productRepository, menuRepository, profanityClient);
  }

  @Nested
  @DisplayName("상품을 등록할 때")
  class WhenCreate {
    @DisplayName("유효한 상품명과 가격을 입력하면 등록된 상품을 반환한다.")
    @Test
    void givenProduct_whenCreate_thenReturnProduct() {
      // given
      Product product = creationRequestProduct("후라이드치킨", BigDecimal.valueOf(11000));
      given(profanityClient.containsProfanity(anyString())).willReturn(false);
      given(productRepository.save(any(Product.class))).willReturn(product);

      // when
      Product savedProduct = productService.create(product);

      // then
      assertThat(savedProduct.getId()).isNotNull();
      assertThat(savedProduct.getName()).isEqualTo(product.getName());
      assertThat(savedProduct.getPrice()).isEqualTo(product.getPrice());
    }


    @DisplayName("상품 가격은 0원 보다 작을 수 없다")
    @MethodSource("kitchenpos.application.ProductServiceTest#provideBigDecimalsForNullAndNegative")
    @ParameterizedTest(name = "{displayName}: [{index}] {argumentsWithNames}")
    void givenNotValidPrice_whenCreate_thenIllegalArgumentException(BigDecimal price) {
      // given
      Product product = creationRequestProduct("후라이드치킨", price);

      // when & then
      assertThatIllegalArgumentException().isThrownBy(() -> productService.create(product));
    }

    @DisplayName("상품 이름은 비어있을 수 없다.")
    @Test
    void givenNotValidName_whenCreate_thenIllegalArgumentException() {
      // given
      Product product = new Product();
      product.setId(UUID.randomUUID());
      product.setPrice(BigDecimal.valueOf(11000));

      // when & then
      assertThatIllegalArgumentException().isThrownBy(() -> productService.create(product));
    }

    @DisplayName("상품 이름에는 비속어를 포함할 수 없다.")
    @Test
    void givenProfanityName_whenCreate_thenIllegalArgumentException() {
      // given
      Product product = creationRequestProduct("Shit", BigDecimal.valueOf(11000));
      given(profanityClient.containsProfanity(anyString())).willReturn(true);

      // when & then
      assertThatIllegalArgumentException().isThrownBy(() -> productService.create(product));
    }

    @DisplayName("유효한 상품 ID와 변경할 상품가격 정보를 입력하면 변경된 상품정보를 반환한다.")
    @Test
    void givenChangePrice_whenChangePrice_thenReturnChangedProduct() {
      // given
      Product product = creationRequestProduct("후라이드치킨", BigDecimal.valueOf(11000));

      given(productRepository.findById(product.getId())).willReturn(Optional.of(product));

      MenuProduct menuProduct = new MenuProduct();
      menuProduct.setProduct(product);
      menuProduct.setQuantity(1);

      Menu menu = new Menu();
      menu.setId(UUID.randomUUID());
      menu.setName("후라이드치킨");
      menu.setPrice(BigDecimal.valueOf(11000));
      menu.setMenuProducts(List.of(menuProduct));

      given(menuRepository.findAllByProductId(product.getId())).willReturn(List.of(menu));

      Product changePriceProduct = new Product();
      changePriceProduct.setPrice(BigDecimal.valueOf(12000));

      // when
      Product changedProduct = productService.changePrice(product.getId(), changePriceProduct);

      // then
      assertThat(changedProduct.getId()).isNotNull();
      assertThat(changedProduct.getName()).isEqualTo(product.getName());
      assertThat(changedProduct.getPrice()).isEqualTo(changePriceProduct.getPrice());
    }
  }

  @Nested
  @DisplayName("상품 가격을 변경할 때")
  class WhenChangePrice {

    @DisplayName("상품 가격 변경할 상품 가격은 0원 보다 작을 수 없다")
    @MethodSource("kitchenpos.application.ProductServiceTest#provideBigDecimalsForNullAndNegative")
    @ParameterizedTest(name = "{displayName}: [{index}] {argumentsWithNames}")
    void givenNotValidPrice_whenChangePrice_thenIllegalArgumentException(BigDecimal price) {
      // given
      Product product = creationRequestProduct("후라이드치킨", BigDecimal.valueOf(11000));

      Product changePriceProduct = new Product();
      changePriceProduct.setPrice(price);

      // when & then
      assertThatIllegalArgumentException().isThrownBy(
          () -> productService.changePrice(product.getId(), changePriceProduct));
    }

    @DisplayName("상품가격을 변경할 상품이 존재하지 않으면 상품 가격을 변경할 수 없다")
    @Test
    void givenNoSuchProduct_whenChangePrice_thenNoSuchElementException() {
      // given
      Product changePriceProduct = new Product();
      changePriceProduct.setPrice(BigDecimal.valueOf(11000));

      // when & then
      assertThatThrownBy(() -> productService.changePrice(UUID.randomUUID(), changePriceProduct))
          .isInstanceOf(NoSuchElementException.class);

    }
  }

  @Nested
  @DisplayName("상품을 조회할 때")
  class WhenFind {
    @DisplayName("상품조회 - 등록된 상품 목록을 조회할 수 있다.")
    @Test
    void givenProduct_whenFindAll_thenReturnProducts() {
      // given
      given(productRepository.findAll()).willReturn(
          List.of(
              creationRequestProduct("후라이드치킨", BigDecimal.valueOf(11000)),
              creationRequestProduct("양념치킨", BigDecimal.valueOf(12000)))
      );

      // when
      List<Product> products = productService.findAll();

      // then
      assertThat(products).hasSize(2);
      assertThat(products).extracting(Product::getName).contains("후라이드치킨", "양념치킨");
      assertThat(products).extracting(Product::getPrice)
          .contains(BigDecimal.valueOf(11000), BigDecimal.valueOf(12000));
    }
  }

  private static Stream<Arguments> provideBigDecimalsForNullAndNegative() {
    return Stream.of(
        null,
        Arguments.of(BigDecimal.valueOf(-1))
    );
  }

  private static Product creationRequestProduct(String name, BigDecimal price) {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setName(name);
    product.setPrice(price);
    return product;
  }
}
