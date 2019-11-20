package io.choerodon.devops.infra.util;

import com.google.common.collect.Lists;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.stream.Collectors;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  14:58 2019/4/4
 * Description:
 */
public class PageRequestUtil {
    private PageRequestUtil() {
    }

    public static String checkSortIsEmpty(Pageable pageable) {
        String index = "";
        if (pageable.getSort() == null) {
            index = "true";
        }
        return index;
    }


    public static String getOrderBy(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort != null) {
            return Lists.newArrayList(pageable.getSort().iterator()).stream()
                    .map(t -> HumpToUnderlineUtil.toUnderLine(t.getProperty()) + " " + t.getDirection())
                    .collect(Collectors.joining(","));
        }
        return "";
    }
}
