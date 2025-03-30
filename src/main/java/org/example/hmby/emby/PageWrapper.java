package org.example.hmby.emby;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * description:  <br>
 * date: 2022/4/6 14:24 <br>
 * author: ws <br>
 */
@Data
public class PageWrapper<T> implements Page<T> {

    @JsonProperty("Items")
    private List<T> items;

    @JsonProperty("TotalRecordCount")
    private Long totalRecordCount;
    
    private int size;
    
    private int number;

    @Override
    public int getTotalPages() {
        return Math.toIntExact(totalRecordCount);
    }

    @Override
    public long getTotalElements() {
        return totalRecordCount;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getNumberOfElements() {
        return 0;
    }

    @Override
    public List<T> getContent() {
        return List.of();
    }

    @Override
    public boolean hasContent() {
        return false;
    }

    @NotNull
    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public boolean isFirst() {
        return false;
    }

    @Override
    public boolean isLast() {
        return false;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @NotNull
    @Override
    public Pageable nextPageable() {
        return Pageable.unpaged();
    }

    @NotNull
    @Override
    public Pageable previousPageable() {
        return Pageable.unpaged();
    }

    @Override
    public <U> Page<U> map(Function<? super T, ? extends U> converter) {
        return null;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
}
