package org.marmotgraph.graphdb.structure.controller;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructureRepositoryTest {

    private final static List<String> TEN_ELEMENTS = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");


    @Test
    void partitionEmptyList() {
        List<Object> emptyList = new ArrayList<>();

        List<List<Object>> partition = StructureRepository.partition(emptyList, 20);

        assertEquals(0, partition.size());
    }

    @Test
    void partitionExactMatchList() {
        List<List<String>> partition = StructureRepository.partition(TEN_ELEMENTS, 5);
        assertEquals(2, partition.size());
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), partition.get(0));
        assertEquals(Arrays.asList("f", "g", "h", "i", "j"), partition.get(1));
    }

    @Test
    void partitionRemainderMatchList() {
        List<List<String>> partition = StructureRepository.partition(TEN_ELEMENTS, 3);
        assertEquals(4, partition.size());
        assertEquals(Arrays.asList("a", "b", "c"), partition.get(0));
        assertEquals(Arrays.asList("d", "e", "f"), partition.get(1));
        assertEquals(Arrays.asList("g", "h", "i"), partition.get(2));
        assertEquals(List.of("j"), partition.get(3));
    }


    @Test
    void partitionLessThanSizeList() {
        List<List<String>> partition = StructureRepository.partition(TEN_ELEMENTS, 15);
        assertEquals(1, partition.size());
        assertEquals(TEN_ELEMENTS, partition.get(0));
    }
}