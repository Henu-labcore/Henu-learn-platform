package com.hwz.favorite.mapper;

import com.hwz.favorite.dto.FavoriteDtos.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.*;

/** 新表数据访问；SQL 参数绑定，原学习内容仅 LEFT JOIN 查询。 */
@Repository
public class FavoriteMapper {
    private final JdbcTemplate db;
    public FavoriteMapper(JdbcTemplate db) { this.db=db; }
    public JdbcTemplate db() { return db; }
    public List<FavoriteView> list(Long user,String keyword,Long category,String tag,boolean oldest) {
        String sql="SELECT f.*,COALESCE(li.title,f.title_snapshot) AS title,COALESCE(li.summary,'') AS summary,li.difficulty,li.duration,COALESCE(li.status='PUBLISHED',0) AS available,c.name AS category_name FROM favorite f LEFT JOIN learning_item li ON li.item_pk=f.item_pk LEFT JOIN favorite_category c ON c.id=f.category_id AND c.user_id=f.user_id WHERE f.user_id=?";
        List<Object> args=new ArrayList<>();args.add(user);
        // LOCATE 使用字面关键词，避免用户输入 %/_ 改变搜索语义。
        // 原表与新表可能使用不同排序规则，在查询表达式中适配，不修改原字段。
        if(keyword!=null&&!keyword.isEmpty()){sql+=" AND (LOCATE(?,COALESCE(li.title,f.title_snapshot) COLLATE utf8mb4_unicode_ci)>0 OR LOCATE(?,COALESCE(li.summary,'') COLLATE utf8mb4_unicode_ci)>0 OR LOCATE(?,f.note COLLATE utf8mb4_unicode_ci)>0)";args.add(keyword);args.add(keyword);args.add(keyword);}
        if(category!=null){sql+=category==0?" AND f.category_id IS NULL":" AND f.category_id=?";if(category!=0)args.add(category);}
        if(tag!=null&&!tag.isEmpty()){sql+=" AND EXISTS (SELECT 1 FROM favorite_tag_rel r JOIN favorite_tag t ON t.id=r.tag_id WHERE r.favorite_id=f.id AND t.user_id=f.user_id AND t.name=?)";args.add(tag);}
        sql+=" ORDER BY f.created_at "+(oldest?"ASC":"DESC")+",f.id "+(oldest?"ASC":"DESC");
        List<FavoriteView> rows=db.query(sql,(rs,n)->{FavoriteView v=new FavoriteView();v.setId(rs.getLong("id"));v.setItemPk(rs.getLong("item_pk"));v.setCategoryId(rs.getObject("category_id")==null?null:rs.getLong("category_id"));v.setTitle(rs.getString("title"));v.setSummary(rs.getString("summary"));v.setCategoryName(rs.getString("category_name"));v.setDifficulty(rs.getString("difficulty"));v.setDuration(rs.getString("duration"));v.setNote(rs.getString("note"));v.setAvailable(rs.getBoolean("available"));v.setVersion(rs.getInt("version"));v.setFavoritedAt(rs.getTimestamp("created_at").toLocalDateTime());v.setTags(new ArrayList<>());return v;},args.toArray());
        Map<Long,FavoriteView> index=new HashMap<>();rows.forEach(v->index.put(v.getId(),v));
        db.query("SELECT r.favorite_id,t.name FROM favorite_tag_rel r JOIN favorite f ON f.id=r.favorite_id JOIN favorite_tag t ON t.id=r.tag_id AND t.user_id=f.user_id WHERE f.user_id=? ORDER BY t.name",rs->{FavoriteView v=index.get(rs.getLong("favorite_id"));if(v!=null)v.getTags().add(rs.getString("name"));},user);
        return rows;
    }
    public List<Label> labels(Long user,boolean categories) {
        // 表名只由内部布尔值选择，绝不接受客户端表名。
        return db.query("SELECT id,name FROM "+(categories?"favorite_category":"favorite_tag")+" WHERE user_id=? ORDER BY name",(rs,n)->{Label l=new Label();l.setId(rs.getLong(1));l.setName(rs.getString(2));return l;},user);
    }
}
