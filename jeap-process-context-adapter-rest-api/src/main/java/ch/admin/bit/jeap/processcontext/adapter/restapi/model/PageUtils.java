package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@UtilityClass
class PageUtils {

    public static <T> Page<T> toPage(List<T> fullList, Pageable pageable) {
        if (fullList == null || fullList.isEmpty()) {
            return Page.empty(pageable);
        }

        int start = (int) pageable.getOffset();
        if (start >= fullList.size()) {
            return new PageImpl<>(List.of(), pageable, fullList.size());
        }
        int end = Math.min(start + pageable.getPageSize(), fullList.size());
        List<T> content = fullList.subList(start, end);
        return new PageImpl<>(content, pageable, fullList.size());
    }
}
