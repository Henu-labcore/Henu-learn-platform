package com.hwz.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.common.entity.StudentMasteryHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 掌握度历史快照 Mapper。
 *
 * <p>聚合查询通过 {@code MasteryHistoryService} 中的 JdbcTemplate 完成，
 * 这里保留 BaseMapper 以便单表 CRUD 与后续扩展。</p>
 */
@Mapper
public interface StudentMasteryHistoryMapper extends BaseMapper<StudentMasteryHistory> {
}
