package com.hwz.favorite.service;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FavoriteServiceTest {
    @Test void validatesAndNormalizesPersonalLabels(){
        assertEquals(Arrays.asList("Java","复习"),FavoriteService.normalizeTags(Arrays.asList(" Java ","java","复习")));
        assertThrows(RuntimeException.class,()->FavoriteService.labelName("   "));
        assertThrows(RuntimeException.class,()->FavoriteService.normalizeTags(Collections.nCopies(11,"标签")));
    }
}
