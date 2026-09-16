package com.bienCriollas.stock.order.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomerNameNormalizerTest {

    @Test
    void normalizesCustomerNames() {
        assertThat(CustomerNameNormalizer.normalize("JULIETA VARGAS"))
                .isEqualTo("Julieta Vargas");
        assertThat(CustomerNameNormalizer.normalize("julieta vargas"))
                .isEqualTo("Julieta Vargas");
        assertThat(CustomerNameNormalizer.normalize("   julieta    vargas   "))
                .isEqualTo("Julieta Vargas");
        assertThat(CustomerNameNormalizer.normalize("Micaela Montiveros"))
                .isEqualTo("Micaela Montiveros");
        assertThat(CustomerNameNormalizer.normalize("mArCeLo JAIME"))
                .isEqualTo("Marcelo Jaime");
    }

    @Test
    void preservesNullAndBlankValues() {
        assertThat(CustomerNameNormalizer.normalize(null)).isNull();
        assertThat(CustomerNameNormalizer.normalize("   ")).isEqualTo("   ");
    }
}
