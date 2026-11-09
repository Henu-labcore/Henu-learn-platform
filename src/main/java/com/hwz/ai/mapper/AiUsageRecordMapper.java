package com.hwz.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.ai.entity.AiUsageRecord;
import org.apache.ibatis.annotations.Mapper;

/** AI 请求用量表的数据访问入口。 */
@Mapper
public interface AiUsageRecordMapper extends BaseMapper<AiUsageRecord> {
}
