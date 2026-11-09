package com.hwz.favorite.service;

import com.hwz.favorite.dto.FavoriteDtos.*;
import com.hwz.favorite.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;

/** 收藏后端真源；每个查询/写入都限制 user_id，浏览器不再作为持久化来源。 */
@Service
public class FavoriteService {
    private final FavoriteMapper mapper;
    public FavoriteService(FavoriteMapper mapper){this.mapper=mapper;}
    public List<FavoriteView> list(Long user,String keyword,Long category,String tag,String sort){
        if(keyword!=null&&keyword.length()>100)bad("搜索词不能超过100字");
        if(tag!=null&&tag.length()>30)bad("标签不能超过30字");
        return mapper.list(user,keyword==null?null:keyword.trim(),category,tag,"oldest".equals(sort));
    }
    public Map<String,Object> meta(Long user){Map<String,Object> m=new HashMap<>();m.put("categories",mapper.labels(user,true));m.put("tags",mapper.labels(user,false));return m;}
    @Transactional public FavoriteView add(Long user,Long item){
        if(item==null||item<=0)bad("学习内容编号不正确");
        List<String> titles=mapper.db().queryForList("SELECT title FROM learning_item WHERE item_pk=? AND status='PUBLISHED'",String.class,item);
        if(titles.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"学习内容不存在或未发布");
        mapper.db().update("INSERT IGNORE INTO favorite(user_id,item_pk,title_snapshot,note,version) VALUES(?,?,?,'',1)",user,item,titles.get(0));
        return find(user,item);
    }
    @Transactional public void remove(Long user,Long item){
        mapper.db().update("DELETE r FROM favorite_tag_rel r JOIN favorite f ON f.id=r.favorite_id WHERE f.user_id=? AND f.item_pk=?",user,item);
        mapper.db().update("DELETE FROM favorite WHERE user_id=? AND item_pk=?",user,item);
    }
    @Transactional public FavoriteView update(Long user,Long item,SaveRequest req){
        if(req==null)bad("收藏信息不能为空");
        List<Map<String,Object>> rows=mapper.db().queryForList("SELECT id,version FROM favorite WHERE user_id=? AND item_pk=? FOR UPDATE",user,item);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"收藏不存在");
        Map<String,Object> row=rows.get(0);int version=((Number)row.get("version")).intValue();
        if(req.getVersion()==null||req.getVersion()!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"收藏已在其他设备更新，请刷新后重试");
        Long category=req.getCategoryId();
        if(category!=null&&mapper.db().queryForObject("SELECT COUNT(*) FROM favorite_category WHERE id=? AND user_id=?",Integer.class,category,user)!=1)bad("分类不属于当前账号");
        String note=req.getNote()==null?"":req.getNote().trim();if(note.length()>1000)bad("备注不能超过1000字");
        List<String> tags=normalizeTags(req.getTags());Long id=((Number)row.get("id")).longValue();
        mapper.db().update("UPDATE favorite SET category_id=?,note=?,version=version+1 WHERE id=? AND user_id=?",category,note,id,user);
        mapper.db().update("DELETE FROM favorite_tag_rel WHERE favorite_id=?",id);
        for(String tag:tags){mapper.db().update("INSERT IGNORE INTO favorite_tag(user_id,name) VALUES(?,?)",user,tag);Long tagId=mapper.db().queryForObject("SELECT id FROM favorite_tag WHERE user_id=? AND name=?",Long.class,user,tag);mapper.db().update("INSERT INTO favorite_tag_rel(favorite_id,tag_id) VALUES(?,?)",id,tagId);}
        return find(user,item);
    }
    @Transactional public Map<String,Integer> importLegacy(Long user,ImportRequest req){
        if(req==null||!Boolean.TRUE.equals(req.getConfirmed())||req.getItemIds()==null||req.getItemIds().size()>1000)bad("请确认收藏归属，单次最多导入1000项");
        int imported=0,skipped=0;
        for(Long id:new LinkedHashSet<>(req.getItemIds())){if(id==null||id<=0){skipped++;continue;}try{add(user,id);imported++;}catch(ResponseStatusException ex){if(ex.getStatus()!=HttpStatus.NOT_FOUND)throw ex;skipped++;}}
        Map<String,Integer> result=new HashMap<>();result.put("imported",imported);result.put("skipped",skipped);return result;
    }
    @Transactional public List<Label> saveCategory(Long user,Long id,String raw){
        String name=labelName(raw);
        try{if(id==null)mapper.db().update("INSERT INTO favorite_category(user_id,name) VALUES(?,?)",user,name);else if(mapper.db().update("UPDATE favorite_category SET name=? WHERE id=? AND user_id=?",name,id,user)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"分类不存在");}
        catch(DuplicateKeyException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,"已存在同名分类");}
        return mapper.labels(user,true);
    }
    @Transactional public void deleteCategory(Long user,Long id){
        mapper.db().update("UPDATE favorite SET category_id=NULL,version=version+1 WHERE user_id=? AND category_id=?",user,id);
        mapper.db().update("DELETE FROM favorite_category WHERE user_id=? AND id=?",user,id);
    }
    private FavoriteView find(Long user,Long item){return mapper.list(user,null,null,null,false).stream().filter(v->v.getItemPk().equals(item)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"收藏不存在"));}
    static List<String> normalizeTags(List<String> input){
        if(input==null)return Collections.emptyList();if(input.size()>10)bad("每项收藏最多10个标签");
        Map<String,String> result=new LinkedHashMap<>();for(String t:input){String n=labelName(t);result.putIfAbsent(n.toLowerCase(Locale.ROOT),n);}return new ArrayList<>(result.values());
    }
    static String labelName(String name){if(name==null||name.trim().isEmpty()||name.trim().length()>30)bad("名称长度应为1至30字");return name.trim();}
    private static void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
